

package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval

import com.linqingying.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue


interface InstructionWithReceivers : Instruction {
    val receiverValues: Map<PseudoValue, ReceiverValue>
}
