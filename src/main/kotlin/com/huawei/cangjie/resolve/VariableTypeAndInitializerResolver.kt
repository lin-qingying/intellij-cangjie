package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.descriptors.Errors.VARIABLE_WITH_NO_TYPE_NO_INITIALIZER
import com.huawei.cangjie.descriptors.impl.VariableDescriptorWithInitializerImpl
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjVariableDeclaration
import com.huawei.cangjie.resolve.DescriptorResolver.transformAnonymousTypeIfNeeded
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.types.expressions.PreliminaryDeclarationVisitor
import com.huawei.cangjie.types.util.TypeUtils

class VariableTypeAndInitializerResolver(

    private val storageManager: StorageManager,
    private val expressionTypingServices: ExpressionTypingServices,
    private val typeResolver: TypeResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
//    private val delegatedPropertyResolver: DelegatedPropertyResolver,
    private val wrappedTypeFactory: WrappedTypeFactory,
    private val typeApproximator: TypeApproximator,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer,
    private val languageVersionSettings: LanguageVersionSettings,
    private val anonymousTypeTransformers: Iterable<DeclarationSignatureAnonymousTypeTransformer>

) {
    companion object {
        @JvmStatic
        fun getTypeForVariableWithoutReturnType(property: String): SimpleType =
            ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_VARIABLE, property)
    }
    fun resolveType(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scopeForInitializer: LexicalScope,
        variable: CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): CangJieType {
        resolveTypeOptional(
            variableDescriptor, scopeForInitializer, variable, dataFlowInfo, inferenceSession, trace, local
        )?.let { return it }

        if (local) {
            trace.report(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.on(variable))
        }

        return getTypeForVariableWithoutReturnType(variableDescriptor.name.asString())
    }
    fun setConstantForVariableIfNeeded(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scope: LexicalScope,
        variable:CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        variableType: CangJieType,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ) {
        if (!variable.hasInitializer() || variable.isVar) return
        variableDescriptor.setCompileTimeInitializerFactory {
            storageManager.createRecursionTolerantNullableLazyValue(
                computeInitializer@{
                    if (!DescriptorUtils.shouldRecordInitializerForProperty(
                            variableDescriptor,
                            variableType
                        )) return@computeInitializer null

                    val initializer = variable.initializer
                    val initializerType =
                        expressionTypingServices.safeGetType(scope, initializer!!, variableType, dataFlowInfo, inferenceSession, trace)
                    val constant = constantExpressionEvaluator.evaluateExpression(initializer, trace, initializerType)
                        ?: return@computeInitializer null

                    if (constant.usesNonConstValAsConstant && variableDescriptor.isConst) {
                        trace.report(Errors.NON_CONST_LET_USED_IN_CONSTANT_EXPRESSION.on(initializer))
                    }

                    constant.toConstantValue(initializerType)
                },
                null
            )
        }
    }

//    private fun resolveDelegatedPropertyType(
//        property: CjVariableDeclaration,
//        variableDescriptor: VariableDescriptorWithAccessors,
//        scopeForInitializer: LexicalScope,
//        dataFlowInfo: DataFlowInfo,
//        inferenceSession: InferenceSession,
//        trace: BindingTrace,
//        local: Boolean
//    ) = wrappedTypeFactory.createRecursionIntolerantDeferredType(trace) {
//        val delegateExpression = property.delegateExpression!!
//        val type = delegatedPropertyResolver.resolveDelegateExpression(
//            delegateExpression, property, variableDescriptor, scopeForInitializer, trace, dataFlowInfo, inferenceSession
//        )
//
//        val getterReturnType = delegatedPropertyResolver.getGetValueMethodReturnType(
//            variableDescriptor, delegateExpression, type, trace, scopeForInitializer, dataFlowInfo, inferenceSession
//        )
//
//        val delegatedType = getterReturnType?.let { approximateType(it, local) }
//            ?: ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_DELEGATION, delegateExpression.text)
//
//        transformAnonymousTypeIfNeeded(
//            variableDescriptor, property, delegatedType, trace, anonymousTypeTransformers, languageVersionSettings
//        )
//    }


    fun resolveTypeOptional(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scopeForInitializer: LexicalScope,
        variable: CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): CangJieType? {
        val propertyTypeRef = variable.typeReference
        return when {
            propertyTypeRef != null -> typeResolver.resolveType(scopeForInitializer, propertyTypeRef, trace, true)

//            !variable.hasInitializer() && (variable is CjProperty || variable is CjVariable) && variableDescriptor is VariableDescriptorWithAccessors &&
//                    variable.hasDelegateExpression() ->
//                resolveDelegatedPropertyType(
//                    variable, variableDescriptor, scopeForInitializer, dataFlowInfo, inferenceSession, trace, local
//                )

            variable.hasInitializer() -> when {
                !local ->
                    wrappedTypeFactory.createRecursionIntolerantDeferredType(
                        trace
                    ) {
                        PreliminaryDeclarationVisitor.createForDeclaration(
                            variable, trace,
                            expressionTypingServices.languageVersionSettings
                        )
                        val initializerType = resolveInitializerType(
                            scopeForInitializer, variable.initializer!!, dataFlowInfo, inferenceSession, trace, local
                        )
                        transformAnonymousTypeIfNeeded(
                            variableDescriptor, variable, initializerType, trace, anonymousTypeTransformers, languageVersionSettings
                        )
                    }

                else -> resolveInitializerType(scopeForInitializer, variable.initializer!!, dataFlowInfo, inferenceSession, trace, local)
            }

            else -> null
        }
    }

    private fun resolveInitializerType(
        scope: LexicalScope,
        initializer: CjExpression,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): CangJieType {
        val inferredType = expressionTypingServices.safeGetType(
            scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession, trace
        )
        val approximatedType = approximateType(inferredType, local)
        return declarationReturnTypeSanitizer.sanitizeReturnType(approximatedType, wrappedTypeFactory, trace, languageVersionSettings)
    }

    private fun approximateType(type: CangJieType, local: Boolean): UnwrappedType =
        typeApproximator.approximateDeclarationType(type, local)
//    private fun resolveInitializerType(
//        scope: LexicalScope,
//        initializer: CjExpression,
//        dataFlowInfo: DataFlowInfo,
//        inferenceSession: InferenceSession,
//        trace: BindingTrace,
//        local: Boolean
//    ): CangJieType {
//        val inferredType = expressionTypingServices.safeGetType(
//            scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession, trace
//        )
//        val approximatedType = approximateType(inferredType, local)
//        return declarationReturnTypeSanitizer.sanitizeReturnType(approximatedType, wrappedTypeFactory, trace, languageVersionSettings)
//    }
}
