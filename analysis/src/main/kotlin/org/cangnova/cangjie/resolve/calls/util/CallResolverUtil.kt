/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.calls.util

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.NO_CONSTRUCTOR
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.CallTransformer
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tasks.OldResolutionCandidate
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.SyntheticScopes
import org.cangnova.cangjie.resolve.scopes.collectSyntheticConstructors
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.AbbreviatedType
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.DefaultTypeSubstitutor

internal fun PsiElement.reportOnElement() =
    (this as? CjConstructorDelegationCall)
        ?.takeIf { isImplicit }
        ?.let { getStrictParentOfType<CjConstructor<*>>()!! }
        ?: this

fun getEffectiveExpectedType(
    parameterDescriptor: ValueParameterDescriptor,
    argument: ValueArgument,
    context: ResolutionContext<*>
): CangJieType {
    return getEffectiveExpectedTypeForSingleArgument(
        parameterDescriptor,
        argument,
        context.languageVersionSettings,
        context.trace
    )
}

fun isConventionCall(call: Call): Boolean {
    if (call is CallTransformer.CallForImplicitInvoke) return true
    val callElement = call.callElement
    if (callElement is CjArrayAccessExpression) return true
    val calleeExpression = call.calleeExpression as? CjOperationReferenceExpression ?: return false
    return calleeExpression.isConventionOperator()
}

fun getEffectiveExpectedTypeForSingleArgument(
    parameterDescriptor: ValueParameterDescriptor,
    argument: ValueArgument,
    languageVersionSettings: LanguageVersionSettings,
    trace: BindingTrace
): CangJieType {
    if (argument.getSpreadElement() != null) {
        // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
        return /*if (parameterDescriptor.varargElementType == null) DONT_CARE else */parameterDescriptor.type
    }

//    if (
//        arrayAssignmentToVarargInNamedFormInAnnotation(parameterDescriptor, argument, languageVersionSettings, trace) ||
//        arrayAssignmentToVarargInNamedFormInFunction(parameterDescriptor, argument, languageVersionSettings, trace)
//    ) {
//        return parameterDescriptor.type
//    }

    return getExpectedType(parameterDescriptor)
}

private fun getExpectedType(parameterDescriptor: ValueParameterDescriptor): CangJieType {
    return /*parameterDescriptor.varargElementType ?: */parameterDescriptor.type
}

fun isOrOverridesSynthesized(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
        return true
    }
    if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return descriptor.overriddenDescriptors.all(::isOrOverridesSynthesized)
    }
    return false
}

// TODO: 重构为使用新的约束系统 API
//private fun CallableDescriptor.hasReturnTypeDependentOnUninferredParams(constraintSystem: ConstraintSystem): Boolean {
//    val returnType = returnType ?: return false
//    val nestedTypeVariables = constraintSystem.getNestedTypeVariables(returnType)
//    return nestedTypeVariables.any { constraintSystem.getTypeBounds(it).value == null }
//}
@Suppress("UNUSED_PARAMETER")
private fun CallableDescriptor.hasReturnTypeDependentOnUninferredParams(constraintSystem: ConstraintSystem): Boolean {
    // 暂时返回 false，等待新约束系统 API 完善
    return false
}

fun checkForConstructorCallOnFunctionalType(
    typeReference: CjTypeReference?,
    context: BasicCallResolutionContext
) {
    if (typeReference?.typeElement is CjFunctionType) {
        val factory = NO_CONSTRUCTOR
        context.trace.report(factory.on(context.call.getValueArgumentListOrElement()))
    }
}

// TODO: 重构为使用新的约束系统 API
//fun CallableDescriptor.hasInferredReturnType(constraintSystem: ConstraintSystem): Boolean {
//    if (hasReturnTypeDependentOnUninferredParams(constraintSystem)) return false
//
//    // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
//    if (constraintSystem.status.hasOnlyErrorsDerivedFrom(ConstraintPositionKind.EXPECTED_TYPE_POSITION)) return false
//    return true
//}
@Suppress("UNUSED_PARAMETER")
fun CallableDescriptor.hasInferredReturnType(constraintSystem: ConstraintSystem): Boolean {
    // 暂时返回 true，等待新约束系统 API 完善
    if (hasReturnTypeDependentOnUninferredParams(constraintSystem)) return false
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
            ComposableTypeSubstitutor.create(
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
