
package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Label
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjReturnExpression
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult
import java.util.*

class ReturnValueInstruction(
    returnExpression: CjExpression,
    blockScope: BlockScope,
    targetLabel: Label,
    val returnedValue: PseudoValue,
    val subroutine: CjElement
) : AbstractJumpInstruction(returnExpression, targetLabel, blockScope) {
    override val inputValues: List<PseudoValue> get() = Collections.singletonList(returnedValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitReturnValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitReturnValue(this)

    override fun toString(): String = "ret(*|$returnedValue) $targetLabel"

    override fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction =
        ReturnValueInstruction((element as CjExpression), blockScope, newLabel, returnedValue, subroutine)

    val returnExpressionIfAny: CjReturnExpression? = element as? CjReturnExpression
}
