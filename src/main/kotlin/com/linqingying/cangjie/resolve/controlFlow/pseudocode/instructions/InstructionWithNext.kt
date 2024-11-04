package com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions

import com.linqingying.cangjie.psi.CjElement
import com.intellij.psi.PsiElement

abstract class InstructionWithNext(
    element: CjElement,
    blockScope: BlockScope
) : CjElementInstructionImpl(element, blockScope) {
    var next: Instruction? = null
        set(value) {
            field = outgoingEdgeTo(value)
        }

    override val nextInstructions: Collection<Instruction>
        get() = listOfNotNull(next)
}
abstract class CjElementInstructionImpl(
    override val element: CjElement,
    blockScope: BlockScope
) : InstructionImpl(blockScope), CjElementInstruction {
    protected fun render(element: PsiElement): String =
        element.text?.replace("\\s+".toRegex(), " ") ?: ""
}
