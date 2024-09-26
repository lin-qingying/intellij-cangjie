//package com.huawei.cangjie.contracts
//
//import com.huawei.cangjie.builtins.CangJieBuiltIns
//import com.huawei.cangjie.config.LanguageFeature
//import com.huawei.cangjie.config.LanguageVersionSettings
//import com.huawei.cangjie.contracts.model.Computation
//import com.huawei.cangjie.contracts.model.ESEffect
//import com.huawei.cangjie.contracts.model.MutableContextInfo
//import com.huawei.cangjie.contracts.model.functors.EqualsFunctor
//import com.huawei.cangjie.contracts.model.structure.ESCalls
//import com.huawei.cangjie.contracts.model.structure.ESConstants
//import com.huawei.cangjie.contracts.model.structure.ESReturns
//import com.huawei.cangjie.contracts.model.structure.UNKNOWN_COMPUTATION
//import com.huawei.cangjie.contracts.model.visitors.InfoCollector
//import com.huawei.cangjie.descriptors.BindingTrace
//import com.huawei.cangjie.descriptors.ModuleDescriptor
//import com.huawei.cangjie.psi.CjCallExpression
//import com.huawei.cangjie.psi.CjDeclaration
//import com.huawei.cangjie.psi.CjExpression
//import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
//import com.huawei.cangjie.resolve.BindingContext
//import com.huawei.cangjie.resolve.BindingTrace
//import com.huawei.cangjie.resolve.calls.model.ResolvedCall
//import com.huawei.cangjie.resolve.calls.smartcasts.ConditionalDataFlowInfo
//import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
//import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
//
//class EffectSystem(
//    val languageVersionSettings: LanguageVersionSettings,
//    val dataFlowValueFactory: DataFlowValueFactory,
//    val builtIns: CangJieBuiltIns
//) {
//    fun getDataFlowInfoForFinishedCall(
//        resolvedCall: ResolvedCall<*>,
//        bindingTrace: BindingTrace,
//        moduleDescriptor: ModuleDescriptor
//    ): DataFlowInfo {
////        if (!languageVersionSettings.supportsFeature(LanguageFeature.UseReturnsEffect)) return DataFlowInfo.EMPTY
//
//        // Prevent launch of effect system machinery on pointless cases (constants/enums/constructors/etc.)
//        val callExpression = resolvedCall.call.callElement as? CjCallExpression ?: return DataFlowInfo.EMPTY
//
//        val resultContextInfo = getContextInfoWhen(ESReturns(ESConstants.wildcard), callExpression, bindingTrace, moduleDescriptor)
//
//        return resultContextInfo.toDataFlowInfo(languageVersionSettings, builtIns)
//    }
//
//    fun getDataFlowInfoWhenEquals(
//        leftExpression: CjExpression?,
//        rightExpression: CjExpression?,
//        bindingTrace: BindingTrace,
//        moduleDescriptor: ModuleDescriptor
//    ): ConditionalDataFlowInfo {
//        if (!languageVersionSettings.supportsFeature(LanguageFeature.UseReturnsEffect)) return ConditionalDataFlowInfo.EMPTY
//        if (leftExpression == null || rightExpression == null) return ConditionalDataFlowInfo.EMPTY
//
//        val leftComputation =
//            getNonTrivialComputation(leftExpression, bindingTrace, moduleDescriptor) ?: return ConditionalDataFlowInfo.EMPTY
//        val rightComputation =
//            getNonTrivialComputation(rightExpression, bindingTrace, moduleDescriptor) ?: return ConditionalDataFlowInfo.EMPTY
//
//        val effects = EqualsFunctor(false).invokeWithArguments(leftComputation, rightComputation)
//
//        val equalsContextInfo = InfoCollector(ESReturns(ESConstants.trueValue), builtIns).collectFromSchema(effects)
//        val notEqualsContextInfo = InfoCollector(ESReturns(ESConstants.falseValue), builtIns).collectFromSchema(effects)
//
//        return ConditionalDataFlowInfo(
//            equalsContextInfo.toDataFlowInfo(languageVersionSettings, builtIns),
//            notEqualsContextInfo.toDataFlowInfo(languageVersionSettings, builtIns)
//        )
//    }
//
//    fun recordDefiniteInvocations(resolvedCall: ResolvedCall<*>, bindingTrace: BindingTrace, moduleDescriptor: ModuleDescriptor) {
//        if (!languageVersionSettings.supportsFeature(LanguageFeature.UseCallsInPlaceEffect)) return
//
//        // Prevent launch of effect system machinery on pointless cases (constants/enums/constructors/etc.)
//        val callExpression = resolvedCall.call.callElement as? CjCallExpression ?: return
//        if (callExpression is CjDeclaration) return
//
//        val resultingContextInfo = getContextInfoWhen(ESReturns(ESConstants.wildcard), callExpression, bindingTrace, moduleDescriptor)
//        for (effect in resultingContextInfo.firedEffects) {
//            val callsEffect = effect as? ESCalls ?: continue
//            val lambdaArgument = (callsEffect.callable as? ESLambda)?.lambda ?: continue
//            bindingTrace.record(BindingContext.LAMBDA_INVOCATIONS, lambdaArgument, callsEffect.kind)
//        }
//    }
//
//    fun extractDataFlowInfoFromCondition(
//        condition: CjExpression?,
//        value: Boolean,
//        bindingTrace: BindingTrace,
//        moduleDescriptor: ModuleDescriptor
//    ): DataFlowInfo {
//        if (!languageVersionSettings.supportsFeature(LanguageFeature.UseReturnsEffect)) return DataFlowInfo.EMPTY
//        if (condition == null) return DataFlowInfo.EMPTY
//
//        return getContextInfoWhen(ESReturns(ESConstants.booleanValue(value)), condition, bindingTrace, moduleDescriptor)
//            .toDataFlowInfo(languageVersionSettings, moduleDescriptor.builtIns)
//    }
//
//    private fun getContextInfoWhen(
//        observedEffect: ESEffect,
//        expression: CjExpression,
//        bindingTrace: BindingTrace,
//        moduleDescriptor: ModuleDescriptor
//    ): MutableContextInfo {
//        val isInContractBlock = expression.parentsWithSelf.filterIsInstance<CjExpression>().any {
//            bindingTrace.bindingContext[BindingContext.IS_CONTRACT_DECLARATION_BLOCK, it] == true
//        }
//        if (isInContractBlock) return MutableContextInfo.EMPTY
//        val computation = getNonTrivialComputation(expression, bindingTrace, moduleDescriptor) ?: return MutableContextInfo.EMPTY
//        return InfoCollector(observedEffect, builtIns).collectFromSchema(computation.effects)
//    }
//
//    private fun getNonTrivialComputation(expression: CjExpression, trace: BindingTrace, moduleDescriptor: ModuleDescriptor): Computation? {
//        val visitor = EffectsExtractingVisitor(trace, moduleDescriptor, dataFlowValueFactory, languageVersionSettings)
//        return visitor.extractOrGetCached(expression).takeUnless { it == UNKNOWN_COMPUTATION }
//    }
//}
