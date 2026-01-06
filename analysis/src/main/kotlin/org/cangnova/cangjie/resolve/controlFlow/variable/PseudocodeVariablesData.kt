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
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.PseudocodeUtil
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.MagicKind
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.ReadValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.WriteValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.VariableDeclarationInstruction

/**
 * 伪代码变量数据分析器
 *
 * 负责对伪代码（Pseudocode）中的变量进行数据流分析，包括：
 * 1. **变量初始化分析** - 检测变量的初始化状态，识别未初始化使用
 * 2. **变量使用分析** - 检测变量的读写情况，识别未使用变量
 *
 * ## 核心概念
 *
 * ### Trivial Initializer (平凡初始化)
 * 指在声明时就已经初始化的变量，这些变量的初始化状态可以静态确定，无需数据流分析：
 * - 函数参数（总是已初始化）
 * - 带初始化器的 `let` 变量：`let x = 1`
 * - 无后备字段的属性（计算属性）
 *
 * ### Non-trivial Variables (非平凡变量)
 * 需要通过数据流分析才能确定初始化状态的变量：
 * - `var` 变量（可能在声明后再初始化）
 * - 不带初始化器的 `let` 变量
 * - 在 `do-while` 循环中的变量（需要特殊处理）
 *
 * ## 算法流程
 *
 * ### 初始化分析流程
 * 1. 收集所有声明的变量，区分 trivial 和 non-trivial 变量
 * 2. 对 trivial 变量直接标记为已初始化（无需数据流分析）
 * 3. 对 non-trivial 变量执行前向数据流分析
 * 4. 生成每条指令的变量初始化状态映射
 *
 * ### 使用分析流程
 * 1. 对 trivial 变量：所有 `ReadValueInstruction` 处标记为已使用
 * 2. 对 non-trivial 变量执行后向数据流分析
 * 3. 生成每条指令的变量使用状态映射
 *
 * @property pseudocode 要分析的伪代码
 * @property bindingContext 绑定上下文，用于获取变量的描述符信息
 */
class PseudocodeVariablesData(val pseudocode: Pseudocode, private val bindingContext: BindingContext) {

    // ==================== 私有字段 ====================

    /** 标记伪代码中是否包含 do-while 循环 */
    private val containsDoWhile = pseudocode.rootPseudocode.containsDoWhile

    /** 变量描述符提取器 */
    private val descriptorExtractor = VariableDescriptorExtractor(bindingContext)

    /** 变量数据收集器 */
    private val dataCollector = PseudocodeVariableDataCollector(bindingContext, pseudocode)

    /** 声明变量的缓存 */
    private val declaredVariablesCache = hashMapOf<Pseudocode, VariablesClassification>()

    /** 根伪代码中的所有变量（延迟初始化） */
    private val rootVariables by lazy(LazyThreadSafetyMode.NONE) {
        collectAllDeclaredVariables(pseudocode, includeLocalDeclarations = true)
    }

    // ==================== 公开属性 ====================

    /** 块作用域变量信息 */
    val blockScopeVariableInfo: BlockScopeVariableInfo
        get() = dataCollector.blockScopeVariableInfo

    /** 变量初始化状态的数据流分析结果（延迟计算） */
    val variableInitializers: Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>> by lazy {
        VariableInitializationAnalyzer(this).analyze()
    }

    /** 变量使用状态的数据流分析结果（延迟计算） */
    val variableUseStatusData: Map<Instruction, Edges<VariableUsageReadOnlyControlInfo>>
        get() = VariableUsageAnalyzer(this).analyze()

    // ==================== 公开方法 ====================

    /**
     * 获取指定伪代码块中声明的所有变量
     *
     * @param pseudocode 要查询的伪代码块
     * @param includeInsideLocalDeclarations 是否包含本地函数内声明的变量
     * @return 声明的变量描述符集合
     */
    fun getDeclaredVariables(pseudocode: Pseudocode, includeInsideLocalDeclarations: Boolean): Set<VariableDescriptor> =
        collectAllDeclaredVariables(pseudocode, includeInsideLocalDeclarations).allVariables

