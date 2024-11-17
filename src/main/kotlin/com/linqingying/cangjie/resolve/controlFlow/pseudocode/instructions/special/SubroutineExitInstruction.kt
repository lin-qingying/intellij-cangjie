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
