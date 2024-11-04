
package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Label
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult
import com.linqingying.cangjie.psi.CjElement


class ConditionalJumpInstruction(
    element: CjElement,
    val onTrue: Boolean,
    blockScope: BlockScope,
    targetLabel: Label,
    private val conditionValue: PseudoValue?
) : AbstractJumpInstruction(element, targetLabel, blockScope) {
    private var _nextOnTrue: Instruction? = null
    private var _nextOnFalse: Instruction? = null

    var nextOnTrue: Instruction
        get() = _nextOnTrue!!
        set(value) {
            _nextOnTrue = outgoingEdgeTo(value)
        }

    var nextOnFalse: Instruction
        get() = _nextOnFalse!!
        set(value) {
            _nextOnFalse = outgoingEdgeTo(value)
        }

    override val nextInstructions: Collection<Instruction>
        get() = listOf(nextOnFalse, nextOnTrue)

    override val inputValues: List<PseudoValue>
        get() = listOfNotNull(conditionValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitConditionalJump(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitConditionalJump(this)
    }

    override fun toString(): String {
        val instr = if (onTrue) "jt" else "jf"
        val inValue = conditionValue?.let { "|" + it } ?: ""
        return "$instr(${targetLabel.name}$inValue)"
    }

    override fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction =
        ConditionalJumpInstruction(element, onTrue, blockScope, newLabel, conditionValue)
}
