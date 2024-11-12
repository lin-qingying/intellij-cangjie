package com.linqingying.cangjie.resolve.controlFlow.pseudocode

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.util.containers.BidirectionalMap
import com.linqingying.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import com.linqingying.cangjie.cfg.pseudocodeTraverser.TraverseInstructionResult
import com.linqingying.cangjie.cfg.pseudocodeTraverser.traverseFollowingInstructions
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.*
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicInstruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicKind
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MergeInstruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.AbstractJumpInstruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.ConditionalJumpInstruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.NondeterministicJumpInstruction
import com.linqingying.cangjie.resolve.controlFlow.pseudocode.instructions.special.*


/**
 * Pseudocode接口代表一个伪代码序列，用于抽象和操作代码结构，而不涉及具体的编程语言细节。
 * 它提供了一种方式来访问和操作代码中的元素，如局部变量声明、指令、以及入口和出口点等。
 */
interface Pseudocode {
    /**
     * 获取与此伪代码序列相对应的代码元素。
     */
    val correspondingElement: CjElement

    /**
     * 获取此伪代码的父伪代码，如果存在。这可以用于表示嵌套的代码结构。
     */
    val parent: Pseudocode?

    /**
     * 获取在此伪代码序列中声明的所有局部函数的集合。
     */
    val localDeclarations: Set<LocalFunctionDeclarationInstruction>

    /**
     * 获取此伪代码中的所有指令列表。
     */
    val instructions: List<Instruction>

    /**
     * 获取逆序排列的所有指令列表，用于某些特定的分析或处理场景。
     */
    val reversedInstructions: List<Instruction>

    /**
     * 获取包括死亡代码在内的所有指令列表。死亡代码是指那些永远不会被执行的代码。
     */
    val instructionsIncludingDeadCode: List<Instruction>

    /**
     * 获取此伪代码的出口指令，代表代码执行的结束点。
     */
    val exitInstruction: SubroutineExitInstruction

    /**
     * 获取此伪代码的错误指令，代表代码执行中遇到错误时的行为。
     */
    val errorInstruction: SubroutineExitInstruction

    /**
     * 获取此伪代码的汇入指令，代表代码执行中的汇入点。
     */
    val sinkInstruction: SubroutineSinkInstruction

    /**
     * 获取此伪代码的入口指令，代表代码执行的起点。
     */
    val enterInstruction: SubroutineEnterInstruction

    /**
     * 表示此伪代码是否被内联。内联伪代码通常代表那些在调用点直接展开的函数或方法。
     */
    val isInlined: Boolean

    /**
     * 表示此伪代码是否包含do-while循环结构。
     */
    val containsDoWhile: Boolean

    /**
     * 获取此伪代码序列的根伪代码。根伪代码是伪代码层次结构的顶层元素。
     */
    val rootPseudocode: Pseudocode

    /**
     * 根据指定的代码元素获取其对应的值。
     *
     * @param element 可选的代码元素。
     * @return 对应的伪值，如果有的话。
     */
    fun getElementValue(element: CjElement?): PseudoValue?

    /**
     * 获取指定伪值对应的所有代码元素。
     *
     * @param value 可选的伪值。
     * @return 对应的代码元素列表。
     */
    fun getValueElements(value: PseudoValue?): List<CjElement>

    /**
     * 获取指定伪值的所有用法，即所有使用该值的指令。
     *
     * @param value 可选的伪值。
     * @return 使用该值的指令列表。
     */
    fun getUsages(value: PseudoValue?): List<Instruction>

    /**
     * 判断指定的指令是否没有副作用。没有副作用的指令是指那些只读取数据而不修改数据的指令。
     *
     * @param instruction 要判断的指令。
     * @return 如果指令没有副作用则返回true，否则返回false。
     */
    fun isSideEffectFree(instruction: Instruction): Boolean

    /**
     * 创建此伪代码序列的副本。
     *
     * @return 此伪代码的新副本。
     */
    fun copy(): Pseudocode

    /**
     * 获取指定代码元素对应的指令。
     *
     * @param element 可选的代码元素。
     * @return 对应的指令，如果有的话。
     */
    fun instructionForElement(element: CjElement): CjElementInstruction?
}


class PseudocodeImpl(override val correspondingElement: CjElement, override val isInlined: Boolean) : Pseudocode {
    // 可变的指令列表，用于内部存储和管理指令
    internal val mutableInstructionList = ArrayList<Instruction>()

    // 只读的指令列表，对外提供安全的访问方式
    override val instructions = ArrayList<Instruction>()

