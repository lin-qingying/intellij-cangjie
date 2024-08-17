package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import com.huawei.cangjie.resolve.calls.inference.getNestedTypeVariables
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.tasks.OldResolutionCandidate
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.SyntheticScopes
import com.huawei.cangjie.resolve.scopes.collectSyntheticConstructors
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.types.AbbreviatedType
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
import com.intellij.psi.PsiElement

internal fun PsiElement.reportOnElement() =
    (this as? CjConstructorDelegationCall)
        ?.takeIf { isImplicit }
        ?.let { getStrictParentOfType<CjSecondaryConstructor>()!! }
        ?: this

fun isOrOverridesSynthesized(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
        return true
    }
    if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return descriptor.overriddenDescriptors.all(::isOrOverridesSynthesized)
    }
    return false
}
private fun CallableDescriptor.hasReturnTypeDependentOnUninferredParams(constraintSystem: ConstraintSystem): Boolean {
    val returnType = returnType ?: return false
    val nestedTypeVariables = constraintSystem.getNestedTypeVariables(returnType)
    return nestedTypeVariables.any { constraintSystem.getTypeBounds(it).value == null }
}

fun checkForConstructorCallOnFunctionalType(
    typeReference: CjTypeReference?,
    context: BasicCallResolutionContext
) {
    if (typeReference?.typeElement is CjFunctionType) {
        val factory =
            when (context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitConstructorCallOnFunctionalSupertype)) {
                true -> Errors.NO_CONSTRUCTOR
                false -> Errors.NO_CONSTRUCTOR_WARNING
            }
        context.trace.report(factory.on(context.call.getValueArgumentListOrElement()))
    }
}

fun CallableDescriptor.hasInferredReturnType(constraintSystem: ConstraintSystem): Boolean {
    if (hasReturnTypeDependentOnUninferredParams(constraintSystem)) return false

    // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
    if (constraintSystem.status.hasOnlyErrorsDerivedFrom(ConstraintPositionKind.EXPECTED_TYPE_POSITION)) return false
    return true
}

fun isSuperOrDelegatingConstructorCall(call: Call): Boolean =
    call.calleeExpression.let { it is CjConstructorCalleeExpression || it is CjConstructorDelegationReferenceExpression }

fun isInvokeCallOnVariable(call: Call): Boolean {
    if (call.callType !== Call.CallType.INVOKE) return false
    val dispatchReceiver = call.dispatchReceiver
    //calleeExpressionAsDispatchReceiver for invoke is always ExpressionReceiver, see CallForImplicitInvoke
    val expression = (dispatchReceiver as ExpressionReceiver).expression
    return expression is CjSimpleNameExpression
}

fun isBinaryRemOperator(call: Call): Boolean {
    val callElement = call.callElement as? CjBinaryExpression ?: return false
    val operator = callElement.operationToken
    return operator is CjToken

    //TODO: check if this is correct
//    val name = OperatorConventions.getNameForOperationSymbol(operator, true, true) ?: return false
//    return name in OperatorConventions.REM_TO_MOD_OPERATION_NAMES.keys
}

fun isInfixCall(call: Call): Boolean {
    val operationRefExpression = call.calleeExpression as? CjOperationReferenceExpression ?: return false
    val binaryExpression = operationRefExpression.parent as? CjBinaryExpression ?: return false
    return binaryExpression.operationReference === operationRefExpression && operationRefExpression.operationSignTokenType == null
}

fun createResolutionCandidatesForConstructors(
    lexicalScope: LexicalScope,
    call: Call,
    typeWithConstructors: CangJieType,
    useKnownTypeSubstitutor: Boolean,
    syntheticScopes: SyntheticScopes
): List<OldResolutionCandidate<ConstructorDescriptor>> {
    val classWithConstructors = typeWithConstructors.constructor.declarationDescriptor as ClassDescriptor

    val unwrappedType = typeWithConstructors.unwrap()
    val knownSubstitutor =
        if (useKnownTypeSubstitutor)
            TypeSubstitutor.create(
                (unwrappedType as? AbbreviatedType)?.abbreviation ?: unwrappedType
            )
        else null

    val typeAliasDescriptor =
        if (unwrappedType is AbbreviatedType)
            unwrappedType.abbreviation.constructor.declarationDescriptor as? TypeAliasDescriptor
        else
            null

    val constructors =
        typeAliasDescriptor?.constructors?.mapNotNull(TypeAliasConstructorDescriptor::withDispatchReceiver)
            ?: classWithConstructors.constructors

    if (constructors.isEmpty()) return emptyList()

    val receiverKind: ExplicitReceiverKind
    val dispatchReceiver: ReceiverValue?

//    if (classWithConstructors.isInner) {
//        val outerClassType = (classWithConstructors.containingDeclaration as? ClassDescriptor)?.defaultType ?: return emptyList()
//        val substitutedOuterClassType = knownSubstitutor?.substitute(outerClassType, Variance.INVARIANT) ?: outerClassType
//
//        val receiver = lexicalScope.getImplicitReceiversHierarchy().firstOrNull {
//            CangJieTypeChecker.DEFAULT.isSubtypeOf(it.type, substitutedOuterClassType)
//        } ?: return emptyList()
//
//        receiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER
//        dispatchReceiver = receiver.value
//    } else {
    receiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
    dispatchReceiver = null
//    }

    val syntheticConstructors = constructors.flatMap { syntheticScopes.collectSyntheticConstructors(it) }

    return (constructors + syntheticConstructors).map {
        OldResolutionCandidate.create(call, it, dispatchReceiver, receiverKind, knownSubstitutor)
    }
}
