

package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special

import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionWithNext

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
