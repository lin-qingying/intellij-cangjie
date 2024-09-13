package com.huawei.cangjie.resolve.controlFlow.pseudocode

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicKind
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MergeInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.AbstractJumpInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.ConditionalJumpInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.NondeterministicJumpInstruction
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special.*
import com.intellij.util.containers.BidirectionalMap
import com.huawei.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import com.huawei.cangjie.cfg.pseudocodeTraverser.TraverseInstructionResult
import com.huawei.cangjie.cfg.pseudocodeTraverser.traverseFollowingInstructions


interface Pseudocode {
    val correspondingElement: CjElement

    val parent: Pseudocode?

    val localDeclarations: Set<LocalFunctionDeclarationInstruction>

    val instructions: List<Instruction>


    val reversedInstructions: List<Instruction>

    val instructionsIncludingDeadCode: List<Instruction>

    val exitInstruction: SubroutineExitInstruction

    //
    val errorInstruction: SubroutineExitInstruction

    //
    val sinkInstruction: SubroutineSinkInstruction

    val enterInstruction: SubroutineEnterInstruction

    val isInlined: Boolean
    val containsDoWhile: Boolean
    val rootPseudocode: Pseudocode

    fun getElementValue(element: CjElement?): PseudoValue?

    fun getValueElements(value: PseudoValue?): List<CjElement>

    fun getUsages(value: PseudoValue?): List<Instruction>

    fun isSideEffectFree(instruction: Instruction): Boolean

    fun copy(): Pseudocode

    fun instructionForElement(element: CjElement): CjElementInstruction?
}

class PseudocodeImpl(override val correspondingElement: CjElement, override val isInlined: Boolean) : Pseudocode {
    internal val mutableInstructionList = ArrayList<Instruction>()
    override val instructions = ArrayList<Instruction>()

    private val labels = java.util.ArrayList<PseudocodeLabel>()
    fun createLabel(name: String, comment: String?): PseudocodeLabel {
        val label = PseudocodeLabel(this, name, comment)
        labels.add(label)
        return label
    }
    fun repeatPart(startLabel: Label, finishLabel: Label, labelCount: Int): Int =
        repeatInternal(startLabel.pseudocode as PseudocodeImpl, startLabel, finishLabel, labelCount)

    override val reversedInstructions: List<Instruction>
        get() {
            val traversedInstructions = linkedSetOf<Instruction>()
            traverseFollowingInstructions(
                if (this.isInlined) instructions.last() else sinkInstruction,
                traversedInstructions,
                TraversalOrder.BACKWARD,
                null
            )
            if (traversedInstructions.size < instructions.size) {
                val simplyReversedInstructions = instructions.reversed()
                for (instruction in simplyReversedInstructions) {
                    if (!traversedInstructions.contains(instruction)) {
                        traverseFollowingInstructions(instruction, traversedInstructions, TraversalOrder.BACKWARD, null)
                    }
                }
            }
            return traversedInstructions.toList()
        }
    override val instructionsIncludingDeadCode: List<Instruction>
        get() = mutableInstructionList

    override var parent: Pseudocode? = null
        private set
    override val localDeclarations: Set<LocalFunctionDeclarationInstruction> by lazy {
        getLocalDeclarations(this)
    }

    override var containsDoWhile: Boolean = false
        internal set
    private var postPrecessed = false
    private var internalErrorInstruction: SubroutineExitInstruction? = null
    private val valueUsages = hashMapOf<PseudoValue, MutableList<Instruction>>()
    private val mergedValues = hashMapOf<PseudoValue, Set<PseudoValue>>()
    private val sideEffectFree = hashSetOf<Instruction>()
    private val elementsToValues = BidirectionalMap<CjElement, PseudoValue>()

    override val errorInstruction: SubroutineExitInstruction
        get() = internalErrorInstruction ?: throw AssertionError("Error instruction is read before initialization")
    private var internalExitInstruction: SubroutineExitInstruction? = null

    override val exitInstruction: SubroutineExitInstruction
        get() = internalExitInstruction ?: throw AssertionError("Exit instruction is read before initialization")

    private var internalSinkInstruction: SubroutineSinkInstruction? = null

