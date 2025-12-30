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


package org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special

import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.*


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

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R =
        visitor.visitLocalFunctionDeclarationInstruction(this)

    override fun toString(): String = "d(${render(element)})"

    override fun createCopy(): InstructionImpl =
        LocalFunctionDeclarationInstruction(element, body.copy(), blockScope)
}
