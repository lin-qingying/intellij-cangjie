package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval

import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstruction

interface InstructionWithValue : CjElementInstruction {
    val outputValue: PseudoValue?
}