    override val sinkInstruction: SubroutineSinkInstruction
        get() = internalSinkInstruction ?: throw AssertionError("Sink instruction is read before initialization")
    override val enterInstruction: SubroutineEnterInstruction
        get() = mutableInstructionList[0] as SubroutineEnterInstruction

    val reachableInstructions = hashSetOf<Instruction>()
    private val representativeInstructions = HashMap<CjElement, CjElementInstruction>()

    override val rootPseudocode: Pseudocode
        get() {
            var parent = parent
            while (parent != null) {
                if (parent.parent == null) return parent
                parent = parent.parent
            }
            return this
        }

    private fun getLocalDeclarations(pseudocode: Pseudocode): Set<LocalFunctionDeclarationInstruction> {
        val localDeclarations = linkedSetOf<LocalFunctionDeclarationInstruction>()
        for (instruction in (pseudocode as PseudocodeImpl).mutableInstructionList) {
            if (instruction is LocalFunctionDeclarationInstruction) {
                localDeclarations.add(instruction)
                localDeclarations.addAll(getLocalDeclarations(instruction.body))
            }
        }
        return localDeclarations
    }

    fun bindElementToValue(element:CjElement, value: PseudoValue) {
        elementsToValues.put(element, value)
    }
    override fun getElementValue(element: CjElement?): PseudoValue? = elementsToValues[element]

    override fun getValueElements(value: PseudoValue?): List<CjElement> =
        elementsToValues.getKeysByValue(value) ?: emptyList()

    override fun getUsages(value: PseudoValue?): List<Instruction> = valueUsages[value] ?: mutableListOf()

    override fun isSideEffectFree(instruction: Instruction): Boolean = sideEffectFree.contains(instruction)

    private fun getMergedValues(value: PseudoValue) = mergedValues[value] ?: emptySet()

    private fun addMergedValues(instruction: MergeInstruction) {
        val result = LinkedHashSet<PseudoValue>()
        for (value in instruction.inputValues) {
            result.addAll(getMergedValues(value))
            result.add(value)
        }
        mergedValues.put(instruction.outputValue, result)
    }

    fun addInstruction(instruction: Instruction) {
        mutableInstructionList.add(instruction)
        instruction.owner = this

        if (instruction is CjElementInstruction) {
            val element = instruction.element
            if (!representativeInstructions.containsKey(element)) {
                representativeInstructions[element] = instruction
            }
        }

        if (instruction is MergeInstruction) {
            addMergedValues(instruction)
        }

        for (inputValue in instruction.inputValues) {
            addValueUsage(inputValue, instruction)
            for (mergedValue in getMergedValues(inputValue)) {
                addValueUsage(mergedValue, instruction)
            }
        }
        if (instruction.calcSideEffectFree()) {
            sideEffectFree.add(instruction)
        }
    }

    private fun addValueUsage(value: PseudoValue, usage: Instruction) {
        if (usage is MergeInstruction) return
        valueUsages.getOrPut(
            value
        ) { arrayListOf() }.add(usage)
    }

    fun addSinkInstruction(sinkInstruction: SubroutineSinkInstruction) {
        addInstruction(sinkInstruction)
        assert(internalSinkInstruction == null) {
            "Repeated initialization of sink instruction: $internalSinkInstruction --> $sinkInstruction"
        }
        internalSinkInstruction = sinkInstruction
    }

    fun addErrorInstruction(errorInstruction: SubroutineExitInstruction) {
        addInstruction(errorInstruction)
        assert(internalErrorInstruction == null) {
            "Repeated initialization of error instruction: $internalErrorInstruction --> $errorInstruction"
        }
        internalErrorInstruction = errorInstruction
    }

    fun addExitInstruction(exitInstruction: SubroutineExitInstruction) {
        addInstruction(exitInstruction)
        assert(internalExitInstruction == null) {
            "Repeated initialization of exit instruction: $internalExitInstruction --> $exitInstruction"
        }
        internalExitInstruction = exitInstruction
    }

    fun postProcess() {
        if (postPrecessed) return
        postPrecessed = true
        errorInstruction.sink = sinkInstruction
        exitInstruction.sink = sinkInstruction

        for ((index, instruction) in mutableInstructionList.withIndex()) {
            //recursively invokes 'postProcess' for local declarations, thus it needs global set of reachable instructions
            instruction.processInstruction(index)
        }

        collectAndCacheReachableInstructions()
    }