    /**
     * 判断变量是否是平凡初始化的 let 变量
     *
     * @param variableDescriptor 要检查的变量描述符
     * @return true 如果该变量在声明时就已初始化，无需数据流分析
     */
    fun isVariableWithTrivialInitializer(variableDescriptor: VariableDescriptor) =
        variableDescriptor in rootVariables.trivialVariables

    // ==================== 内部访问方法 ====================

    internal fun getDataCollector() = dataCollector
    internal fun getDescriptorExtractor() = descriptorExtractor
    internal fun getRootVariables() = rootVariables
    internal fun getBindingContext() = bindingContext

    // ==================== 变量收集逻辑 ====================

    /**
     * 收集所有声明的变量
     *
     * @param pseudocode 要分析的伪代码块
     * @param includeLocalDeclarations 是否递归收集本地函数内的变量
     * @return 变量分类信息（trivial 和 non-trivial）
     */
    private fun collectAllDeclaredVariables(
        pseudocode: Pseudocode,
        includeLocalDeclarations: Boolean
    ): VariablesClassification {
        if (!includeLocalDeclarations) {
            return getOrComputeVariables(pseudocode)
        }

        val trivialVariables = linkedSetOf<VariableDescriptor>()
        val nonTrivialVariables = linkedSetOf<VariableDescriptor>()

        // 收集当前伪代码块的变量
        getOrComputeVariables(pseudocode).let {
            trivialVariables.addAll(it.trivialVariables)
            nonTrivialVariables.addAll(it.nonTrivialVariables)
        }

        // 递归收集本地函数内的变量
        for (localDeclaration in pseudocode.localDeclarations) {
            getOrComputeVariables(localDeclaration.body).let {
                trivialVariables.addAll(it.trivialVariables)
                nonTrivialVariables.addAll(it.nonTrivialVariables)
            }
        }

        return VariablesClassification(trivialVariables, nonTrivialVariables)
    }

    /**
     * 获取或计算指定伪代码块的变量分类
     */
    private fun getOrComputeVariables(pseudocode: Pseudocode): VariablesClassification =
        declaredVariablesCache.getOrPut(pseudocode) {
            computeVariablesForPseudocode(pseudocode)
        }

    /**
     * 计算指定伪代码块中的变量声明信息
     */
    private fun computeVariablesForPseudocode(pseudocode: Pseudocode): VariablesClassification {
        val trivialVariables = linkedSetOf<VariableDescriptor>()
        val nonTrivialVariables = linkedSetOf<VariableDescriptor>()

        for (instruction in pseudocode.instructions) {
            if (instruction is VariableDeclarationInstruction) {
                classifyVariables(instruction, trivialVariables, nonTrivialVariables)
            }
        }

        return VariablesClassification(trivialVariables, nonTrivialVariables)
    }

    /**
     * 对变量声明指令中的变量进行分类
     */
    private fun classifyVariables(
        instruction: VariableDeclarationInstruction,
        trivialVariables: MutableSet<VariableDescriptor>,
        nonTrivialVariables: MutableSet<VariableDescriptor>
    ) {
        val element = instruction.variableDeclarationElement
        val descriptors = descriptorExtractor.extractFromDeclarationElement(element)

        for (descriptor in descriptors) {
            if (!containsDoWhile && isTriviallyInitialized(element, descriptor)) {
                trivialVariables.add(descriptor)
            } else {
                nonTrivialVariables.add(descriptor)
            }
        }
    }

    /**
     * 判断变量声明是否是平凡初始化
     */
    private fun isTriviallyInitialized(element: CjDeclaration, descriptor: VariableDescriptor): Boolean {
        // 参数总是已初始化
        if (element is CjParameter) return true

        // 检查变量声明
        val variableDecl = element as? CjVariableDeclaration ?: return false

        // 无后备字段的属性（计算属性）
        if (descriptor.isPropertyWithoutBackingField()) return true

        // var 变量总是非平凡的
        if (variableDecl.isVar) return false

        // let 变量必须有初始化器才是平凡的
        return variableDecl.initializer != null
    }

