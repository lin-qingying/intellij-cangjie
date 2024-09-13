package com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions

import com.huawei.cangjie.resolve.controlFlow.pseudocode.PseudoValue
import com.huawei.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjElement

interface CjElementInstruction : Instruction {
    val element: CjElement
}
interface Instruction {
    var owner: Pseudocode

    val previousInstructions: Collection<Instruction>
    val nextInstructions: Collection<Instruction>

    val dead: Boolean

    val blockScope: BlockScope

    val inputValues: List<PseudoValue>

    val copies: Collection<Instruction>

    fun accept(visitor: InstructionVisitor)
    fun <R> accept(visitor: InstructionVisitorWithResult<R>): R
}

class BlockScope(private val parentScope: BlockScope?, val block: CjElement) {
    val depth: Int = (parentScope?.depth ?: 0) + 1

    val blockScopeForContainingDeclaration: BlockScope? by lazy {
        var scope: BlockScope? = this
        while (scope != null) {
            if (scope.block is CjDeclaration) {
                break
            }
            scope = scope.parentScope
        }
        scope
    }
}
