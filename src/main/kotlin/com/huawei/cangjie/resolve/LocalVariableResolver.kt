package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.diagnostics.Errors.LOCAL_EXTENSION_VARIABLE
import com.huawei.cangjie.descriptors.VariableDescriptor

import com.huawei.cangjie.descriptors.impl.LocalVariableDescriptor
import com.huawei.cangjie.descriptors.impl.VariableDescriptorImpl
import com.huawei.cangjie.descriptors.impl.VariableDescriptorWithInitializerImpl
import com.huawei.cangjie.psi.CjPsiUtil
import com.huawei.cangjie.psi.CjVariable
import com.huawei.cangjie.psi.CjVariableDeclaration
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.DataFlowAnalyzer
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.types.expressions.ExpressionTypingFacade
import com.huawei.cangjie.types.expressions.ExpressionTypingUtils
import com.huawei.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

class LocalVariableResolver(
    private val modifiersChecker: ModifiersChecker,
    private val identifierChecker: IdentifierChecker,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val annotationResolver: AnnotationResolver,
    private val variableTypeAndInitializerResolver: VariableTypeAndInitializerResolver,
//    private val delegatedVariableResolver: DelegatedVariableResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {

    fun process(
        variable: CjVariable,
        typingContext: ExpressionTypingContext,
        scope: LexicalScope,
        facade: ExpressionTypingFacade
    ): Pair<CangJieTypeInfo, VariableDescriptor> {
        val context = typingContext.replaceContextDependency(ContextDependency.INDEPENDENT).replaceScope(scope)
        val receiverTypeRef = variable.receiverTypeReference
        if (receiverTypeRef != null) {
            context.trace.report(LOCAL_EXTENSION_VARIABLE.on(receiverTypeRef))
        }

//        val getter = variable.getter
//        if (getter != null) {
//            context.trace.report(LOCAL_VARIABLE_WITH_GETTER.on(getter))
//        }
//
//        val setter = variable.setter
//        if (setter != null) {
//            context.trace.report(LOCAL_VARIABLE_WITH_SETTER.on(setter))
//        }

        val variableDescriptor =
            resolveLocalVariableDescriptor(
                scope,
                variable,
                context.dataFlowInfo,
                context.inferenceSession,
                context.trace
            )

//        val delegateExpression = variable.delegateExpression
//        if (delegateExpression != null) {
//            if (!languageVersionSettings.supportsFeature(LanguageFeature.LocalDelegatedProperties)) {
//                context.trace.report(
//                    UNSUPPORTED_FEATURE.on(
//                        variable.delegate!!,
//                        LanguageFeature.LocalDelegatedProperties to languageVersionSettings
//                    )
//                )
//            }

//            if (variableDescriptor is VariableDescriptorWithAccessors) {
//                delegatedVariableResolver.resolveVariableDelegate(
//                    typingContext.dataFlowInfo,
//                    variable,
//                    variableDescriptor,
//                    delegateExpression,
//                    typingContext.scope,
//                    typingContext.inferenceSession,
//                    typingContext.trace
//                )
//                variableDescriptor.getter?.updateAccessorFlagsFromResolvedCallForDelegatedVariable(typingContext.trace)
//                variableDescriptor.setter?.updateAccessorFlagsFromResolvedCallForDelegatedVariable(typingContext.trace)
//            }
//        }

        val initializer = variable.initializer
        var typeInfo: CangJieTypeInfo
        if (initializer != null) {
            val outType = variableDescriptor.type
            typeInfo = facade.getTypeInfo(initializer, context.replaceExpectedType(outType))
            val dataFlowInfo = typeInfo.dataFlowInfo
            val type = typeInfo.type
            if (type != null) {
                val initializerDataFlowValue = dataFlowValueFactory.createDataFlowValue(initializer, type, context)
                if (!variableDescriptor.isVar && initializerDataFlowValue.canBeBound) {
                    context.trace.record(
                        BindingContext.BOUND_INITIALIZER_VALUE,
                        variableDescriptor,
                        initializerDataFlowValue
                    )
                }
                // At this moment we do not take initializer value into account if type is given for a variable
                // We can comment this condition to take them into account, like here: var s: String? = "xyz"
                // In this case s will be not-nullable until it is changed
                if (variable.typeReference == null) {
                    val variableDataFlowValue = dataFlowValueFactory.createDataFlowValueForVariable(
                        variable, variableDescriptor, context.trace.bindingContext,
                        DescriptorUtils.getContainingModuleOrNull(scope.ownerDescriptor)
                    )
                    // We cannot say here anything new about initializerDataFlowValue
                    // except it has the same value as variableDataFlowValue
                    typeInfo = typeInfo.replaceDataFlowInfo(
                        dataFlowInfo.assign(
                            variableDataFlowValue, initializerDataFlowValue,
//                            languageVersionSettings
                        )
                    )
                }
            }
        } else {
            typeInfo = noTypeInfo(context)
        }

        checkLocalVariableDeclaration(context, variableDescriptor, variable)

        return Pair(typeInfo.replaceType(dataFlowAnalyzer.checkStatementType(variable, context)), variableDescriptor)
    }

    private fun checkLocalVariableDeclaration(
        context: ExpressionTypingContext,
        descriptor: VariableDescriptor,
        cjVariable: CjVariable
    ) {
        ExpressionTypingUtils.checkVariableShadowing(context.scope, context.trace, descriptor)

        modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(cjVariable, descriptor)
        identifierChecker.checkDeclaration(cjVariable, context.trace)
//
//        LateinitModifierApplicabilityChecker.checkLateinitModifierApplicability(
//            context.trace,
//            cjVariable,
//            descriptor,
//            languageVersionSettings
//        )
    }

    private fun resolveLocalVariableDescriptor(
        scope: LexicalScope,
        variable: CjVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ): VariableDescriptor {
        val containingDeclaration = scope.ownerDescriptor
        val result: VariableDescriptorWithInitializerImpl
        val type: CangJieType

        val variableDescriptor = resolveLocalVariableDescriptorWithType(scope, variable, null, trace)
        // For a local variable the type must not be deferred
        type = variableTypeAndInitializerResolver.resolveType(
            variableDescriptor, scope, variable, dataFlowInfo, inferenceSession, trace, local = true
        )
        variableDescriptor.setOutType(type)
        result = variableDescriptor

//        if (inferenceSession is BuilderInferenceSession) {
//            inferenceSession.addExpression(variable)
//        }
        variableTypeAndInitializerResolver
            .setConstantForVariableIfNeeded(result, scope, variable, dataFlowInfo, type, inferenceSession, trace)
        // Type annotations also should be resolved
        ForceResolveUtil.forceResolveAllContents(type.annotations)
        return result
    }

    private fun initializeWithDefaultGetterSetter(variableDescriptor: VariableDescriptorImpl) {
//        var getter = variableDescriptor.getter
//        if (getter == null && !DescriptorVisibilities.isPrivate(variableDescriptor.visibility)) {
//            getter = DescriptorFactory.createDefaultGetter(variableDescriptor, Annotations.EMPTY)
//            getter.initialize(variableDescriptor.type)
//        }
//
//        var setter = variableDescriptor.setter
//        if (setter == null && variableDescriptor.isVar) {
//            setter = DescriptorFactory.createDefaultSetter(variableDescriptor, Annotations.EMPTY, Annotations.EMPTY)
//        }
//        variableDescriptor.initialize(getter, setter)
    }

      fun resolveLocalVariableDescriptorWithType(
        scope: LexicalScope,
        variable: CjVariableDeclaration,
        type: CangJieType?,
        trace: BindingTrace
    ): LocalVariableDescriptor {
//        val hasDelegate = variable is CjVariable && variable.hasDelegate()
//        val hasLateinit = variable.hasModifier(CjTokens.LATEINIT_KEYWORD)
        val variableDescriptor = LocalVariableDescriptor(
            scope.ownerDescriptor,
            annotationResolver.resolveAnnotationsWithArguments(scope, variable.modifierList, trace),
            // Note, that the same code works both for common local vars and for destructuring declarations,
            // but since the first case is illegal error must be reported somewhere else
//            if (variable.isSingleUnderscore)
//                Name.special("<underscore local var>")
//            else
            CjPsiUtil.safeName(variable.name),
            type,
            variable.isVar,
//            hasDelegate,
//            hasLateinit,
            variable.toSourceElement()
        )
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor)
        return variableDescriptor
    }

//    private fun VariableAccessorDescriptor.updateAccessorFlagsFromResolvedCallForDelegatedVariable(trace: BindingTrace) {
//        if (this is FunctionDescriptorImpl) {
//            val resultingDescriptor = trace.bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, this)?.resultingDescriptor
//            if (resultingDescriptor != null) {
//                setSuspend(resultingDescriptor.isSuspend)
//            }
//        }
//    }

}
