/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special.*

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
