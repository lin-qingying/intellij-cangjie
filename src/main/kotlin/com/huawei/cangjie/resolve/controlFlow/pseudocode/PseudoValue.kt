package com.huawei.cangjie.resolve.controlFlow.pseudocode

import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.InstructionWithValue
import com.huawei.cangjie.psi.CjElement

interface PseudoValue {
    val debugName: String
    val element: CjElement?
    val createdAt: InstructionWithValue?
}
interface PseudoValueFactory {
    fun newValue(element: CjElement?, instruction: InstructionWithValue?): PseudoValue
}