    /**
     * 判断变量描述符是否是无后备字段的属性
     */
    private fun VariableDescriptor.isPropertyWithoutBackingField(): Boolean {
        if (this !is PropertyDescriptor) return false
        return bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) != true
    }

    // ==================== 变量分类容器 ====================

    /**
     * 变量分类结果
     *
     * @property trivialVariables 平凡初始化变量（无需数据流分析）
     * @property nonTrivialVariables 非平凡变量（需要数据流分析）
     */
    internal class VariablesClassification(
        val trivialVariables: Set<VariableDescriptor>,
        val nonTrivialVariables: Set<VariableDescriptor>
    ) {
        /** 所有变量的合集 */
        val allVariables: Set<VariableDescriptor> by lazy {
            if (nonTrivialVariables.isEmpty()) {
                trivialVariables
            } else {
                LinkedHashSet(trivialVariables).also { it.addAll(nonTrivialVariables) }
            }
        }
    }

    // ==================== 伴生对象 ====================

    companion object {
        /**
         * 获取变量的默认初始化状态
         *
         * @param variable 要查询的变量描述符
         * @param instruction 当前指令
         * @param blockScopeVariableInfo 块作用域信息
         * @return 变量的默认控制流状态
         */
        fun getDefaultValueForInitializers(
            variable: VariableDescriptor,
            instruction: Instruction,
            blockScopeVariableInfo: BlockScopeVariableInfo
        ): VariableControlFlowState {
            val declaredIn = blockScopeVariableInfo.declaredIn[variable]
            val declaredOutsideThisDeclaration =
                declaredIn == null ||
                        declaredIn.blockScopeForContainingDeclaration != instruction.blockScope.blockScopeForContainingDeclaration
            return VariableControlFlowState.create(isInitialized = declaredOutsideThisDeclaration)
        }
    }
}

// ==================== 初始化分析器 ====================

/**
 * 变量初始化分析器
 *
 * 负责计算变量的初始化状态数据流分析结果
 */
private class VariableInitializationAnalyzer(private val data: PseudocodeVariablesData) {

    private val pseudocode = data.pseudocode
    private val dataCollector = data.getDataCollector()
    private val descriptorExtractor = data.getDescriptorExtractor()
    private val rootVariables = data.getRootVariables()
    private val blockScopeVariableInfo = data.blockScopeVariableInfo
    private val bindingContext = data.getBindingContext()

    /**
     * 执行初始化分析
     */
    fun analyze(): Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>> {
        // 第一步：计算平凡变量的初始化状态
        val trivialResult = computeTrivialVariablesInitInfo()

        // 如果没有非平凡变量，直接返回平凡变量的结果
        if (rootVariables.nonTrivialVariables.isEmpty()) {
            return trivialResult
        }

        // 第二步：对非平凡变量执行前向数据流分析
        val nonTrivialResult = dataCollector.collectData(
            TraversalOrder.FORWARD,
            VariableInitControlFlowInfo()
        ) { instruction, incomingEdgesData ->
            processInitInstruction(instruction, incomingEdgesData)
        }

        // 第三步：合并结果
        return nonTrivialResult.mapValues { (instruction, edges) ->
            val trivialEdges = trivialResult[instruction]!!
            Edges(
                (trivialEdges.incoming as TrivialVariableInitInfo).replaceDelegate(edges.incoming),
                (trivialEdges.outgoing as TrivialVariableInitInfo).replaceDelegate(edges.outgoing)
            )
        }
    }

    /**
     * 计算平凡变量的初始化状态
     */
    private fun computeTrivialVariablesInitInfo(): Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>> {
        val result = hashMapOf<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>>()
        val builder = TrivialVariableInitInfo.Builder()

        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            val enterState = builder.snapshot()

            when (instruction) {
                is VariableDeclarationInstruction -> {
                    processTrivialDeclaration(instruction, builder)
                }
                is WriteValueInstruction -> {
                    processTrivialWrite(instruction, builder)
                }
            }

            val exitState = builder.snapshot()
            result[instruction] = Edges(enterState, exitState)
        }

