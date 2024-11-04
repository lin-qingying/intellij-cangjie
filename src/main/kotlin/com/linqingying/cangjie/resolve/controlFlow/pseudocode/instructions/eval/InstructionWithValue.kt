package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstruction

interface InstructionWithValue : CjElementInstruction {
    val outputValue: PseudoValue?
}
