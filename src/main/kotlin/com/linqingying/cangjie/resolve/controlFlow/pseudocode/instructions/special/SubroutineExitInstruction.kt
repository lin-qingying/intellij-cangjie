package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionImpl
import java.util.*
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult

class SubroutineExitInstruction(
    val subroutine: CjElement,
    blockScope: BlockScope,
    val isError: Boolean
) : InstructionImpl(blockScope) {
    private var _sink: SubroutineSinkInstruction? = null

    var sink: SubroutineSinkInstruction
        get() = _sink!!
        set(value: SubroutineSinkInstruction) {
            _sink = outgoingEdgeTo(value) as SubroutineSinkInstruction
        }

    override val nextInstructions: Collection<Instruction>
        get() = Collections.singleton(sink)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitSubroutineExit(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitSubroutineExit(this)

    override fun toString(): String = if (isError) "<ERROR>" else "<END>"

    override fun createCopy(): InstructionImpl =
        SubroutineExitInstruction(subroutine, blockScope, isError)
}
