package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions

import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.huawei.cangjie.resolve.controlFlow.pseudocode.Pseudocode
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
