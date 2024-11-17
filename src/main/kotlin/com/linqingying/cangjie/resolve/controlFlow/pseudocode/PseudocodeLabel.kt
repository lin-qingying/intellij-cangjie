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

package com.linqingying.cangjie.resolve.controlFlow.pseudocode

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.linqingying.cangjie.psi.CjElement


class PseudocodeLabel internal constructor(
    override val pseudocode: PseudocodeImpl, override val name: String, private val comment: String?
) : Label {

    private val instructionList: List<Instruction> get() = pseudocode.mutableInstructionList

    private val correspondingElement: CjElement get() = pseudocode.correspondingElement

    override var targetInstructionIndex = -1

    override fun toString(): String = if (comment == null) name else "$name [$comment]"

    override fun resolveToInstruction(): Instruction {
        val index = targetInstructionIndex
        when {
            index < 0 ->
                error(
                    "resolveToInstruction: unbound label $name " +
                            "in subroutine ${correspondingElement.text} with instructions $instructionList"
                )
            index >= instructionList.size ->
                error(
                    "resolveToInstruction: incorrect index $index for label $name " +
                            "in subroutine ${correspondingElement.text} with instructions $instructionList"
                )
            else ->
                return instructionList[index]
        }
    }

    fun copy(newPseudocode: PseudocodeImpl, newLabelIndex: Int): PseudocodeLabel =
        PseudocodeLabel(newPseudocode, "L" + newLabelIndex, "copy of $name, $comment")
}
