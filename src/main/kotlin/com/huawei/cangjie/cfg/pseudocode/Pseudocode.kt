package com.huawei.cangjie.cfg.pseudocode

import com.huawei.cangjie.psi.CjElement


interface Pseudocode {
    val correspondingElement: CjElement

    val parent: Pseudocode?

//    val localDeclarations: Set<LocalFunctionDeclarationInstruction>
//
//    val instructions: List<Instruction>
//
//    val reversedInstructions: List<Instruction>
//
//    val instructionsIncludingDeadCode: List<Instruction>
//
//    val exitInstruction: SubroutineExitInstruction
//
//    val errorInstruction: SubroutineExitInstruction
//
//    val sinkInstruction: SubroutineSinkInstruction
//
//    val enterInstruction: SubroutineEnterInstruction

    val isInlined: Boolean
    val containsDoWhile: Boolean
    val rootPseudocode: Pseudocode

//    fun getElementValue(element: CjElement?): PseudoValue?
//
//    fun getValueElements(value: PseudoValue?): List<CjElement>
//
//    fun getUsages(value: PseudoValue?): List<Instruction>
//
//    fun isSideEffectFree(instruction: Instruction): Boolean

    fun copy(): Pseudocode

//    fun instructionForElement(element: CjElement): CjElementInstruction?
}
class PseudocodeImpl(override val correspondingElement: CjElement, override val isInlined: Boolean) : Pseudocode {
    override var parent: Pseudocode? = null
        private set
    override var containsDoWhile: Boolean = false
        internal set

    override val rootPseudocode: Pseudocode
        get() {
            var parent = parent
            while (parent != null) {
                if (parent.parent == null) return parent
                parent = parent.parent
            }
            return this
        }

    private fun repeatWhole(originalPseudocode: PseudocodeImpl) {
//        repeatInternal(originalPseudocode, null, null, 0)
        parent = originalPseudocode.parent
    }
    override fun copy(): PseudocodeImpl {
        val result = PseudocodeImpl(correspondingElement, isInlined)
        result.repeatWhole(this)
        return result
    }
}
