
package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.*


class SubroutineEnterInstruction(
    val subroutine: CjElement,
    blockScope: BlockScope
) : InstructionWithNext(subroutine, blockScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitSubroutineEnter(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitSubroutineEnter(this)

    override fun toString(): String = "<START>"

    override fun createCopy(): InstructionImpl =
        SubroutineEnterInstruction(subroutine, blockScope)
}