        return result
    }

    /**
     * 处理平凡变量的声明
     */
    private fun processTrivialDeclaration(
        instruction: VariableDeclarationInstruction,
        builder: TrivialVariableInitInfo.Builder
    ) {
        val descriptors = descriptorExtractor.extractFromDeclaration(instruction)
        for (descriptor in descriptors) {
            if (descriptor in rootVariables.trivialVariables) {
                builder.addDeclared(descriptor)
            }
        }
    }

    /**
     * 处理平凡变量的写入
     */
    private fun processTrivialWrite(
        instruction: WriteValueInstruction,
        builder: TrivialVariableInitInfo.Builder
    ) {
        if (!VariableDescriptorExtractor.isTrivialInitializer(instruction)) return

        val element = instruction.element
        if (element is CjPatternVariable) {
            val descriptors = descriptorExtractor.extractFromPatternVariable(element)
            for (descriptor in descriptors) {
                if (descriptor in rootVariables.trivialVariables) {
                    builder.addInitialized(descriptor)
                }
            }
        } else {
            val descriptor = descriptorExtractor.extractFromAccessInstruction(instruction)
            if (descriptor != null && descriptor in rootVariables.trivialVariables) {
                builder.addInitialized(descriptor)
            }
        }
    }

    /**
     * 处理初始化指令
     */
    private fun processInitInstruction(
        instruction: Instruction,
        incomingEdgesData: Collection<VariableInitControlFlowInfo>
    ): Edges<VariableInitControlFlowInfo> {
        // 合并入边数据
        val enterData = mergeIncomingEdgesData(instruction, incomingEdgesData)

        // 更新初始化状态
        val exitData = updateInitState(instruction, enterData)

        return Edges(enterData, exitData)
    }

    /**
     * 合并多个入边的初始化状态
     */
    private fun mergeIncomingEdgesData(
        instruction: Instruction,
        incomingEdgesData: Collection<VariableInitControlFlowInfo>
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
                val varState = edgeData.getOrNull(variable)
                    ?: PseudocodeVariablesData.getDefaultValueForInitializers(
                        variable, instruction, blockScopeVariableInfo
                    )
                initState = initState?.merge(varState.initState) ?: varState.initState
                if (!varState.isDeclared) isDeclared = false
            }

            if (initState == null) {
                throw AssertionError("An empty set of incoming edges data")
            }
            result.put(variable, VariableControlFlowState.create(initState, isDeclared))
        }
    }

    /**
     * 根据当前指令更新初始化状态
     */
    private fun updateInitState(
        instruction: Instruction,
        enterData: VariableInitControlFlowInfo
    ): VariableInitControlFlowInfo {
        // 处理 match 表达式的穷尽 else 分支
        if (instruction is MagicInstruction && instruction.kind === MagicKind.EXHAUSTIVE_MATCH_ELSE) {
            return enterData.iterator().fold(enterData) { result, (key, value) ->
                if (!value.definitelyInitialized()) {
                    result.put(key, VariableControlFlowState.createInitializedExhaustively(value.isDeclared))
                } else result
            }
        }

        if (instruction !is WriteValueInstruction && instruction !is VariableDeclarationInstruction) {
            return enterData
        }

        val variable = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)
            ?.takeIf { it in rootVariables.nonTrivialVariables }
            ?: return enterData

        return when (instruction) {
            is WriteValueInstruction -> handleWriteInstruction(instruction, variable, enterData)
            is VariableDeclarationInstruction -> handleDeclarationInstruction(variable, enterData)
            else -> enterData
        }
    }

    /**
     * 处理写入指令
     */
    private fun handleWriteInstruction(
        instruction: WriteValueInstruction,
        variable: VariableDescriptor,
        enterData: VariableInitControlFlowInfo
    ): VariableInitControlFlowInfo {
        if (!PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, bindingContext)) {
            return enterData
        }

        val enterState = enterData.getOrNull(variable)
        val initState = VariableControlFlowState.create(instruction.element is CjProperty, enterState)
        return enterData.put(variable, initState, enterState)
    }

    /**
     * 处理声明指令
     */
    private fun handleDeclarationInstruction(
        variable: VariableDescriptor,
        enterData: VariableInitControlFlowInfo
    ): VariableInitControlFlowInfo {
        val enterState = enterData.getOrNull(variable)
            ?: PseudocodeVariablesData.getDefaultValueForInitializers(
                variable, pseudocode.instructions.first(), blockScopeVariableInfo
            )

        if (!enterState.mayBeInitialized() || !enterState.isDeclared) {
            val declaredState = VariableControlFlowState.create(enterState.initState, isDeclared = true)
            return enterData.put(variable, declaredState, enterState)
        }
        return enterData
    }

    companion object {
        private val EMPTY_INIT_CONTROL_FLOW_INFO = VariableInitControlFlowInfo()
    }
}

