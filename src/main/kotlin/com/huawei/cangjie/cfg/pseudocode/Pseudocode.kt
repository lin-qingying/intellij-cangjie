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
