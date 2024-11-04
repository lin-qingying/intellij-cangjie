package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Label
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult


class UnconditionalJumpInstruction(
    element: CjElement,
    targetLabel: Label,
    blockScope: BlockScope
) : AbstractJumpInstruction(element, targetLabel, blockScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitUnconditionalJump(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitUnconditionalJump(this)

    override fun toString(): String = "jmp(${targetLabel.name})"

    override fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction =
        UnconditionalJumpInstruction(element, newLabel, blockScope)
}
