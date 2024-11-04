package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Label
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstructionImpl
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionImpl
import com.linqingying.cangjie.psi.CjElement


abstract class AbstractJumpInstruction(
    element: CjElement,
    val targetLabel: Label,
    blockScope: BlockScope
) : CjElementInstructionImpl(element, blockScope), JumpInstruction {
    var resolvedTarget: Instruction? = null
        set(value) {
            field = outgoingEdgeTo(value)
        }

    protected abstract fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction

    fun copy(newLabel: Label): Instruction = updateCopyInfo(createCopy(newLabel, blockScope))

    override fun createCopy(): InstructionImpl = createCopy(targetLabel, blockScope)

    override val nextInstructions: Collection<Instruction>
        get() = listOfNotNull(resolvedTarget)
}