    private fun collectReachableInstructions() {
        val reachableFromThisPseudocode = hashSetOf<Instruction>()
        traverseFollowingInstructions(
            enterInstruction, reachableFromThisPseudocode, TraversalOrder.FORWARD
        ) { instruction ->
            if (instruction is MagicInstruction && instruction.kind === MagicKind.EXHAUSTIVE_WHEN_ELSE) {
                return@traverseFollowingInstructions TraverseInstructionResult.SKIP
            }
            TraverseInstructionResult.CONTINUE
        }

        // Don't force-add EXIT and ERROR for inlined pseudocodes because for such
        // declarations those instructions has special semantic
        if (!isInlined) {
            reachableFromThisPseudocode.add(exitInstruction)
            reachableFromThisPseudocode.add(errorInstruction)
            reachableFromThisPseudocode.add(sinkInstruction)
        }

        reachableFromThisPseudocode.forEach { (it.owner as PseudocodeImpl).reachableInstructions.add(it) }
    }

    private fun collectAndCacheReachableInstructions() {
        collectReachableInstructions()
        for (instruction in mutableInstructionList) {
            if (reachableInstructions.contains(instruction)) {
                instructions.add(instruction)
            }
        }
        markDeadInstructions()
    }

    private fun markDeadInstructions() {
        val instructionSet = instructions.toHashSet()
        for (instruction in mutableInstructionList) {
            if (!instructionSet.contains(instruction)) {
                (instruction as? InstructionImpl)?.markedAsDead = true
                for (nextInstruction in instruction.nextInstructions) {
                    (nextInstruction as? InstructionImpl)?.previousInstructions?.remove(instruction)
                }
            }
        }
    }

