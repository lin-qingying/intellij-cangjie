
package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval

import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.*


class LoadUnitValueInstruction(
    expression: CjExpression,
    blockScope: BlockScope
) : InstructionWithNext(expression, blockScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitLoadUnitValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitLoadUnitValue(this)
    }

    override fun toString(): String =
        "read (Unit)"

    override fun createCopy(): InstructionImpl =
        LoadUnitValueInstruction(element as CjExpression, blockScope)
}
