
package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.jumps

import com.huawei.cangjie.resolve.controlFlow.pseudocode.Label
import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.huawei.cangjie.psi.CjThrowExpression
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitorWithResult
import java.util.*


class ThrowExceptionInstruction(
    expression: CjThrowExpression,
    blockScope: BlockScope,
    errorLabel: Label,
    private val thrownValue: PseudoValue
) : AbstractJumpInstruction(expression, errorLabel, blockScope) {
    override val inputValues: List<PseudoValue> get() = Collections.singletonList(thrownValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitThrowExceptionInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitThrowExceptionInstruction(this)

    override fun toString(): String = "throw (${element.text}|$thrownValue)"

    override fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction =
        ThrowExceptionInstruction((element as CjThrowExpression), blockScope, newLabel, thrownValue)
}