    // 标签列表，用于管理伪代码标签
    private val labels = java.util.ArrayList<PseudocodeLabel>()

    /**
     * 创建一个新的标签
     *
     * @param name 标签的名称
     * @param comment 标签的注释，可以为null
     * @return 创建的PseudocodeLabel实例
     */
    fun createLabel(name: String, comment: String?): PseudocodeLabel {
        val label = PseudocodeLabel(this, name, comment)
        labels.add(label)
        return label
    }

    /**
     * 在指定的标签范围内重复代码。
     *
     * 此函数负责在内部处理代码的重复逻辑，通过调用[repeatInternal]函数实现。
     *
     * @param startLabel 起始标签，表示重复区域的开始。
     * @param finishLabel 结束标签，表示重复区域的结束。
     * @param labelCount 标签数量，用于控制重复的次数。
     * @return 返回重复操作的结果。
     */
    fun repeatPart(startLabel: Label, finishLabel: Label, labelCount: Int): Int =
        repeatInternal(startLabel.pseudocode as PseudocodeImpl, startLabel, finishLabel, labelCount)

    /**
     * 获取逆序的指令列表。
     *
     * 此属性通过遍历指令来构建一个逆序的指令列表。首先尝试从当前结构的指令中逆向遍历，
     * 如果遍历到的指令数量少于总指令数量，则从简单逆序的指令列表中开始补充遍历，以确保
     * 所有指令都被正确逆序处理。
     *
     * @return 返回逆序后的指令列表。
     */
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

    /**
     * 包含死代码的指令列表
     * 这个属性重写了接口的方法，提供了一个只读的指令列表，包括那些可能是死代码的指令
     * 主要用于分析和调试目的，帮助开发者理解程序的行为，包括那些可能永远不会被执行的代码
     */
    override val instructionsIncludingDeadCode: List<Instruction>
        get() = mutableInstructionList

    /**
     * 父伪代码对象
     * 这个属性表示当前伪代码对象的父对象，初始值为null，并且只能在类内部进行设置
     * 设定为private set确保了父对象的设置权被限制在类内部，维护了数据的封装性
     */
    override var parent: Pseudocode? = null
        private set

    /**
     * 本地声明的函数集合
     * 通过委托属性lazy初始化，这个集合包含了所有在当前上下文中声明的本地函数
     * 使用lazy初始化是为了提高性能，只有在第一次访问时才会计算和初始化这个集合
     */
    override val localDeclarations: Set<LocalFunctionDeclarationInstruction> by lazy {
        getLocalDeclarations(this)
    }

    // 表示是否包含do-while循环的标志，仅内部可设置
    override var containsDoWhile: Boolean = false
        internal set

    // 标记是否已进行后期处理，私有属性
    private var postPrecessed = false

    // 记录内部错误指令，可能为null，私有属性
    private var internalErrorInstruction: SubroutineExitInstruction? = null

    // 存储伪值到指令的映射，一个伪值可以被多个指令使用
    private val valueUsages = hashMapOf<PseudoValue, MutableList<Instruction>>()

    // 存储合并的伪值，表示一个伪值可以由一组伪值合并而来
    private val mergedValues = hashMapOf<PseudoValue, Set<PseudoValue>>()

    // 存储没有副作用的指令集合
    private val sideEffectFree = hashSetOf<Instruction>()

    // 建立CjElement和PseudoValue之间的双向映射
    private val elementsToValues = BidirectionalMap<CjElement, PseudoValue>()

    /**
     * 获取错误指令
     * 在子程序中用于表示发生错误时的出口指令
     * 如果在初始化之前读取该属性，将抛出AssertionError异常
     * 这确保了错误指令在使用前已被正确设置
     */
    override val errorInstruction: SubroutineExitInstruction
        get() = internalErrorInstruction ?: throw AssertionError("Error instruction is read before initialization")

    /**
     * 内部使用的退出指令
     * 用于在子程序正常结束时退出
     * 如果在初始化之前读取该属性，将抛出AssertionError异常
     * 这确保了退出指令在使用前已被正确设置
     */
    private var internalExitInstruction: SubroutineExitInstruction? = null

    /**
     * 获取退出指令
     * 在子程序正常结束时使用，表示正常的出口指令
     * 如果在初始化之前读取该属性，将抛出AssertionError异常
     * 这确保了退出指令在使用前已被正确设置
     */
    override val exitInstruction: SubroutineExitInstruction
        get() = internalExitInstruction ?: throw AssertionError("Exit instruction is read before initialization")

