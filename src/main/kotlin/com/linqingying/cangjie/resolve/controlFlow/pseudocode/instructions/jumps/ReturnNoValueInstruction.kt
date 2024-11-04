
package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Label
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult


class ReturnNoValueInstruction(
    element: CjElement,
    blockScope: BlockScope,
    targetLabel: Label,
    val subroutine: CjElement
) : AbstractJumpInstruction(element, targetLabel, blockScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitReturnNoValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitReturnNoValue(this)

    override fun toString(): String = "ret $targetLabel"

    override fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction =
        ReturnNoValueInstruction(element, blockScope, newLabel, subroutine)
}
