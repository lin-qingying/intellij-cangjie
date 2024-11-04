package com.linqingying.cangjie.resolve.calls.smartcasts

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjProperty
import com.linqingying.cangjie.psi.CjVariable
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.calls.context.ResolutionContext
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.types.CangJieType

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
        property: CjProperty,
        variableDescriptor: PropertyDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue
    fun createDataFlowValueForVariable(
        variable: CjVariable,
        variableDescriptor: VariableDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue

}
