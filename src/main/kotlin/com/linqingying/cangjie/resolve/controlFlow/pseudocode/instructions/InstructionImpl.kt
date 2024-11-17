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

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import java.util.*

abstract class InstructionImpl(override val blockScope: BlockScope) : Instruction
{
    private var _owner: Pseudocode? = null

    override var owner: Pseudocode
        get() = _owner!!
        set(value) {
            assert(_owner == null || _owner == value)
            _owner = value
        }
    private var allCopies: MutableSet<InstructionImpl>? = null
    override val inputValues: List<PseudoValue> = Collections.emptyList()
    override val previousInstructions: MutableCollection<Instruction> = LinkedHashSet()
    protected fun outgoingEdgeTo(target: Instruction?): Instruction? {
        (target as InstructionImpl?)?.previousInstructions?.add(this)
        return target
    }
    fun copy(): Instruction = updateCopyInfo(createCopy())

    protected fun updateCopyInfo(instruction: InstructionImpl): Instruction {
        if (allCopies == null) {
            allCopies = hashSetOf(this)
        }
        instruction.allCopies = allCopies
        allCopies!!.add(instruction)
        return instruction
    }
    var markedAsDead: Boolean = false

    override val copies: Collection<Instruction>
        get() = allCopies?.filter { it != this } ?: Collections.emptyList()
    override val dead: Boolean get() = allCopies?.all { it.markedAsDead } ?: markedAsDead

    protected abstract fun createCopy(): InstructionImpl

}
