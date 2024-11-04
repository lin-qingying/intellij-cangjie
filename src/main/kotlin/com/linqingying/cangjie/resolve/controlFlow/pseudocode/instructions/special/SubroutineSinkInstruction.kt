package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.*
import java.util.*


class SubroutineSinkInstruction(
    val subroutine: CjElement,
    blockScope: BlockScope,
    private val debugLabel: String
) : InstructionImpl(blockScope) {
    override val nextInstructions: Collection<Instruction>
        get() = Collections.emptyList()


    override fun accept(visitor: InstructionVisitor) {
        visitor.visitSubroutineSink(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitSubroutineSink(this)

    override fun toString(): String = debugLabel

    override fun createCopy(): InstructionImpl =
        SubroutineSinkInstruction(subroutine, blockScope, debugLabel)


}
