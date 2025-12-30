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

import org.cangnova.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement

interface CjElementInstruction : Instruction {
    val element: CjElement
}

interface Instruction {
    var owner: Pseudocode

    val previousInstructions: Collection<Instruction>
    val nextInstructions: Collection<Instruction>

    val dead: Boolean

    val blockScope: BlockScope

    val inputValues: List<PseudoValue>

    val copies: Collection<Instruction>

    fun accept(visitor: InstructionVisitor)
    fun <R> accept(visitor: InstructionVisitorWithResult<R>): R
}

class BlockScope(private val parentScope: BlockScope?, val block: CjElement) {
    val depth: Int = (parentScope?.depth ?: 0) + 1

    val blockScopeForContainingDeclaration: BlockScope? by lazy {
        var scope: BlockScope? = this
        while (scope != null) {
            if (scope.block is CjDeclaration) {
                break
            }
            scope = scope.parentScope
        }
        scope
    }
}