    // 定义一个可变的SubroutineSinkInstruction类型属性，初始值为null
    private var internalSinkInstruction: SubroutineSinkInstruction? = null

    /**
     * 获取sinkInstruction属性的值
     * 如果internalSinkInstruction为null，则抛出AssertionError异常，表明在初始化之前就读取了sink instruction
     * 这个属性的目的是提供对internalSinkInstruction的受控访问，确保其在初始化后被安全使用
     */
    override val sinkInstruction: SubroutineSinkInstruction
        get() = internalSinkInstruction ?: throw AssertionError("Sink instruction is read before initialization")

    /**
     * 获取enterInstruction属性的值
     * 通过索引访问mutableInstructionList的第一个元素，并将其强制转换为SubroutineEnterInstruction类型
     * 这个属性的目的是提供对子例程入口指令的便捷访问
     */
    override val enterInstruction: SubroutineEnterInstruction
        get() = mutableInstructionList[0] as SubroutineEnterInstruction

    // 存储可达指令的集合，用于跟踪程序中所有可到达的指令
    val reachableInstructions = hashSetOf<Instruction>()

    // 映射代表指令的哈希表，用于快速查找特定元素对应的指令
    private val representativeInstructions = HashMap<CjElement, CjElementInstruction>()

    // 获取根伪代码，用于确定当前代码元素的顶层父元素
    override val rootPseudocode: Pseudocode
        get() {
            var parent = parent
            // 循环遍历父元素，直到找到根元素
            while (parent != null) {
                // 如果当前元素的父元素为空，则当前元素为根元素
                if (parent.parent == null) return parent
                parent = parent.parent
            }
            // 如果遍历结束仍未找到根元素，则当前元素自身即为根元素
            return this
        }

    /**
     * 递归获取伪代码中的所有本地函数声明指令
     *
     * 此函数遍历给定伪代码的指令列表，寻找并收集所有的本地函数声明指令
     * 当遇到本地函数声明时，不仅收集该声明，还会递归地收集其函数体内可能包含的本地函数声明
     *
     * @param pseudocode 代表一系列计算步骤的伪代码对象
     * @return 包含所有找到的本地函数声明指令的集合
     */
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

    /**
     * 将编程元素绑定到特定的伪代码值
     *
     * 此函数用于在编程元素和伪代码值之间建立映射关系，便于后续处理和查询
     *
     * @param element 要绑定值的编程元素
     * @param value 要绑定到元素的伪代码值
     */
    fun bindElementToValue(element: CjElement, value: PseudoValue) {
        elementsToValues.put(element, value)
    }

    /**
     * 根据给定的元素获取其对应的伪值
     *
     * @param element 可能包含伪值的CjElement对象，可以为空
     * @return 对应的PseudoValue对象，如果没有找到则返回空
     */
    override fun getElementValue(element: CjElement?): PseudoValue? = elementsToValues[element]

    /**
     * 根据给定的伪值获取所有关联的元素
     *
     * @param value 要查询的PseudoValue对象，可以为空
     * @return 包含所有关联的CjElement对象的列表，如果没有找到则返回空列表
     */
    override fun getValueElements(value: PseudoValue?): List<CjElement> =
        elementsToValues.getKeysByValue(value) ?: emptyList()

    /**
     * 根据给定的伪值获取其所有用法
     *
     * @param value 要查询的PseudoValue对象，可以为空
     * @return 包含所有相关用法的Instruction对象列表，如果没有找到则返回空列表
     */
    override fun getUsages(value: PseudoValue?): List<Instruction> = valueUsages[value] ?: mutableListOf()

    /**
     * 判断给定的指令是否没有副作用
     *
     * @param instruction 要判断的Instruction对象
     * @return 如果指令没有副作用则返回true，否则返回false
     */
    override fun isSideEffectFree(instruction: Instruction): Boolean = sideEffectFree.contains(instruction)

    /**
     * 获取合并值根据给定的伪值
     *
     * @param value 要查询的PseudoValue对象
     * @return 包含所有合并值的集合，如果没有找到则返回空集合
     */
    private fun getMergedValues(value: PseudoValue) = mergedValues[value] ?: emptySet()

    /**
     * 为合并指令添加合并值
     *
     * @param instruction 要处理的MergeInstruction对象
     */
    private fun addMergedValues(instruction: MergeInstruction) {
        val result = LinkedHashSet<PseudoValue>()
        for (value in instruction.inputValues) {
            result.addAll(getMergedValues(value))
            result.add(value)
        }
        mergedValues.put(instruction.outputValue, result)
    }

