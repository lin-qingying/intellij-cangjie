/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.controlFlow.variable

import org.cangnova.cangjie.cfg.pseudocodeTraverser.Edges
import org.cangnova.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import org.cangnova.cangjie.cfg.pseudocodeTraverser.traverse
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContextUtils.variableDescriptorForDeclaration
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.PseudocodeUtil
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicKind
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.ReadValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.WriteValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.VariableDeclarationInstruction
import org.cangnova.cangjie.utils.ImmutableHashMap
import org.cangnova.cangjie.utils.ImmutableHashSet
import org.cangnova.cangjie.utils.ImmutableMap
import org.cangnova.cangjie.utils.ImmutableSet


class PseudocodeVariablesData(val pseudocode: Pseudocode, private val bindingContext: BindingContext) {
    private val containsDoWhile = pseudocode.rootPseudocode.containsDoWhile
    private val pseudocodeVariableDataCollector =
        PseudocodeVariableDataCollector(bindingContext, pseudocode)

    private class VariablesForDeclaration(
        val valsWithTrivialInitializer: Set<VariableDescriptor>,
        val nonTrivialVariables: Set<VariableDescriptor>
    ) {
        val allVars =
            if (nonTrivialVariables.isEmpty())
                valsWithTrivialInitializer
            else
                LinkedHashSet(valsWithTrivialInitializer).also { it.addAll(nonTrivialVariables) }
    }

    private val declaredVariablesForDeclaration = hashMapOf<Pseudocode, VariablesForDeclaration>()
    private val rootVariables by lazy(LazyThreadSafetyMode.NONE) {
        getAllDeclaredVariables(pseudocode, includeInsideLocalDeclarations = true)
    }

    val variableInitializers: Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>> by lazy {
        computeVariableInitializers()
    }

    val blockScopeVariableInfo: BlockScopeVariableInfo
        get() = pseudocodeVariableDataCollector.blockScopeVariableInfo

    fun getDeclaredVariables(pseudocode: Pseudocode, includeInsideLocalDeclarations: Boolean): Set<VariableDescriptor> =
        getAllDeclaredVariables(pseudocode, includeInsideLocalDeclarations).allVars

    fun isVariableWithTrivialInitializer(variableDescriptor: VariableDescriptor) =
        variableDescriptor in rootVariables.valsWithTrivialInitializer

    private fun getAllDeclaredVariables(
        pseudocode: Pseudocode,
        includeInsideLocalDeclarations: Boolean
    ): VariablesForDeclaration {
        if (!includeInsideLocalDeclarations) {
            return getUpperLevelDeclaredVariables(pseudocode)
        }
        val nonTrivialVariables = linkedSetOf<VariableDescriptor>()
        val valsWithTrivialInitializer = linkedSetOf<VariableDescriptor>()
        addVariablesFromPseudocode(pseudocode, nonTrivialVariables, valsWithTrivialInitializer)

        for (localFunctionDeclarationInstruction in pseudocode.localDeclarations) {
            val localPseudocode = localFunctionDeclarationInstruction.body
            addVariablesFromPseudocode(localPseudocode, nonTrivialVariables, valsWithTrivialInitializer)
        }
        return VariablesForDeclaration(
            valsWithTrivialInitializer,
            nonTrivialVariables
        )
    }

    private fun addVariablesFromPseudocode(
        pseudocode: Pseudocode,
        nonTrivialVariables: MutableSet<VariableDescriptor>,
        valsWithTrivialInitializer: MutableSet<VariableDescriptor>
    ) {
        getUpperLevelDeclaredVariables(pseudocode).let {
            nonTrivialVariables.addAll(it.nonTrivialVariables)
            valsWithTrivialInitializer.addAll(it.valsWithTrivialInitializer)
        }
    }

    private fun getUpperLevelDeclaredVariables(pseudocode: Pseudocode) =
        declaredVariablesForDeclaration.getOrPut(pseudocode) {
            computeDeclaredVariablesForPseudocode(pseudocode)
        }

    private fun computeDeclaredVariablesForPseudocode(pseudocode: Pseudocode): VariablesForDeclaration {
        val valsWithTrivialInitializer = linkedSetOf<VariableDescriptor>()
        val nonTrivialVariables = linkedSetOf<VariableDescriptor>()
        for (instruction in pseudocode.instructions) {
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.variableDeclarationElement
                val descriptor =
                    variableDescriptorForDeclaration(
                        bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                    ) ?: continue

                if (!containsDoWhile && isValWithTrivialInitializer(variableDeclarationElement, descriptor)) {
                    valsWithTrivialInitializer.add(descriptor)
                } else {
                    nonTrivialVariables.add(descriptor)
                }
            }
        }

