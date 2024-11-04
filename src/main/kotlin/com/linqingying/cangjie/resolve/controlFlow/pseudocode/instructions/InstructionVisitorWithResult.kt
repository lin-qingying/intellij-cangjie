package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special.*

abstract class InstructionVisitorWithResult<out R>
{
    open fun visitWriteValue(instruction: WriteValueInstruction): R = visitAccessInstruction(instruction)
    open fun visitReturnValue(instruction: ReturnValueInstruction): R = visitJump(instruction)
    open fun visitThrowExceptionInstruction(instruction: ThrowExceptionInstruction): R = visitJump(instruction)
    open fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction): R = visitJump(instruction)
    open fun visitLoadUnitValue(instruction: LoadUnitValueInstruction): R = visitInstructionWithNext(instruction)

    abstract fun visitInstruction(instruction: Instruction): R
    open fun visitInstructionWithNext(instruction: InstructionWithNext): R = visitInstruction(instruction)
    open fun visitLocalFunctionDeclarationInstruction(instruction: LocalFunctionDeclarationInstruction): R =
        visitInstructionWithNext(instruction)
    open fun visitInlinedFunctionDeclarationInstruction(instruction: InlinedLocalFunctionDeclarationInstruction): R =
        visitLocalFunctionDeclarationInstruction(instruction)
    open fun visitMarkInstruction(instruction: MarkInstruction): R = visitInstructionWithNext(instruction)
    open fun visitSubroutineEnter(instruction: SubroutineEnterInstruction): R = visitInstructionWithNext(instruction)
    open fun visitCallInstruction(instruction: CallInstruction): R = visitOperation(instruction)
    open fun visitOperation(instruction: OperationInstruction): R = visitInstructionWithNext(instruction)
    open fun visitMagic(instruction: MagicInstruction): R = visitOperation(instruction)
    open fun visitMerge(instruction: MergeInstruction): R = visitOperation(instruction)
    open fun visitReadValue(instruction: ReadValueInstruction): R = visitAccessInstruction(instruction)
    open fun visitAccessInstruction(instruction: AccessValueInstruction): R = visitInstructionWithNext(instruction)
    open fun visitConditionalJump(instruction: ConditionalJumpInstruction): R = visitJump(instruction)
    open fun visitJump(instruction: AbstractJumpInstruction): R = visitInstruction(instruction)
    open fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction): R = visitInstruction(instruction)
    open fun visitReturnNoValue(instruction: ReturnNoValueInstruction): R = visitJump(instruction)

    open fun visitSubroutineExit(instruction: SubroutineExitInstruction): R = visitInstruction(instruction)
    open fun visitSubroutineSink(instruction: SubroutineSinkInstruction): R = visitInstruction(instruction)
    open fun visitVariableDeclarationInstruction(instruction: VariableDeclarationInstruction): R = visitInstructionWithNext(instruction)

}
