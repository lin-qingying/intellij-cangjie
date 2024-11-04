
package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Label
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.*


class NondeterministicJumpInstruction(
    element: CjElement,
    targetLabels: List<Label>,
    blockScope: BlockScope,
    private val inputValue: PseudoValue?
) : CjElementInstructionImpl(element, blockScope), JumpInstruction {
    private var _next: Instruction? = null
    private val _resolvedTargets: MutableMap<Label, Instruction> = linkedMapOf()

    val targetLabels: List<Label> = ArrayList(targetLabels)
    private val resolvedTargets: Map<Label, Instruction>
        get() = _resolvedTargets

    fun setResolvedTarget(label: Label, resolvedTarget: Instruction) {
        _resolvedTargets[label] = outgoingEdgeTo(resolvedTarget)!!
    }

    var next: Instruction
        get() = _next!!
        set(value) {
            _next = outgoingEdgeTo(value)
        }

    override val nextInstructions: Collection<Instruction>
        get() {
            val targetInstructions = ArrayList(resolvedTargets.values)
            targetInstructions.add(next)
            return targetInstructions
        }

    override val inputValues: List<PseudoValue>
        get() = listOfNotNull(inputValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitNondeterministicJump(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitNondeterministicJump(this)

    override fun toString(): String {
        val inVal = if (inputValue != null) "|$inputValue" else ""
        val labels = targetLabels.joinToString(", ") { it.name }
        return "jmp?($labels$inVal)"
    }

    override fun createCopy(): InstructionImpl = createCopy(targetLabels)

    fun copy(newTargetLabels: MutableList<Label>): Instruction = updateCopyInfo(createCopy(newTargetLabels))

    private fun createCopy(newTargetLabels: List<Label>): InstructionImpl =
        NondeterministicJumpInstruction(element, newTargetLabels, blockScope, inputValue)
}
