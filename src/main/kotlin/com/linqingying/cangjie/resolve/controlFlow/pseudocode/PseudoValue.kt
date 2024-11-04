package com.linqingying.cangjie.resolve.controlFlow.pseudocode

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.InstructionWithValue
import com.linqingying.cangjie.psi.CjElement

interface PseudoValue {
    val debugName: String
    val element: CjElement?
    val createdAt: InstructionWithValue?
}
interface PseudoValueFactory {
    fun newValue(element: CjElement?, instruction: InstructionWithValue?): PseudoValue
}
