/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions

import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.*

abstract class InstructionVisitorWithResult<out R> {
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
    open fun visitVariableDeclarationInstruction(instruction: VariableDeclarationInstruction): R =
        visitInstructionWithNext(instruction)

}