    /**
     * 向当前上下文中添加一条指令，并更新相关数据结构
     *
     * @param instruction 要添加的指令对象，代表一个新的操作或计算步骤
     */
    fun addInstruction(instruction: Instruction) {
        // 将指令添加到可变的指令列表中
        mutableInstructionList.add(instruction)
        // 设置指令的所有者为当前上下文对象
        instruction.owner = this

        // 如果指令是控制流图元素指令，则处理其代表的元素
        if (instruction is CjElementInstruction) {
            val element = instruction.element
            // 如果当前元素尚未有代表指令，则将其添加到代表指令映射中
            if (!representativeInstructions.containsKey(element)) {
                representativeInstructions[element] = instruction
            }
        }

        // 如果指令是合并指令，则添加其合并的值
        if (instruction is MergeInstruction) {
            addMergedValues(instruction)
        }

        // 遍历指令的输入值，添加值的使用记录
        for (inputValue in instruction.inputValues) {
            addValueUsage(inputValue, instruction)
            // 如果输入值有合并的值，也添加这些合并值的使用记录
            for (mergedValue in getMergedValues(inputValue)) {
                addValueUsage(mergedValue, instruction)
            }
        }

        // 如果指令计算无副作用，则将其添加到无副作用指令集合中
        if (instruction.calcSideEffectFree()) {
            sideEffectFree.add(instruction)
        }
    }

    /**
     * 向指定的伪值对象添加使用情况记录
     * 此函数用于跟踪伪值对象在代码中的使用情况，以便进行更有效的数据流分析和代码优化
     * 它通过记录每个伪值对象被使用的指令来实现这一点
     *
     * @param value 伪值对象，表示一个在代码中产生的临时或虚拟值
     * @param usage 使用伪值对象的指令，表示伪值的具体使用情况
     */
    private fun addValueUsage(value: PseudoValue, usage: Instruction) {
        // 如果使用情况是合并指令，则不记录，因为合并指令的处理方式特殊
        if (usage is MergeInstruction) return

        // 获取伪值对象的使用情况列表，如果尚未记录使用情况，则初始化为空数组
        // 然后将当前使用情况添加到列表中
        valueUsages.getOrPut(value) {
            arrayListOf()
        }.add(usage)
    }

    fun addSinkInstruction(sinkInstruction: SubroutineSinkInstruction) {
        addInstruction(sinkInstruction)
        assert(internalSinkInstruction == null) {
            "Repeated initialization of sink instruction: $internalSinkInstruction --> $sinkInstruction"
        }
        internalSinkInstruction = sinkInstruction
    }

    /**
     * 添加错误指令
     *
     * 此方法用于向指令集添加一个错误处理指令，并确保错误指令的唯一性
     * 它防止重复初始化错误指令，通过断言来确保当前实例尚未设置错误指令
     *
     * @param errorInstruction 要添加的错误处理指令，类型为SubroutineExitInstruction
     */
    fun addErrorInstruction(errorInstruction: SubroutineExitInstruction) {
        addInstruction(errorInstruction)
        // 确保错误指令的唯一性，如果已经存在，则抛出异常
        assert(internalErrorInstruction == null) {
            "Repeated initialization of error instruction: $internalErrorInstruction --> $errorInstruction"
        }
        internalErrorInstruction = errorInstruction
    }

    /**
     * 添加退出指令
     *
     * 此函数用于向子程序添加退出指令，并确保子程序的退出指令只被初始化一次
     * 它首先调用addInstruction函数将退出指令添加到指令列表中，然后通过断言确保当前子程序的退出指令尚未被设置
     * 如果断言失败，说明尝试重复初始化退出指令，将抛出异常
     * 最后，将internalExitInstruction属性设置为传入的退出指令，完成退出指令的初始化
     *
     * @param exitInstruction 要添加的子程序退出指令
     */
    fun addExitInstruction(exitInstruction: SubroutineExitInstruction) {
        addInstruction(exitInstruction)
        // 确保退出指令未被重复初始化
        assert(internalExitInstruction == null) {
            "Repeated initialization of exit instruction: $internalExitInstruction --> $exitInstruction"
        }
        internalExitInstruction = exitInstruction
    }

