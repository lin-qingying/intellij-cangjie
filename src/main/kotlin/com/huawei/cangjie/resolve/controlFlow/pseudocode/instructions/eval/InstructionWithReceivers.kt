

package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval

import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue


interface InstructionWithReceivers : Instruction {
    val receiverValues: Map<PseudoValue, ReceiverValue>
}
