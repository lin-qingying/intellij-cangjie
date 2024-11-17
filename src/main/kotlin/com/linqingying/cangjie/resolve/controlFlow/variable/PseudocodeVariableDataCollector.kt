/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.resolve.controlFlow.variable

import com.linqingying.cangjie.cfg.pseudocodeTraverser.Edges
import com.linqingying.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import com.linqingying.cangjie.cfg.pseudocodeTraverser.collectData
import com.linqingying.cangjie.cfg.pseudocodeTraverser.traverse
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.BindingContextUtils
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.BlockScope
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special.VariableDeclarationInstruction


class PseudocodeVariableDataCollector(
    private val bindingContext: BindingContext,
    private val pseudocode: Pseudocode
) {
    val blockScopeVariableInfo = computeBlockScopeVariableInfo(pseudocode)

    fun <I : VariableUsageControlFlowInfo<*, *>> collectData(
        traversalOrder: TraversalOrder,
        initialInfo: I,
        instructionDataMergeStrategy: (Instruction, Collection<I>) -> Edges<I>
    ): Map<Instruction, Edges<I>> {
        return pseudocode.collectData(
            traversalOrder,
            instructionDataMergeStrategy,
            { from, to, info -> filterOutVariablesOutOfScope(from, to, info) },
            initialInfo
        )
    }

    private fun <I : VariableUsageControlFlowInfo<*, *>> filterOutVariablesOutOfScope(
        from: Instruction,
        to: Instruction,
        info: I
    ): I {
        // If an edge goes from deeper scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.blockScope.depth
        if (toDepth >= from.blockScope.depth) return info

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        @Suppress("UNCHECKED_CAST")
        return info.retainAll { variable ->
            val blockScope = blockScopeVariableInfo.declaredIn[variable]
            // '-1' for variables declared outside this pseudocode
            val depth = blockScope?.depth ?: -1
            depth <= toDepth
        } as I
    }

    private fun computeBlockScopeVariableInfo(pseudocode: Pseudocode): BlockScopeVariableInfo {
        val blockScopeVariableInfo = BlockScopeVariableInfoImpl()
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.variableDeclarationElement
                val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement) ?: return@traverse
                val variableDescriptor = BindingContextUtils.variableDescriptorForDeclaration(descriptor)
                    ?: throw AssertionError(
                        "Variable or class descriptor should correspond to " +
                                "the instruction for ${instruction.element.text}.\n" +
                                "Descriptor: $descriptor"
                    )
                blockScopeVariableInfo.registerVariableDeclaredInScope(variableDescriptor, instruction.blockScope)
            }
        }
        return blockScopeVariableInfo
    }
}

interface BlockScopeVariableInfo {
    val declaredIn: Map<VariableDescriptor, BlockScope>
    val scopeVariables: Map<BlockScope, Collection<VariableDescriptor>>
}

class BlockScopeVariableInfoImpl : BlockScopeVariableInfo {
    override val declaredIn = HashMap<VariableDescriptor, BlockScope>()
    override val scopeVariables = HashMap<BlockScope, MutableCollection<VariableDescriptor>>()

    fun registerVariableDeclaredInScope(variable: VariableDescriptor, blockScope: BlockScope) {
        declaredIn[variable] = blockScope
        val variablesInScope = scopeVariables.getOrPut(blockScope, { arrayListOf() })
        variablesInScope.add(variable)
    }
}
