package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special

import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjEnumEntry
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.CjVariableDeclaration
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.*

class VariableDeclarationInstruction(
    element: CjDeclaration,
    blockScope: BlockScope
) : InstructionWithNext(element, blockScope) {
    init {
        assert(element is CjVariableDeclaration || element is CjParameter || element is CjEnumEntry ) {
            "Invalid element: ${render(element)}}"
        }
    }

    val variableDeclarationElement: CjDeclaration
        get() = element as CjDeclaration

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitVariableDeclarationInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitVariableDeclarationInstruction(this)

    override fun toString(): String = "v(${render(element)})"

    override fun createCopy(): InstructionImpl =
        VariableDeclarationInstruction(variableDeclarationElement, blockScope)
}
