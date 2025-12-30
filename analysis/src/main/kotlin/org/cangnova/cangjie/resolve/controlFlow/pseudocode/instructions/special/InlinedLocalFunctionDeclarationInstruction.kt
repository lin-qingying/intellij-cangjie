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
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionImpl
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult
import org.cangnova.cangjie.contracts.description.EventOccurrencesRange
import org.cangnova.cangjie.psi.CjElement


class InlinedLocalFunctionDeclarationInstruction(
    element: CjElement,
    body: Pseudocode,
    blockScope: BlockScope,
    val kind: EventOccurrencesRange
) : LocalFunctionDeclarationInstruction(element, body, blockScope) {
    override fun createCopy(): InstructionImpl =
        InlinedLocalFunctionDeclarationInstruction(element, body.copy(), blockScope, kind)

    override fun accept(visitor: InstructionVisitor) = visitor.visitInlinedLocalFunctionDeclarationInstruction(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R =
        visitor.visitInlinedFunctionDeclarationInstruction(this)

    override fun toString(): String = "inlined(${render(element)})"
}
