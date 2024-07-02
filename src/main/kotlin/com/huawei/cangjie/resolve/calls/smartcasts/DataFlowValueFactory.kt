package com.huawei.cangjie.resolve.calls.smartcasts

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjVariable
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.types.CangJieType

/**
 * This class is intended to create data flow values for different kind of expressions.
 * Then data flow values serve as keys to obtain data flow information for these expressions.
 */
interface DataFlowValueFactory {
    fun createDataFlowValue(
        expression: CjExpression,
        type: CangJieType,
        resolutionContext: ResolutionContext<*>
    ): DataFlowValue

    fun createDataFlowValue(
        expression: CjExpression,
        type: CangJieType,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue

    fun createDataFlowValueForStableReceiver(receiver: ReceiverValue): DataFlowValue

    fun createDataFlowValue(
        receiverValue: ReceiverValue,
        resolutionContext: ResolutionContext<*>
    ): DataFlowValue

    fun createDataFlowValue(
        receiverValue: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue

    fun createDataFlowValueForProperty(
        property: CjVariable,
        variableDescriptor: VariableDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue

}