    /**
     * 执行指令列表的后处理优化。
     *
     * 该函数通过检查是否已经执行过后处理来避免冗余处理。
     * 它更新错误和退出指令，使其指向sink指令，以简化控制流图。
     * 然后，它处理可变指令列表中的每条指令，包括递归处理局部声明。
     * 最后，它收集并缓存所有可达指令，以便后续使用。
     */
    fun postProcess() {
        // 如果后处理已经执行过，则提前退出，防止冗余处理
        if (postPrecessed) return
        postPrecessed = true

        // 将错误和退出指令指向sink指令，以简化控制流结构
        errorInstruction.sink = sinkInstruction
        exitInstruction.sink = sinkInstruction

        // 遍历指令列表，处理每条指令及其索引
        for ((index, instruction) in mutableInstructionList.withIndex()) {
            // 递归调用局部声明的 `postProcess`，因此需要全局可达指令集
            instruction.processInstruction(index)
        }

        // 收集并缓存所有可达指令，以优化后续处理
        collectAndCacheReachableInstructions()
    }


    /**
     * 收集从当前伪代码可达的所有指令。
     *
     * 该函数旨在从当前伪代码开始遍历所有指令，以识别并记录所有可达指令。
     * 对某些特定指令进行特殊处理，以确保遍历的准确性和完整性。
     */
    private fun collectReachableInstructions() {
        // 创建一个哈希集合，用于存储从当前伪代码可达的指令
        val reachableFromThisPseudocode = hashSetOf<Instruction>()

        // 从入口指令开始，按正序遍历后续指令
        traverseFollowingInstructions(
            enterInstruction, reachableFromThisPseudocode, TraversalOrder.FORWARD
        ) { instruction ->
            // 如果当前指令是特殊的穷尽匹配 else 指令，则跳过它
            if (instruction is MagicInstruction && instruction.kind === MagicKind.EXHAUSTIVE_MATCH_ELSE) {
                return@traverseFollowingInstructions TraverseInstructionResult.SKIP
            }
            TraverseInstructionResult.CONTINUE
        }

        // 不要强制为内联伪代码添加退出和错误指令，因为对于这些声明，这些指令具有特殊语义
        // 如果当前伪代码不是内联的，则将退出、错误和汇流指令添加到可达指令集合中
        if (!isInlined) {
            reachableFromThisPseudocode.add(exitInstruction)
            reachableFromThisPseudocode.add(errorInstruction)
            reachableFromThisPseudocode.add(sinkInstruction)
        }

        // 遍历可达指令集合，并将这些指令添加到其所属伪代码的可达指令列表中
        reachableFromThisPseudocode.forEach { (it.owner as PseudocodeImpl).reachableInstructions.add(it) }
    }


    /**
     * 收集并缓存可到达的指令
     *
     * 此方法旨在分析并记录所有可到达的指令，以便后续处理使用具体步骤包括：
     * 1. 收集可到达的指令，这一步的结果将更新reachableInstructions集合
     * 2. 遍历mutableInstructionList中的所有指令，如果指令存在于reachableInstructions中，则将其添加到instructions列表中
     * 3. 标记不可到达（即“死”）指令，这些指令不会被进一步处理或执行
     */
    private fun collectAndCacheReachableInstructions() {
        // 第一步：收集所有可到达的指令
        collectReachableInstructions()

        // 第二步：遍历所有指令，并将可到达的指令添加到instructions列表中
        for (instruction in mutableInstructionList) {
            if (reachableInstructions.contains(instruction)) {
                instructions.add(instruction)
            }
        }

        // 第三步：标记不可到达的指令
        markDeadInstructions()
    }

    /**
     * 标记无用指令
     *
     * 此方法用于遍历可变指令列表，标记那些在指令集合中不存在的指令为“死指令”（即不会被执行的指令）
     * 它首先将当前指令集合转换为HashSet以提高查找效率，然后遍历每个指令如果指令不在集合中，
     * 则将其标记为死指令，并且从其后续指令的前驱指令列表中移除该指令
     */
    private fun markDeadInstructions() {
        // 创建一个HashSet，用于快速查找指令是否存在于当前指令集合中
        val instructionSet = instructions.toHashSet()
        // 遍历可变指令列表，检查每个指令是否在上述HashSet中
        for (instruction in mutableInstructionList) {
            // 如果指令不在集合中，说明它是无用的，需要被标记和清理
            if (!instructionSet.contains(instruction)) {
                // 将当前指令标记为死指令
                (instruction as? InstructionImpl)?.markedAsDead = true
                // 从当前指令的后续指令中移除对当前指令的引用
                for (nextInstruction in instruction.nextInstructions) {
                    (nextInstruction as? InstructionImpl)?.previousInstructions?.remove(instruction)
                }
            }
        }
    }

