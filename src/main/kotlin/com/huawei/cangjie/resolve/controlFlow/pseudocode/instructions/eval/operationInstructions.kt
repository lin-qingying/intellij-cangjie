

package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval

import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValueFactory
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionWithNext
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue


abstract class OperationInstruction protected constructor(
    element: CjElement,
    blockScope: BlockScope,
    override val inputValues: List<PseudoValue>
) : InstructionWithNext(element, blockScope), InstructionWithValue {
    protected var resultValue: PseudoValue? = null

    override val outputValue: PseudoValue?
        get() = resultValue

    protected fun renderInstruction(name: String, desc: String): String =
        "$name($desc" +
                (if (inputValues.isNotEmpty()) "|${inputValues.joinToString(", ")})" else ")") +
                (if (resultValue != null) " -> $resultValue" else "")

    protected fun setResult(value: PseudoValue?): OperationInstruction {
        this.resultValue = value
        return this
    }

    protected fun setResult(factory: PseudoValueFactory?, valueElement: CjElement? = element): OperationInstruction =
        setResult(factory?.newValue(valueElement, this))
}

class CallInstruction private constructor(
    element: CjElement,
    blockScope: BlockScope,
    val resolvedCall: ResolvedCall<*>,
    override val receiverValues: Map<PseudoValue, ReceiverValue>,
    val arguments: Map<PseudoValue, ValueParameterDescriptor>
) : OperationInstruction(element, blockScope, (receiverValues.keys as Collection<PseudoValue>) + arguments.keys),
    InstructionWithReceivers {

    constructor (
        element: CjElement,
        blockScope: BlockScope,
        resolvedCall: ResolvedCall<*>,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        arguments: Map<PseudoValue, ValueParameterDescriptor>,
        factory: PseudoValueFactory?
    ) : this(element, blockScope, resolvedCall, receiverValues, arguments) {
        setResult(factory)
    }

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitCallInstruction(this)

    override fun createCopy() =
        CallInstruction(element, blockScope, resolvedCall, receiverValues, arguments).setResult(resultValue)

    override fun toString() =
        renderInstruction("call", "${render(element)}, ${resolvedCall.resultingDescriptor!!.name}")
}

// Introduces black-box operation
// Used to:
//      consume input values (so that they aren't considered unused)
//      denote value transformation which can't be expressed by other instructions (such as call or read)
//      pass more than one value to instruction which formally requires only one (e.g. jump)
class MagicInstruction(
    element: CjElement,
    blockScope: BlockScope,
    inputValues: List<PseudoValue>,
    val kind: MagicKind
) : OperationInstruction(element, blockScope, inputValues) {
    constructor (
        element: CjElement,
        valueElement: CjElement?,
        blockScope: BlockScope,
        inputValues: List<PseudoValue>,
        kind: MagicKind,
        factory: PseudoValueFactory
    ) : this(element, blockScope, inputValues, kind) {
        setResult(factory, valueElement)
    }

    val synthetic: Boolean get() = outputValue.element == null

    override val outputValue: PseudoValue
        get() = resultValue!!

    override fun accept(visitor: InstructionVisitor) = visitor.visitMagic(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMagic(this)

    override fun createCopy() =
        MagicInstruction(element, blockScope, inputValues, kind).setResult(resultValue)

    override fun toString() = renderInstruction("magic[$kind]", render(element))
}

enum class MagicKind(val sideEffectFree: Boolean = false) {
    // builtin operations
    STRING_TEMPLATE(true),
    AND(true),
    OR(true),
    NOT_NULL_ASSERTION(),
    EQUALS_IN_WHEN_CONDITION(),
    IS(),
    CAST(),
    UNBOUND_CALLABLE_REFERENCE(true),
    BOUND_CALLABLE_REFERENCE(true),
    // implicit operations
    LOOP_RANGE_ITERATION(),
    IMPLICIT_RECEIVER(),
    VALUE_CONSUMER(),
    // unrecognized operations
    UNRESOLVED_CALL(),
    UNSUPPORTED_ELEMENT(),
    UNRECOGNIZED_WRITE_RHS(),
    FAKE_INITIALIZER(),
    EXHAUSTIVE_WHEN_ELSE()
}

// Merges values produced by alternative control-flow paths (such as 'if' branches)
class MergeInstruction private constructor(
    element: CjElement,
    blockScope: BlockScope,
    inputValues: List<PseudoValue>
) : OperationInstruction(element, blockScope, inputValues) {
    constructor (
        element: CjElement,
        blockScope: BlockScope,
        inputValues: List<PseudoValue>,
        factory: PseudoValueFactory
    ) : this(element, blockScope, inputValues) {
        setResult(factory)
    }

    override val outputValue: PseudoValue
        get() = resultValue!!

    override fun accept(visitor: InstructionVisitor) = visitor.visitMerge(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMerge(this)

    override fun createCopy() = MergeInstruction(element, blockScope, inputValues).setResult(resultValue)

    override fun toString() = renderInstruction("merge", render(element))
}
