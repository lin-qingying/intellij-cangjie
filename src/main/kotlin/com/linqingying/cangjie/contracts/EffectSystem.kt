package com.linqingying.cangjie.contracts

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.contracts.model.Computation
import com.linqingying.cangjie.contracts.model.ESEffect


import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.psi.CjCallExpression
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.psiUtil.parentsWithSelf
import com.linqingying.cangjie.resolve.BindingContext

import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.calls.smartcasts.ConditionalDataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory


//数据流和条件信息
class EffectSystem(
    val languageVersionSettings: LanguageVersionSettings,
    val dataFlowValueFactory: DataFlowValueFactory,
    val builtIns: CangJieBuiltIns
) {
    fun getDataFlowInfoForFinishedCall(
        resolvedCall: ResolvedCall<*>,
        bindingTrace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ): DataFlowInfo {
        return DataFlowInfo.EMPTY

    }
    fun getDataFlowInfoWhenEquals(
        leftExpression: CjExpression?,
        rightExpression: CjExpression?,
        bindingTrace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ): ConditionalDataFlowInfo {
        return ConditionalDataFlowInfo.EMPTY

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
    }
    fun getDataFlowInfoMatchEquals(
        leftExpression: CjExpression?,
        rightExpression: CjExpression?,
        bindingTrace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ): ConditionalDataFlowInfo {
        return ConditionalDataFlowInfo.EMPTY
    }

    fun recordDefiniteInvocations(resolvedCall: ResolvedCall<*>, bindingTrace: BindingTrace, moduleDescriptor: ModuleDescriptor) {

    }

    fun extractDataFlowInfoFromCondition(
        condition: CjExpression?,
        value: Boolean,
        bindingTrace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ): DataFlowInfo {
        return DataFlowInfo.EMPTY
    }


}