    /**
     * 处理不同类型的指令，根据其特性进行处理
     * 处理的目的包括解析跳转目标、处理条件跳转、处理局部函数声明等
     *
     * @param currentPosition 当前指令的位置，用于确定下一个指令或跳转目标
     */
    private fun Instruction.processInstruction(currentPosition: Int) {
        accept(object : InstructionVisitor() {
            /**
             * 处理带有下一个指令的指令
             * 设置给定指令的下一个指令
             *
             * @param instruction 要处理的指令
             */
            override fun visitInstructionWithNext(instruction: InstructionWithNext) {
                instruction.next = getNextPosition(currentPosition)
            }

            /**
             * 处理跳转指令
             * 解析并设置给定跳转指令的跳转目标
             *
             * @param instruction 要处理的跳转指令
             */
            override fun visitJump(instruction: AbstractJumpInstruction) {
                instruction.resolvedTarget = getJumpTarget(instruction.targetLabel)
            }

            /**
             * 处理非确定性跳转指令
             * 除了设置下一个指令外，还解析多个跳转目标
             *
             * @param instruction 要处理的非确定性跳转指令
             */
            override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                instruction.next = getNextPosition(currentPosition)
                val targetLabels = instruction.targetLabels
                for (targetLabel in targetLabels) {
                    instruction.setResolvedTarget(targetLabel, getJumpTarget(targetLabel))
                }
            }

            /**
             * 处理条件跳转指令
             * 根据条件设置下一个指令和跳转目标
             * 还调用 visitJump 处理跳转指令的公共部分
             *
             * @param instruction 要处理的条件跳转指令
             */
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

            /**
             * 处理局部函数声明指令
             * 处理函数体并设置下一个指令
             *
             * @param instruction 要处理的局部函数声明指令
             */
            override fun visitLocalFunctionDeclarationInstruction(instruction: LocalFunctionDeclarationInstruction) {
                val body = instruction.body as PseudocodeImpl
                body.parent = this@PseudocodeImpl
                body.postProcess()
                instruction.next = sinkInstruction
            }

            /**
             * 处理内联局部函数声明指令
             * 处理函数体并根据是否能到达出口来设置下一个指令
             *
             * @param instruction 要处理的内联局部函数声明指令
             */
            override fun visitInlinedLocalFunctionDeclarationInstruction(instruction: InlinedLocalFunctionDeclarationInstruction) {
                val body = instruction.body as PseudocodeImpl
                body.parent = this@PseudocodeImpl
                body.postProcess()
                // 如果流不能到达内联声明的出口，则不添加到下一个指令的边
                instruction.next =
                    if (body.instructions.contains(body.exitInstruction)) getNextPosition(currentPosition) else sinkInstruction
            }

            /**
             * 处理子例程退出指令
             * 不做任何处理
             */
            override fun visitSubroutineExit(instruction: SubroutineExitInstruction) {
                // Nothing
            }

            /**
             * 处理子例程汇合指令
             * 不做任何处理
             */
            override fun visitSubroutineSink(instruction: SubroutineSinkInstruction) {
                // Nothing
            }

            /**
             * 处理未定义的指令
             * 抛出不支持的操作异常
             *
             * @param instruction 要处理的指令
             */
            override fun visitInstruction(instruction: Instruction) {
                throw UnsupportedOperationException(instruction.toString())
            }
        })
    }


    /**
     * 绑定标签到当前伪代码对象
     *
     * 此函数确保标签属于当前伪代码对象，以防止跨伪代码对象的错误绑定
     * 它通过检查标签所属的伪代码对象是否与当前对象相同来实现这一点
     * 如果标签尝试绑定到不同的伪代码对象，将抛出断言错误
     *
     * @param label 要绑定到当前伪代码对象的标签
     * @throws AssertionError 如果标签不属于当前伪代码对象
     */
    fun bindLabel(label: PseudocodeLabel) {
        // 确保标签属于当前伪代码对象，以防止错误绑定
        assert(this == label.pseudocode) {
            "Attempt to bind label $label to instruction from different pseudocode: " +
                    "\nowner pseudocode = ${label.pseudocode.mutableInstructionList}, " +
                    "\nbound pseudocode = ${this.mutableInstructionList}"
        }

        // 设置标签的目标指令索引为当前指令列表的大小
        label.targetInstructionIndex = mutableInstructionList.size
    }

    /**
     * 重复执行整个伪代码
     *
     * 此函数的目的是将当前伪代码实例标记为需要完全重复执行它模拟原始伪代码的执行过程
     * 通过调用[repeatInternal]函数，但不指定任何特定的重复参数，实际上是从头到尾重复执行一次
     *
     * @param originalPseudocode 需要重复执行的原始伪代码实例
     */
    private fun repeatWhole(originalPseudocode: PseudocodeImpl) {
        // 调用内部重复函数，参数为null表示不指定重复的起始和结束位置，0表示重复次数为1
        repeatInternal(originalPseudocode, null, null, 0)
        // 更新当前实例的父伪代码引用为原始伪代码的父伪代码，以保持伪代码结构的一致性
        parent = originalPseudocode.parent
    }

    /**
     * 获取下一个指令的位置
     * 该函数旨在根据当前位置找到并返回下一个指令，确保不会超出指令列表的范围
     *
     * @param currentPosition 当前指令的位置，用于计算下一个指令的位置
     * @return 返回下一个指令，如果不存在则抛出异常
     *
     * 注意：调用此函数前应确保当前指令列表不为空，且当前位置有效
     */
    private fun getNextPosition(currentPosition: Int): Instruction {
        // 计算下一个指令的位置
        val targetPosition = currentPosition + 1
        // 断言确保目标位置在指令列表范围内，以避免IndexOutOfBoundsException
        assert(targetPosition < mutableInstructionList.size) { currentPosition }
        // 返回计算出的目标位置的指令
        return mutableInstructionList[targetPosition]
    }

    /**
     * 重复子程序的内部伪代码。
     *
     * 此函数负责复制子程序中的部分伪代码，包括复制标签和指令。
     * 它还处理标签目标的重新分配以及跳转指令的更新，以确保复制后的伪代码的完整性。
     *
     * @param originalPseudocode 原始伪代码对象，提供复制的基础。
     * @param startLabel 复制范围的起始标签，如果从头开始则为 null。
     * @param finishLabel 复制范围的结束标签，如果到最后一条指令则为 null。
     * @param labelCountArg 标签的初始计数，用于在复制过程中生成新标签。
     * @return 复制后的更新标签计数。
     */
    private fun repeatInternal(
        originalPseudocode: PseudocodeImpl,
        startLabel: Label?, finishLabel: Label?,
        labelCountArg: Int
    ): Int {
        var labelCount = labelCountArg
        // 确定复制范围的起始和结束索引
        val startIndex = startLabel?.targetInstructionIndex ?: 0
        val finishIndex = finishLabel?.targetInstructionIndex ?: originalPseudocode.mutableInstructionList.size

        // 原始标签到其复制副本的映射
        val originalToCopy = linkedMapOf<Label, PseudocodeLabel>()
        // 指令到其对应标签的映射
        val originalLabelsForInstruction = HashMultimap.create<Instruction, Label>()

        // 遍历原始伪代码中的所有标签
        for (label in originalPseudocode.labels) {
            val index = label.targetInstructionIndex
            // 跳过未绑定的标签或恰好是起始或结束标签的标签
            if (index < 0 || label === startLabel || label === finishLabel) continue

            // 如果标签在复制范围内，则复制它
            if (index in startIndex..finishIndex) {
                originalToCopy.put(label, label.copy(this, labelCount++))
                originalLabelsForInstruction.put(getJumpTarget(label), label)
            }
        }

        // 将所有复制的标签添加到当前伪代码中
        for (label in originalToCopy.values) {
            labels.add(label)
        }

        // 遍历复制范围内的所有指令并复制它们
        for (index in startIndex until finishIndex) {
            val originalInstruction = originalPseudocode.mutableInstructionList[index]
            repeatLabelsBindingForInstruction(originalInstruction, originalToCopy, originalLabelsForInstruction)
            val copy = copyInstruction(originalInstruction, originalToCopy)
            addInstruction(copy)

            // 必要时更新内部错误、退出和汇流指令
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

        // 如果复制范围的结束不是最后一条指令，则更新最后一条指令的绑定
        if (finishIndex < originalPseudocode.mutableInstructionList.size) {
            repeatLabelsBindingForInstruction(
                originalPseudocode.mutableInstructionList[finishIndex],
                originalToCopy,
                originalLabelsForInstruction
            )
        }
        return labelCount
    }


    /**
     * 为指令重复绑定标签
     *
     * 此函数的目的是将与原始指令相关的所有标签重新绑定到伪代码指令上
     * 它通过使用原始标签到伪代码标签的映射来实现这一点
     *
     * @param originalInstruction 原始指令实例
     * @param originalToCopy 原始标签到伪代码标签的映射
     * @param originalLabelsForInstruction 指令到标签的多映射关系
     */
    private fun repeatLabelsBindingForInstruction(
        originalInstruction: Instruction,
        originalToCopy: Map<Label, PseudocodeLabel>,
        originalLabelsForInstruction: Multimap<Instruction, Label>
    ) {
        // 遍历与原始指令相关的所有标签
        for (originalLabel in originalLabelsForInstruction.get(originalInstruction)) {
            // 根据映射关系，绑定对应的伪代码标签
            bindLabel(originalToCopy[originalLabel]!!)
        }
    }

    /**
     * 复制指定的指令，并根据原始标签到伪代码标签的映射关系更新跳转目标
     *
     * @param instruction 待复制的指令对象
     * @param originalToCopy 包含原始标签与对应伪代码标签的映射关系的字典
     * @return 返回复制后的指令对象
     */
    private fun copyInstruction(instruction: Instruction, originalToCopy: Map<Label, PseudocodeLabel>): Instruction {
        // 如果指令是抽象跳转指令，则需要更新跳转目标标签
        if (instruction is AbstractJumpInstruction) {
            // 获取原始的跳转目标标签
            val originalTarget = instruction.targetLabel
            // 查找映射关系中的对应伪代码标签
            val item = originalToCopy[originalTarget]
            // 如果找到了对应的伪代码标签，则使用新标签复制指令
            if (item != null) {
                return instruction.copy(item)
            }
        }
        // 如果指令是非确定性跳转指令，则需要更新所有可能的跳转目标标签
        if (instruction is NondeterministicJumpInstruction) {
            // 获取原始的跳转目标标签列表
            val originalTargets = instruction.targetLabels
            // 复制并更新跳转目标标签列表
            val copyTargets = copyLabels(originalTargets, originalToCopy)
            // 使用更新后的标签列表复制指令
            return instruction.copy(copyTargets)
        }
        // 对于其他类型的指令，直接复制
        return (instruction as InstructionImpl).copy()
    }

    /**
     * 复制标签集合，并根据原始标签到复制标签的映射关系更新标签
     * 如果某个标签在映射中不存在，则保持该标签不变
     *
     * @param labels 要复制的原始标签集合
     * @param originalToCopy 原始标签到复制标签的映射关系
     * @return 复制后的标签集合，其中的标签根据映射关系进行了更新
     */
    private fun copyLabels(labels: Collection<Label>, originalToCopy: Map<Label, PseudocodeLabel>): MutableList<Label> {
        // 创建一个新的标签列表，用于存储复制后的标签
        val newLabels = arrayListOf<Label>()
        // 遍历原始标签集合
        for (label in labels) {
            // 根据原始标签获取复制标签，如果不存在，则保持原始标签不变
            val newLabel = originalToCopy[label]
            // 将获取到的复制标签或原始标签添加到新的标签列表中
            newLabels.add(newLabel ?: label)
        }
        // 返回新的标签列表
        return newLabels
    }

    /**
     * 创建当前伪代码实例的副本
     *
     * 通过委托给[PseudocodeImpl]的构造函数，并将当前实例的[correspondingElement]和[isInlined]属性值复制到新实例中，
     * 然后通过调用[repeatWhole]方法将当前实例的所有属性和状态复制到新实例中
     *
     * @return 返回一个新的[PseudocodeImpl]实例，该实例是当前伪代码实例的完整副本
     */
    override fun copy(): PseudocodeImpl {
        // 创建一个新的PseudocodeImpl实例，初始化时不改变isInlined的值
        val result = PseudocodeImpl(correspondingElement, isInlined)
        // 将当前实例的全部属性和状态复制到新创建的实例中
        result.repeatWhole(this)
        // 返回复制后的新实例
        return result
    }

    /**
     * 根据目标标签获取跳转目标指令
     *
     * 此函数用于解析给定的目标标签，以获取其对应的指令
     * 主要应用于字节码解析或虚拟机指令处理等场景
     *
     * @param targetLabel 目标标签，表示需要跳转到的位置
     * @return 解析后得到的指令，表示跳转的目标位置
     */
    private fun getJumpTarget(targetLabel: Label): Instruction = targetLabel.resolveToInstruction()

    /**
     * 根据CjElement对象获取对应的指令
     *
     * 此函数用于根据一个CjElement对象来获取一个CjElementInstruction对象
     * 它通过代表指令映射[representativeInstructions]来查找与给定元素关联的指令
     * 如果找不到对应的指令，则返回null
     *
     * @param element CjElement对象，作为获取指令的键
     * @return CjElementInstruction对象，如果找不到则返回null
     */
    override fun instructionForElement(element: CjElement): CjElementInstruction? = representativeInstructions[element]
}
