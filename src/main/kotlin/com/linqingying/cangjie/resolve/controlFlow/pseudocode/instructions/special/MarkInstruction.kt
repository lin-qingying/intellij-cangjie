

package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionWithNext

class MarkInstruction(
    element: CjElement,
    blockScope: BlockScope
) : InstructionWithNext(element, blockScope) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitMarkInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMarkInstruction(this)

    override fun createCopy() = MarkInstruction(element, blockScope)

    override fun toString() = "mark(${render(element)})"
}