        return VariablesForDeclaration(
            valsWithTrivialInitializer,
            nonTrivialVariables
        )
    }

    private fun isValWithTrivialInitializer(variableDeclarationElement: CjDeclaration, descriptor: VariableDescriptor) =
        variableDeclarationElement is CjParameter ||
                (variableDeclarationElement as? CjVariableDeclaration)?.isVariableWithTrivialInitializer(descriptor) == true

    private fun CjVariableDeclaration.isVariableWithTrivialInitializer(descriptor: VariableDescriptor): Boolean {
        if (descriptor.isPropertyWithoutBackingField()) return true
        if (isVar) return false
        return initializer != null
    }

    private fun VariableDescriptor.isPropertyWithoutBackingField(): Boolean {
        if (this !is PropertyDescriptor) return false
        return bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) != true
    }

    // variable initializers

    private fun computeVariableInitializers(): Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>> {

        val blockScopeVariableInfo = pseudocodeVariableDataCollector.blockScopeVariableInfo

        val resultForValsWithTrivialInitializer = computeInitInfoForTrivialVals()

        if (rootVariables.nonTrivialVariables.isEmpty()) return resultForValsWithTrivialInitializer

        return pseudocodeVariableDataCollector.collectData(
            TraversalOrder.FORWARD,
            VariableInitControlFlowInfo()
        ) { instruction: Instruction, incomingEdgesData: Collection<VariableInitControlFlowInfo> ->

            val enterInstructionData =
                mergeIncomingEdgesDataForInitializers(
                    instruction,
                    incomingEdgesData,
                    blockScopeVariableInfo
                )
            val exitInstructionData = addVariableInitStateFromCurrentInstructionIfAny(
                instruction, enterInstructionData, blockScopeVariableInfo
            )
            Edges(enterInstructionData, exitInstructionData)
        }.mapValues { (instruction, edges) ->
            val trivialEdges = resultForValsWithTrivialInitializer[instruction]!!
            Edges(
                trivialEdges.incoming.replaceDelegate(edges.incoming),
                trivialEdges.outgoing.replaceDelegate(edges.outgoing)
            )
        }
    }

    private fun computeInitInfoForTrivialVals(): Map<Instruction, Edges<ReadOnlyInitVariableControlFlowInfoImpl>> {
        val result = hashMapOf<Instruction, Edges<ReadOnlyInitVariableControlFlowInfoImpl>>()
        var declaredSet = ImmutableHashSet.empty<VariableDescriptor>()
        var initSet = ImmutableHashSet.empty<VariableDescriptor>()
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            val enterState = ReadOnlyInitVariableControlFlowInfoImpl(declaredSet, initSet, null)
            when (instruction) {
                is VariableDeclarationInstruction ->
                    extractValWithTrivialInitializer(instruction)?.let { variableDescriptor ->
                        declaredSet = declaredSet.add(variableDescriptor)
                    }

                is WriteValueInstruction -> {
                    val variableDescriptor = extractValWithTrivialInitializer(instruction)
                    if (variableDescriptor != null && instruction.isTrivialInitializer()) {
                        initSet = initSet.add(variableDescriptor)
                    }
                }
            }

            val afterState = ReadOnlyInitVariableControlFlowInfoImpl(declaredSet, initSet, null)

            result[instruction] = Edges(enterState, afterState)
        }
        return result
    }

    private fun WriteValueInstruction.isTrivialInitializer() =
    // WriteValueInstruction having CjDeclaration as an element means
    // it must be a write happened at the same time when
        // the variable (common variable/parameter/object) has been declared
        element is CjDeclaration

    private inner class ReadOnlyInitVariableControlFlowInfoImpl(
        val declaredSet: ImmutableSet<VariableDescriptor>,
        val initSet: ImmutableSet<VariableDescriptor>,
        private val delegate: VariableInitReadOnlyControlFlowInfo?
    ) : VariableInitReadOnlyControlFlowInfo {
        override fun getOrNull(key: VariableDescriptor): VariableControlFlowState? {
            if (key in declaredSet) {
                return VariableControlFlowState.create(isInitialized = key in initSet, isDeclared = true)
            }
            return delegate?.getOrNull(key)
        }

        override fun checkDefiniteInitializationInMatch(merge: VariableInitReadOnlyControlFlowInfo): Boolean =
            delegate?.checkDefiniteInitializationInMatch(merge) ?: false

        fun replaceDelegate(newDelegate: VariableInitReadOnlyControlFlowInfo): VariableInitReadOnlyControlFlowInfo =
            ReadOnlyInitVariableControlFlowInfoImpl(declaredSet, initSet, newDelegate)

        override fun asMap(): ImmutableMap<VariableDescriptor, VariableControlFlowState> {
            val initial = delegate?.asMap() ?: ImmutableHashMap.empty()

            return declaredSet.fold(initial) { acc, variableDescriptor ->
                acc.put(variableDescriptor, getOrNull(variableDescriptor)!!)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ReadOnlyInitVariableControlFlowInfoImpl

            if (declaredSet != other.declaredSet) return false
            if (initSet != other.initSet) return false
            if (delegate != other.delegate) return false

            return true
        }

        override fun hashCode(): Int {
            var result = declaredSet.hashCode()
            result = 31 * result + initSet.hashCode()
            result = 31 * result + (delegate?.hashCode() ?: 0)
            return result
        }
    }

    private fun addVariableInitStateFromCurrentInstructionIfAny(
        instruction: Instruction,
        enterInstructionData: VariableInitControlFlowInfo,
        blockScopeVariableInfo: BlockScopeVariableInfo
    ): VariableInitControlFlowInfo {
        if (instruction is MagicInstruction) {
            if (instruction.kind === MagicKind.EXHAUSTIVE_MATCH_ELSE) {
                return enterInstructionData.iterator().fold(enterInstructionData) { result, (key, value) ->
                    if (!value.definitelyInitialized()) {
                        result.put(
                            key,
                            VariableControlFlowState.createInitializedExhaustively(value.isDeclared)
                        )
                    } else result
                }
            }
        }
        if (instruction !is WriteValueInstruction && instruction !is VariableDeclarationInstruction) {
            return enterInstructionData
        }
        val variable =
            PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)
                ?.takeIf { it in rootVariables.nonTrivialVariables }
                ?: return enterInstructionData
        var exitInstructionData = enterInstructionData
        if (instruction is WriteValueInstruction) {
            // if writing to already initialized object
            if (!PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, bindingContext)) {
                return enterInstructionData
            }

            val enterInitState = enterInstructionData.getOrNull(variable)
            val initializationAtThisElement =
                VariableControlFlowState.create(instruction.element is CjProperty, enterInitState)
            exitInstructionData = exitInstructionData.put(variable, initializationAtThisElement, enterInitState)
        } else {
            // instruction instanceof VariableDeclarationInstruction
            val enterInitState =
                enterInstructionData.getOrNull(variable)
                    ?: getDefaultValueForInitializers(
                        variable,
                        instruction,
                        blockScopeVariableInfo
                    )

            if (!enterInitState.mayBeInitialized() || !enterInitState.isDeclared) {
                val variableDeclarationInfo =
                    VariableControlFlowState.create(enterInitState.initState, isDeclared = true)
                exitInstructionData = exitInstructionData.put(variable, variableDeclarationInfo, enterInitState)
            }
        }
        return exitInstructionData
    }

    // variable use

    val variableUseStatusData: Map<Instruction, Edges<VariableUsageReadOnlyControlInfo>>
        get() {
            val edgesForTrivialVals = computeUseInfoForTrivialVals()
            if (rootVariables.nonTrivialVariables.isEmpty()) {
                return hashMapOf<Instruction, Edges<VariableUsageReadOnlyControlInfo>>().apply {
                    pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
                        put(instruction, edgesForTrivialVals)
                    }
                }
            }

            return pseudocodeVariableDataCollector.collectData(
                TraversalOrder.BACKWARD,
                UsageVariableControlFlowInfo()
            ) { instruction: Instruction, incomingEdgesData: Collection<UsageVariableControlFlowInfo> ->

                val enterResult: UsageVariableControlFlowInfo = if (incomingEdgesData.size == 1) {
                    incomingEdgesData.single()
                } else {
                    incomingEdgesData.fold(UsageVariableControlFlowInfo()) { result, edgeData ->
                        edgeData.iterator().fold(result) { subResult, (variableDescriptor, variableUseState) ->
                            subResult.put(
                                variableDescriptor,
                                variableUseState.merge(subResult.getOrNull(variableDescriptor))
                            )
                        }
                    }
                }

                val variableDescriptor =
                    PseudocodeUtil.extractVariableDescriptorFromReference(instruction, bindingContext)
                        ?.takeIf { it in rootVariables.nonTrivialVariables }
                if (variableDescriptor == null || instruction !is ReadValueInstruction && instruction !is WriteValueInstruction) {
                    Edges(enterResult, enterResult)
                } else {
                    val exitResult =
                        if (instruction is ReadValueInstruction) {
                            enterResult.put(variableDescriptor, VariableUseState.READ)
                        } else {
                            var variableUseState: VariableUseState? = enterResult.getOrNull(variableDescriptor)
                            if (variableUseState == null) {
                                variableUseState = VariableUseState.UNUSED
                            }
                            when (variableUseState) {
                                VariableUseState.UNUSED, VariableUseState.ONLY_WRITTEN_NEVER_READ ->
                                    enterResult.put(variableDescriptor, VariableUseState.ONLY_WRITTEN_NEVER_READ)

                                VariableUseState.WRITTEN_AFTER_READ, VariableUseState.READ ->
                                    enterResult.put(variableDescriptor, VariableUseState.WRITTEN_AFTER_READ)
                            }
                        }
                    Edges(enterResult, exitResult)
                }
            }.mapValues { (_, edges) ->
                Edges(
                    edgesForTrivialVals.incoming.replaceDelegate(edges.incoming),
                    edgesForTrivialVals.outgoing.replaceDelegate(edges.outgoing)
                )
            }
        }

    private fun computeUseInfoForTrivialVals(): Edges<ReadOnlyUseControlFlowInfoImpl> {
        val used = hashSetOf<VariableDescriptor>()

        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction is ReadValueInstruction) {
                extractValWithTrivialInitializer(instruction)?.let {
                    used.add(it)
                }
            }
        }

        val constantUseInfo = ReadOnlyUseControlFlowInfoImpl(used, null)
        return Edges(constantUseInfo, constantUseInfo)
    }

    private fun extractValWithTrivialInitializer(instruction: Instruction): VariableDescriptor? {
        return PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)?.takeIf {
            it in rootVariables.valsWithTrivialInitializer
        }
    }

    private inner class ReadOnlyUseControlFlowInfoImpl(
        val used: Set<VariableDescriptor>,
        val delegate: VariableUsageReadOnlyControlInfo?
    ) : VariableUsageReadOnlyControlInfo {
        override fun getOrNull(key: VariableDescriptor): VariableUseState? {
            if (key in used) return VariableUseState.READ
            return delegate?.getOrNull(key)
        }

        fun replaceDelegate(newDelegate: VariableUsageReadOnlyControlInfo): VariableUsageReadOnlyControlInfo =
            ReadOnlyUseControlFlowInfoImpl(used, newDelegate)

        override fun asMap(): ImmutableMap<VariableDescriptor, VariableUseState> {
            val initial = delegate?.asMap() ?: ImmutableHashMap.empty()

            return used.fold(initial) { acc, variableDescriptor ->
                acc.put(variableDescriptor, getOrNull(variableDescriptor)!!)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ReadOnlyUseControlFlowInfoImpl

            if (used != other.used) return false
            if (delegate != other.delegate) return false

            return true
        }

        override fun hashCode(): Int {
            var result = used.hashCode()
            result = 31 * result + (delegate?.hashCode() ?: 0)
            return result
        }

    }

    companion object {

        
        fun getDefaultValueForInitializers(
            variable: VariableDescriptor,
            instruction: Instruction,
            blockScopeVariableInfo: BlockScopeVariableInfo
        ): VariableControlFlowState {
            //todo: think of replacing it with "MapWithDefaultValue"
            val declaredIn = blockScopeVariableInfo.declaredIn[variable]
            val declaredOutsideThisDeclaration =
                declaredIn == null //declared outside this pseudocode
                        || declaredIn.blockScopeForContainingDeclaration != instruction.blockScope.blockScopeForContainingDeclaration
            return VariableControlFlowState.create(isInitialized = declaredOutsideThisDeclaration)
        }

        private val EMPTY_INIT_CONTROL_FLOW_INFO = VariableInitControlFlowInfo()

        private fun mergeIncomingEdgesDataForInitializers(
            instruction: Instruction,
            incomingEdgesData: Collection<VariableInitControlFlowInfo>,
            blockScopeVariableInfo: BlockScopeVariableInfo
        ): VariableInitControlFlowInfo {
            if (incomingEdgesData.size == 1) return incomingEdgesData.single()
            if (incomingEdgesData.isEmpty()) return EMPTY_INIT_CONTROL_FLOW_INFO
            val variablesInScope = linkedSetOf<VariableDescriptor>()
            for (edgeData in incomingEdgesData) {
                variablesInScope.addAll(edgeData.keySet())
            }

            return variablesInScope.fold(EMPTY_INIT_CONTROL_FLOW_INFO) { result, variable ->
                var initState: InitState? = null
                var isDeclared = true
                for (edgeData in incomingEdgesData) {
                    val varControlFlowState = edgeData.getOrNull(variable)
                        ?: getDefaultValueForInitializers(
                            variable,
                            instruction,
                            blockScopeVariableInfo
                        )
                    initState = initState?.merge(varControlFlowState.initState) ?: varControlFlowState.initState
                    if (!varControlFlowState.isDeclared) {
                        isDeclared = false
                    }
                }
                if (initState == null) {
                    throw AssertionError("An empty set of incoming edges data")
                }
                result.put(variable, VariableControlFlowState.create(initState, isDeclared))
            }
        }
    }
}
