
package com.linqingying.cangjie.resolve.controlFlow.pseudocode

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.InstructionWithValue
import com.linqingying.cangjie.psi.CjElement


class PseudoValueImpl(
    override val debugName: String,
    override val element: CjElement?,
    override val createdAt: InstructionWithValue?
) : PseudoValue {
    override fun toString(): String = debugName
}

open class PseudoValueFactoryImpl : PseudoValueFactory {
    private var lastIndex: Int = 0

    override fun newValue(element: CjElement?, instruction: InstructionWithValue?): PseudoValue {
        return PseudoValueImpl((instruction?.let { "" } ?: "!") + "<v${lastIndex++}>", element, instruction)
    }
}
