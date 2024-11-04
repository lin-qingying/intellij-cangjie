package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special

import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjVariableDeclaration
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.*

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
