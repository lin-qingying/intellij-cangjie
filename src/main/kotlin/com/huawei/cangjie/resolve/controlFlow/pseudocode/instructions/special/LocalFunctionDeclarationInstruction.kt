
package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special

import com.huawei.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.*


open class LocalFunctionDeclarationInstruction(
    element: CjElement,
    val body: Pseudocode,
    blockScope: BlockScope
) : InstructionWithNext(element, blockScope) {
    var sink: SubroutineSinkInstruction? = null
        set(value) {
            field = outgoingEdgeTo(value) as SubroutineSinkInstruction?
        }

    override val nextInstructions: Collection<Instruction>
        get() {
            sink?.let {
                val instructions = arrayListOf<Instruction>(it)
                instructions.addAll(super.nextInstructions)
                return instructions
            }
            return super.nextInstructions
        }

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitLocalFunctionDeclarationInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitLocalFunctionDeclarationInstruction(this)

    override fun toString(): String = "d(${render(element)})"

    override fun createCopy(): InstructionImpl =
        LocalFunctionDeclarationInstruction(element, body.copy(), blockScope)
}