// ==================== 使用分析器 ====================

/**
 * 变量使用分析器
 *
 * 负责计算变量的使用状态数据流分析结果
 */
private class VariableUsageAnalyzer(private val data: PseudocodeVariablesData) {

    private val pseudocode = data.pseudocode
    private val dataCollector = data.getDataCollector()
    private val descriptorExtractor = data.getDescriptorExtractor()
    private val rootVariables = data.getRootVariables()
    private val bindingContext = data.getBindingContext()

    /**
     * 执行使用分析
     */
    fun analyze(): Map<Instruction, Edges<VariableUsageReadOnlyControlInfo>> {
        // 计算平凡变量的使用状态
        val trivialEdges = computeTrivialVariablesUsageInfo()

        // 如果没有非平凡变量，直接返回
        if (rootVariables.nonTrivialVariables.isEmpty()) {
            return hashMapOf<Instruction, Edges<VariableUsageReadOnlyControlInfo>>().apply {
                pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
                    put(instruction, trivialEdges)
                }
            }
        }

        // 对非平凡变量执行后向数据流分析
        return dataCollector.collectData(
            TraversalOrder.BACKWARD,
            UsageVariableControlFlowInfo()
        ) { instruction, incomingEdgesData ->
            processUsageInstruction(instruction, incomingEdgesData)
        }.mapValues { (_, edges) ->
            Edges(
                trivialEdges.incoming.replaceDelegate(edges.incoming),
                trivialEdges.outgoing.replaceDelegate(edges.outgoing)
            )
        }
    }

    /**
     * 计算平凡变量的使用状态
     */
    private fun computeTrivialVariablesUsageInfo(): Edges<TrivialVariableUsageInfo> {
        val usedVariables = hashSetOf<VariableDescriptor>()

        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction is ReadValueInstruction) {
                val descriptor = descriptorExtractor.extractFromAccessInstruction(instruction)
                if (descriptor != null && descriptor in rootVariables.trivialVariables) {
                    usedVariables.add(descriptor)
                }
            }
        }

        val usageInfo = TrivialVariableUsageInfo(usedVariables)
        return Edges(usageInfo, usageInfo)
    }

    /**
     * 处理使用指令
     */
    private fun processUsageInstruction(
        instruction: Instruction,
        incomingEdgesData: Collection<UsageVariableControlFlowInfo>
    ): Edges<UsageVariableControlFlowInfo> {
        // 合并入边数据
        val enterResult = if (incomingEdgesData.size == 1) {
            incomingEdgesData.single()
        } else {
            incomingEdgesData.fold(UsageVariableControlFlowInfo()) { result, edgeData ->
                edgeData.iterator().fold(result) { subResult, (descriptor, useState) ->
                    subResult.put(descriptor, useState.merge(subResult.getOrNull(descriptor)))
                }
            }
        }

        // 提取变量描述符
        val descriptor = PseudocodeUtil.extractVariableDescriptorFromReference(instruction, bindingContext)
            ?.takeIf { it in rootVariables.nonTrivialVariables }

        if (descriptor == null || (instruction !is ReadValueInstruction && instruction !is WriteValueInstruction)) {
            return Edges(enterResult, enterResult)
        }

        // 根据指令类型更新使用状态
        val exitResult = when (instruction) {
            is ReadValueInstruction -> enterResult.put(descriptor, VariableUseState.READ)
            is WriteValueInstruction -> {
                val currentState = enterResult.getOrNull(descriptor) ?: VariableUseState.UNUSED
                val newState = when (currentState) {
                    VariableUseState.UNUSED, VariableUseState.ONLY_WRITTEN_NEVER_READ ->
                        VariableUseState.ONLY_WRITTEN_NEVER_READ
                    VariableUseState.WRITTEN_AFTER_READ, VariableUseState.READ ->
                        VariableUseState.WRITTEN_AFTER_READ
                }
                enterResult.put(descriptor, newState)
            }
            else -> enterResult
        }

        return Edges(enterResult, exitResult)
    }
}