    private fun Instruction.processInstruction(currentPosition: Int) {
        accept(object : InstructionVisitor() {
            override fun visitInstructionWithNext(instruction: InstructionWithNext) {
                instruction.next = getNextPosition(currentPosition)
            }

            override fun visitJump(instruction: AbstractJumpInstruction) {
                instruction.resolvedTarget = getJumpTarget(instruction.targetLabel)
            }

            override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                instruction.next = getNextPosition(currentPosition)
                val targetLabels = instruction.targetLabels
                for (targetLabel in targetLabels) {
                    instruction.setResolvedTarget(targetLabel, getJumpTarget(targetLabel))
                }
            }

            override fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
                val nextInstruction = getNextPosition(currentPosition)
                val jumpTarget = getJumpTarget(instruction.targetLabel)
                if (instruction.onTrue) {
                    instruction.nextOnFalse = nextInstruction
                    instruction.nextOnTrue = jumpTarget
                } else {
                    instruction.nextOnFalse = jumpTarget
                    instruction.nextOnTrue = nextInstruction
                }
                visitJump(instruction)
            }

            override fun visitLocalFunctionDeclarationInstruction(instruction: LocalFunctionDeclarationInstruction) {
                val body = instruction.body as PseudocodeImpl
                body.parent = this@PseudocodeImpl
                body.postProcess()
                instruction.next = sinkInstruction
            }

            override fun visitInlinedLocalFunctionDeclarationInstruction(instruction: InlinedLocalFunctionDeclarationInstruction) {
                val body = instruction.body as PseudocodeImpl
                body.parent = this@PseudocodeImpl
                body.postProcess()
                // Don't add edge to next instruction if flow can't reach exit of inlined declaration
                instruction.next =
                    if (body.instructions.contains(body.exitInstruction)) getNextPosition(currentPosition) else sinkInstruction
            }

            override fun visitSubroutineExit(instruction: SubroutineExitInstruction) {
                // Nothing
            }

            override fun visitSubroutineSink(instruction: SubroutineSinkInstruction) {
                // Nothing
            }

            override fun visitInstruction(instruction: Instruction) {
                throw UnsupportedOperationException(instruction.toString())
            }
        })
    }
    fun bindLabel(label: PseudocodeLabel) {
        assert(this == label.pseudocode) {
            "Attempt to bind label $label to instruction from different pseudocode: " +
                    "\nowner pseudocode = ${label.pseudocode.mutableInstructionList}, " +
                    "\nbound pseudocode = ${this.mutableInstructionList}"
        }
        label.targetInstructionIndex = mutableInstructionList.size
    }
    private fun repeatWhole(originalPseudocode: PseudocodeImpl) {
        repeatInternal(originalPseudocode, null, null, 0)
        parent = originalPseudocode.parent
    }
    private fun getNextPosition(currentPosition: Int): Instruction {
        val targetPosition = currentPosition + 1
        assert(targetPosition < mutableInstructionList.size) { currentPosition }
        return mutableInstructionList[targetPosition]
    }
    private fun repeatInternal(
        originalPseudocode: PseudocodeImpl,
        startLabel: Label?, finishLabel: Label?,
        labelCountArg: Int
    ): Int {
        var labelCount = labelCountArg
        val startIndex = startLabel?.targetInstructionIndex ?: 0
        val finishIndex = finishLabel?.targetInstructionIndex ?: originalPseudocode.mutableInstructionList.size

        val originalToCopy = linkedMapOf<Label, PseudocodeLabel>()
        val originalLabelsForInstruction = HashMultimap.create<Instruction, Label>()
        for (label in originalPseudocode.labels) {
            val index = label.targetInstructionIndex
            //label is not bounded yet
            if (index < 0) continue

            if (label === startLabel || label === finishLabel) continue

            if (index in startIndex..finishIndex) {
                originalToCopy.put(label, label.copy(this, labelCount++))
                originalLabelsForInstruction.put(getJumpTarget(label), label)
            }
        }
        for (label in originalToCopy.values) {
            labels.add(label)
        }
        for (index in startIndex until finishIndex) {
            val originalInstruction = originalPseudocode.mutableInstructionList[index]
            repeatLabelsBindingForInstruction(originalInstruction, originalToCopy, originalLabelsForInstruction)
            val copy = copyInstruction(originalInstruction, originalToCopy)
            addInstruction(copy)
            if (originalInstruction === originalPseudocode.internalErrorInstruction && copy is SubroutineExitInstruction) {
                internalErrorInstruction = copy
            }
            if (originalInstruction === originalPseudocode.internalExitInstruction && copy is SubroutineExitInstruction) {
                internalExitInstruction = copy
            }
            if (originalInstruction === originalPseudocode.internalSinkInstruction && copy is SubroutineSinkInstruction) {
                internalSinkInstruction = copy
            }
        }
        if (finishIndex < originalPseudocode.mutableInstructionList.size) {
            repeatLabelsBindingForInstruction(
                originalPseudocode.mutableInstructionList[finishIndex],
                originalToCopy,
                originalLabelsForInstruction
            )
        }
        return labelCount
    }
    private fun repeatLabelsBindingForInstruction(
        originalInstruction: Instruction,
        originalToCopy: Map<Label, PseudocodeLabel>,
        originalLabelsForInstruction: Multimap<Instruction, Label>
    ) {
        for (originalLabel in originalLabelsForInstruction.get(originalInstruction)) {
            bindLabel(originalToCopy[originalLabel]!!)
        }
    }
    private fun copyInstruction(instruction: Instruction, originalToCopy: Map<Label, PseudocodeLabel>): Instruction {
        if (instruction is AbstractJumpInstruction) {
            val originalTarget = instruction.targetLabel
            val item = originalToCopy[originalTarget]
            if (item != null) {
                return instruction.copy(item)
            }
        }
        if (instruction is NondeterministicJumpInstruction) {
            val originalTargets = instruction.targetLabels
            val copyTargets = copyLabels(originalTargets, originalToCopy)
            return instruction.copy(copyTargets)
        }
        return (instruction as InstructionImpl).copy()
    }
    private fun copyLabels(labels: Collection<Label>, originalToCopy: Map<Label, PseudocodeLabel>): MutableList<Label> {
        val newLabels = arrayListOf<Label>()
        for (label in labels) {
            val newLabel = originalToCopy[label]
            newLabels.add(newLabel ?: label)
        }
        return newLabels
    }

    override fun copy(): PseudocodeImpl {
        val result = PseudocodeImpl(correspondingElement, isInlined)
        result.repeatWhole(this)
        return result
    }
    private fun getJumpTarget(targetLabel: Label): Instruction = targetLabel.resolveToInstruction()

    override fun instructionForElement(element: CjElement): CjElementInstruction?  = representativeInstructions[element]
}
