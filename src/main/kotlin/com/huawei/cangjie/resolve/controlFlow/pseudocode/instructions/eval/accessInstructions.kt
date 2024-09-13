
package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval

import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValueFactory
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.*
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue


sealed class AccessTarget {
    class Declaration(val descriptor: VariableDescriptor) : AccessTarget() {
        override fun equals(other: Any?) = other is Declaration && descriptor == other.descriptor

        override fun hashCode() = descriptor.hashCode()
    }

    class Call(val resolvedCall: ResolvedCall<*>) : AccessTarget() {
        override fun equals(other: Any?) = other is Call && resolvedCall == other.resolvedCall

        override fun hashCode() = resolvedCall.hashCode()
    }

    object BlackBox : AccessTarget()
}

val AccessTarget.accessedDescriptor: CallableDescriptor?
    get() = when (this) {
        is AccessTarget.Declaration -> descriptor
        is AccessTarget.Call -> resolvedCall.resultingDescriptor
        is AccessTarget.BlackBox -> null
    }

abstract class AccessValueInstruction protected constructor(
    element: CjElement,
    blockScope: BlockScope,
    val target: AccessTarget,
    override val receiverValues: Map<PseudoValue, ReceiverValue>
) : InstructionWithNext(element, blockScope), InstructionWithReceivers

class ReadValueInstruction private constructor(
    element: CjElement,
    blockScope: BlockScope,
    target: AccessTarget,
    receiverValues: Map<PseudoValue, ReceiverValue>,
    private var _outputValue: PseudoValue?
) : AccessValueInstruction(element, blockScope, target, receiverValues), InstructionWithValue {
    constructor(
        element: CjElement,
        blockScope: BlockScope,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        factory: PseudoValueFactory
    ) : this(element, blockScope, target, receiverValues, null) {
        _outputValue = factory.newValue(element, this)
    }

    override val inputValues: List<PseudoValue>
        get() = receiverValues.keys.toList()

    override val outputValue: PseudoValue
        get() = _outputValue!!

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitReadValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitReadValue(this)

    override fun toString(): String {
        val inVal = if (receiverValues.isEmpty()) "" else "|${receiverValues.keys.joinToString()}"
        val targetName = when (target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> target.resolvedCall.resultingDescriptor
            else -> null
        }?.name?.asString()

        val elementText = render(element)
        val description = if (targetName != null && targetName != elementText) "$elementText, $targetName" else elementText
        return "r($description$inVal) -> $outputValue"
    }

    override fun createCopy(): InstructionImpl =
        ReadValueInstruction(element, blockScope, target, receiverValues, outputValue)
}

class WriteValueInstruction(
    assignment: CjElement,
    blockScope: BlockScope,
    target: AccessTarget,
    receiverValues: Map<PseudoValue, ReceiverValue>,
    val lValue: CjElement,
    private val rValue: PseudoValue
) : AccessValueInstruction(assignment, blockScope, target, receiverValues) {
    override val inputValues: List<PseudoValue>
        get() = (receiverValues.keys as Collection<PseudoValue>) + rValue

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitWriteValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitWriteValue(this)

    override fun toString(): String {
        val lhs = (lValue as? CjNamedDeclaration)?.name ?: render(lValue)
        return "w($lhs|${inputValues.joinToString(", ")})"
    }

    override fun createCopy(): InstructionImpl =
        WriteValueInstruction(element, blockScope, target, receiverValues, lValue, rValue)
}
