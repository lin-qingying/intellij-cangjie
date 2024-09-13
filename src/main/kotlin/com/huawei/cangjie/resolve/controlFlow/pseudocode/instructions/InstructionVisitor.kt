package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions

import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special.*

open class InstructionVisitor {
    open fun visitVariableDeclarationInstruction(instruction: VariableDeclarationInstruction) {
        visitInstructionWithNext(instruction)
    }
    open fun visitReadValue(instruction: ReadValueInstruction) {
        visitAccessInstruction(instruction)
    }

    open fun visitLoadUnitValue(instruction: LoadUnitValueInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitThrowExceptionInstruction(instruction: ThrowExceptionInstruction) {
        visitJump(instruction)
    }
    open fun visitReturnNoValue(instruction: ReturnNoValueInstruction) {
        visitJump(instruction)
    }
    open fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
        visitInstruction(instruction)
    }
    open fun visitReturnValue(instruction: ReturnValueInstruction) {
        visitJump(instruction)
    }
    open fun visitJump(instruction: AbstractJumpInstruction) {
        visitInstruction(instruction)
    }
    open fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
        visitJump(instruction)
    }
    open fun visitAccessInstruction(instruction: AccessValueInstruction) {
        visitInstructionWithNext(instruction)
    }
    open fun visitWriteValue(instruction: WriteValueInstruction) {
        visitAccessInstruction(instruction)
    }
    open fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction) {
        visitJump(instruction)
    }
    open fun visitInlinedLocalFunctionDeclarationInstruction(instruction: InlinedLocalFunctionDeclarationInstruction) {
        visitLocalFunctionDeclarationInstruction(instruction)
    }
    open fun visitMagic(instruction: MagicInstruction) {
        visitOperation(instruction)
    }
    open fun visitMerge(instruction: MergeInstruction) {
        visitOperation(instruction)
    }

    open fun visitInstructionWithNext(instruction: InstructionWithNext) {
        visitInstruction(instruction)
    }
    open fun visitOperation(instruction: OperationInstruction) {
        visitInstructionWithNext(instruction)
    }
    open fun visitCallInstruction(instruction: CallInstruction) {
        visitOperation(instruction)
    }
    open fun visitSubroutineEnter(instruction: SubroutineEnterInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitMarkInstruction(instruction: MarkInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitLocalFunctionDeclarationInstruction(instruction: LocalFunctionDeclarationInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitSubroutineExit(instruction: SubroutineExitInstruction) {
        visitInstruction(instruction)
    }

    open fun visitInstruction(instruction: Instruction) {
    }

    open fun visitSubroutineSink(instruction: SubroutineSinkInstruction) {
        visitInstruction(instruction)
    }
}
