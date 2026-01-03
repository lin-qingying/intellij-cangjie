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
 * 3. 对 non-trivial 变量执行前向数据流分析：
 *    - 从函数入口开始遍历伪代码指令
 *    - 在 `VariableDeclarationInstruction` 处标记变量为已声明
 *    - 在 `WriteValueInstruction` 处标记变量为已初始化
 *    - 在分支汇合点合并不同路径的初始化状态
 * 4. 生成每条指令的变量初始化状态映射
 *
 * ### 使用分析流程
 * 1. 对 trivial 变量：所有 `ReadValueInstruction` 处标记为已使用
 * 2. 对 non-trivial 变量执行后向数据流分析：
 *    - 从函数出口开始反向遍历伪代码指令
 *    - 在 `ReadValueInstruction` 处标记为 READ
 *    - 在 `WriteValueInstruction` 处根据后续状态标记为 ONLY_WRITTEN 或 WRITTEN_AFTER_READ
 * 3. 生成每条指令的变量使用状态映射
 *
 * @property pseudocode 要分析的伪代码
 * @property bindingContext 绑定上下文，用于获取变量的描述符信息
 */
class PseudocodeVariablesData(val pseudocode: Pseudocode, private val bindingContext: BindingContext) {
    /**
     * 标记伪代码中是否包含 do-while 循环
     *
     * do-while 的特殊性：循环体至少执行一次，因此循环体内的变量初始化逻辑需要特殊处理。
     * 例如：
     * ```
     * let x: Int
     * do {
     *   x = 1  // 这里的初始化在第一次执行时有效
     * } while (condition)
     * println(x)  // x 在这里可能是已初始化的
     * ```
     */
    private val containsDoWhile = pseudocode.rootPseudocode.containsDoWhile

    /**
     * 变量数据收集器，负责执行数据流分析的核心遍历逻辑
     */
    private val pseudocodeVariableDataCollector =
        PseudocodeVariableDataCollector(bindingContext, pseudocode)

    /**
     * 声明变量的分类容器
     *
     * 将变量分为两类以优化分析性能：
     * - **valsWithTrivialInitializer**: 平凡初始化变量（无需数据流分析）
     * - **nonTrivialVariables**: 非平凡变量（需要数据流分析）
     *
     * @property valsWithTrivialInitializer 平凡初始化的 let 变量集合
     * @property nonTrivialVariables 需要数据流分析的变量集合
     */
    private class VariablesForDeclaration(
        val valsWithTrivialInitializer: Set<VariableDescriptor>,
        val nonTrivialVariables: Set<VariableDescriptor>
    ) {
        /**
         * 所有变量的合集
         *
         * 性能优化：如果没有 non-trivial 变量，直接返回 trivial 集合，避免不必要的复制
         */
        val allVars =
            if (nonTrivialVariables.isEmpty())
                valsWithTrivialInitializer
            else
                LinkedHashSet(valsWithTrivialInitializer).also { it.addAll(nonTrivialVariables) }
    }

    /**
     * 缓存每个伪代码块的变量声明信息
     *
     * key: 伪代码块
     * value: 该伪代码块中声明的变量分类信息
     */
    private val declaredVariablesForDeclaration = hashMapOf<Pseudocode, VariablesForDeclaration>()

    /**
     * 根伪代码中的所有变量（包括本地函数内的变量）
     *
     * 延迟初始化，使用 NONE 模式（非线程安全）以提升性能
     */
    private val rootVariables by lazy(LazyThreadSafetyMode.NONE) {
        getAllDeclaredVariables(pseudocode, includeInsideLocalDeclarations = true)
    }

    /**
     * 变量初始化状态的数据流分析结果
     *
     * 映射每条指令到其入口和出口的变量初始化状态。
     *
     * - **key**: 伪代码指令
     * - **value**: 该指令的入口和出口边的初始化状态
     *   - `incoming`: 执行该指令前的变量初始化状态
     *   - `outgoing`: 执行该指令后的变量初始化状态
     *
     * 延迟计算，只有在需要时才进行数据流分析
     */
    val variableInitializers: Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>> by lazy {
        computeVariableInitializers()
    }

    /**
     * 块作用域变量信息
     *
     * 记录每个变量声明在哪个作用域块中，用于判断变量的可见性和初始化要求
     */
    val blockScopeVariableInfo: BlockScopeVariableInfo
        get() = pseudocodeVariableDataCollector.blockScopeVariableInfo

    /**
     * 获取指定伪代码块中声明的所有变量
     *
     * @param pseudocode 要查询的伪代码块
     * @param includeInsideLocalDeclarations 是否包含本地函数内声明的变量
     * @return 声明的变量描述符集合
     */
    fun getDeclaredVariables(pseudocode: Pseudocode, includeInsideLocalDeclarations: Boolean): Set<VariableDescriptor> =
        getAllDeclaredVariables(pseudocode, includeInsideLocalDeclarations).allVars

    /**
     * 判断变量是否是平凡初始化的 let 变量
     *
     * @param variableDescriptor 要检查的变量描述符
     * @return true 如果该变量在声明时就已初始化，无需数据流分析
     */
    fun isVariableWithTrivialInitializer(variableDescriptor: VariableDescriptor) =
        variableDescriptor in rootVariables.valsWithTrivialInitializer

    /**
     * 获取所有声明的变量（包含或不包含本地函数内的变量）
     *
     * @param pseudocode 要分析的伪代码块
     * @param includeInsideLocalDeclarations 是否递归收集本地函数内的变量
     * @return 变量分类信息（trivial 和 non-trivial）
     */
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

        // 递归收集本地函数内的变量
        for (localFunctionDeclarationInstruction in pseudocode.localDeclarations) {
            val localPseudocode = localFunctionDeclarationInstruction.body
            addVariablesFromPseudocode(localPseudocode, nonTrivialVariables, valsWithTrivialInitializer)
        }
        return VariablesForDeclaration(
            valsWithTrivialInitializer,
            nonTrivialVariables
        )
    }

    /**
     * 从指定伪代码块中收集变量并添加到目标集合
     *
     * @param pseudocode 要分析的伪代码块
     * @param nonTrivialVariables 非平凡变量的输出集合
     * @param valsWithTrivialInitializer 平凡初始化变量的输出集合
     */
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

    /**
     * 获取当前层级（不包含本地函数）的声明变量
     *
     * 使用缓存避免重复计算
     */
    private fun getUpperLevelDeclaredVariables(pseudocode: Pseudocode) =
        declaredVariablesForDeclaration.getOrPut(pseudocode) {
            computeDeclaredVariablesForPseudocode(pseudocode)
        }

    /**
     * 计算指定伪代码块中的变量声明信息
     *
     * 遍历所有 [VariableDeclarationInstruction] 指令，对每个变量进行分类：
     * - 平凡初始化变量：参数、带初始化器的 let 变量、无后备字段的属性
     * - 非平凡变量：var 变量、不带初始化器的 let 变量、do-while 中的变量
     *
     * @param pseudocode 要分析的伪代码块
     * @return 变量分类结果
     */
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

    /**
     * 判断变量声明是否是平凡初始化的 let 变量
     *
     * @param variableDeclarationElement 变量声明元素
     * @param descriptor 变量描述符
     * @return true 如果是参数或带初始化器的 let 变量声明
     */
    private fun isValWithTrivialInitializer(variableDeclarationElement: CjDeclaration, descriptor: VariableDescriptor) =
        variableDeclarationElement is CjParameter ||
                (variableDeclarationElement as? CjVariableDeclaration)?.isVariableWithTrivialInitializer(descriptor) == true

    /**
     * 判断变量声明是否是平凡初始化的 let 变量（扩展方法）
     *
     * 平凡初始化的条件：
     * 1. 无后备字段的属性（计算属性）
     * 2. 带初始化器的 let 变量
     *
     * @param descriptor 变量描述符
     * @return true 如果满足平凡初始化条件
     */
    private fun CjVariableDeclaration.isVariableWithTrivialInitializer(descriptor: VariableDescriptor): Boolean {
        if (descriptor.isPropertyWithoutBackingField()) return true
        if (isVar) return false  // var 变量总是非平凡的
        return initializer != null  // let 变量必须有初始化器才是平凡的
    }

    /**
     * 判断变量描述符是否是无后备字段的属性
     *
     * 无后备字段的属性通常是计算属性，不需要初始化检查
     */
    private fun VariableDescriptor.isPropertyWithoutBackingField(): Boolean {
        if (this !is PropertyDescriptor) return false
        return bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) != true
    }

    // ==================== 变量初始化分析 ====================

    /**
     * 计算变量初始化状态的数据流分析结果
     *
     * ## 算法步骤：
     *
     * 1. **处理平凡变量**：调用 [computeInitInfoForTrivialVals] 直接标记为已初始化
     * 2. **处理非平凡变量**：执行前向数据流分析
     *    - 使用 [PseudocodeVariableDataCollector.collectData] 遍历伪代码
     *    - 在每条指令处：
     *      - 合并入边的初始化状态（[mergeIncomingEdgesDataForInitializers]）
     *      - 根据指令类型更新初始化状态（[addVariableInitStateFromCurrentInstructionIfAny]）
     * 3. **合并结果**：将 trivial 和 non-trivial 的分析结果组合
     *
     * @return 每条指令的变量初始化状态映射
     */
    private fun computeVariableInitializers(): Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>> {

        val blockScopeVariableInfo = pseudocodeVariableDataCollector.blockScopeVariableInfo

        // 第一步：计算平凡变量的初始化状态（无需数据流分析）
        val resultForValsWithTrivialInitializer = computeInitInfoForTrivialVals()

        // 如果没有非平凡变量，直接返回平凡变量的结果
        if (rootVariables.nonTrivialVariables.isEmpty()) return resultForValsWithTrivialInitializer

        // 第二步：对非平凡变量执行前向数据流分析
        return pseudocodeVariableDataCollector.collectData(
            TraversalOrder.FORWARD,  // 前向遍历：从函数入口到出口
            VariableInitControlFlowInfo()  // 初始状态：所有变量未初始化
        ) { instruction: Instruction, incomingEdgesData: Collection<VariableInitControlFlowInfo> ->

            // 合并所有入边的初始化状态
            val enterInstructionData =
                mergeIncomingEdgesDataForInitializers(
                    instruction,
                    incomingEdgesData,
                    blockScopeVariableInfo
                )
            // 根据当前指令更新初始化状态
            val exitInstructionData = addVariableInitStateFromCurrentInstructionIfAny(
                instruction, enterInstructionData, blockScopeVariableInfo
            )
            Edges(enterInstructionData, exitInstructionData)
        }.mapValues { (instruction, edges) ->
            // 第三步：将 trivial 和 non-trivial 结果组合
            val trivialEdges = resultForValsWithTrivialInitializer[instruction]!!
            Edges(
                trivialEdges.incoming.replaceDelegate(edges.incoming),
                trivialEdges.outgoing.replaceDelegate(edges.outgoing)
            )
        }
    }

    /**
     * 计算平凡初始化变量的初始化状态
     *
     * 平凡变量的初始化状态可以直接通过静态分析确定，无需数据流分析：
     * - 在 `VariableDeclarationInstruction` 处标记为已声明
     * - 在 `WriteValueInstruction` 处（如果是声明时初始化）标记为已初始化
     *
     * **优化点**: 平凡变量的状态在所有控制流路径上都是确定的，因此可以在一次遍历中完成
     *
     * @return 每条指令的平凡变量初始化状态映射
     */
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

    /**
     * 判断 WriteValueInstruction 是否是平凡初始化器
     *
     * 平凡初始化器的特征：
     * - 指令的元素是一个声明（CjDeclaration）
     * - 表示在声明变量的同时进行初始化（如 `let x = 1`）
     *
     * @receiver WriteValueInstruction
     * @return true 如果是声明时的初始化
     */
    private fun WriteValueInstruction.isTrivialInitializer() =
    // WriteValueInstruction having CjDeclaration as an element means
    // it must be a write happened at the same time when
        // the variable (common variable/parameter/object) has been declared
        element is CjDeclaration

    /**
     * 只读初始化变量控制流信息实现（用于平凡变量）
     *
     * 此类用于表示平凡初始化变量的初始化状态，使用不可变集合以提高性能。
     * 通过委托模式，可以将平凡变量的状态与非平凡变量的状态组合。
     *
     * @property declaredSet 已声明的平凡变量集合
     * @property initSet 已初始化的平凡变量集合
     * @property delegate 委托给非平凡变量的控制流信息（用于组合结果）
     */
    private inner class ReadOnlyInitVariableControlFlowInfoImpl(
        val declaredSet: ImmutableSet<VariableDescriptor>,
        val initSet: ImmutableSet<VariableDescriptor>,
        private val delegate: VariableInitReadOnlyControlFlowInfo?
    ) : VariableInitReadOnlyControlFlowInfo {
        /**
         * 获取变量的初始化状态
         *
         * 查询顺序：
         * 1. 如果变量在 declaredSet 中，返回其初始化状态（基于 initSet）
         * 2. 否则委托给 delegate 查询（用于非平凡变量）
         *
         * @param key 要查询的变量描述符
         * @return 变量的控制流状态，如果未找到则返回 null
         */
        override fun getOrNull(key: VariableDescriptor): VariableControlFlowState? {
            if (key in declaredSet) {
                return VariableControlFlowState.create(isInitialized = key in initSet, isDeclared = true)
            }
            return delegate?.getOrNull(key)
        }

        /**
         * 检查 match 表达式中的确定初始化
         *
         * 委托给非平凡变量的检查逻辑
         */
        override fun checkDefiniteInitializationInMatch(merge: VariableInitReadOnlyControlFlowInfo): Boolean =
            delegate?.checkDefiniteInitializationInMatch(merge) ?: false

        /**
         * 替换委托对象
         *
         * 用于在合并平凡和非平凡变量结果时，创建新的组合对象
         *
         * @param newDelegate 新的委托对象（非平凡变量的控制流信息）
         * @return 新的控制流信息对象
         */
        fun replaceDelegate(newDelegate: VariableInitReadOnlyControlFlowInfo): VariableInitReadOnlyControlFlowInfo =
            ReadOnlyInitVariableControlFlowInfoImpl(declaredSet, initSet, newDelegate)

        /**
         * 将控制流信息转换为不可变映射
         *
         * @return 变量描述符到控制流状态的映射
         */
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

    /**
     * 根据当前指令更新变量初始化状态
     *
     * 此方法是前向数据流分析的核心，负责根据指令类型更新变量的初始化状态：
     *
     * ## 处理的指令类型：
     *
     * ### 1. MagicInstruction (EXHAUSTIVE_MATCH_ELSE)
     * - 在 match 表达式的 else 分支中，标记所有未初始化的变量为"穷尽初始化"
     * - 这是因为如果能到达 else 分支，说明其他分支都没有覆盖到，变量可能在某些分支中被初始化
     *
     * ### 2. WriteValueInstruction
     * - 表示变量赋值操作
     * - 检查是否是对 this 或无派发接收者的写入（确保不是对对象属性的写入）
     * - 将变量标记为已初始化
     *
     * ### 3. VariableDeclarationInstruction
     * - 表示变量声明
     * - 如果变量未初始化且未声明，标记为已声明
     * - 保留现有的初始化状态
     *
     * @param instruction 当前指令
     * @param enterInstructionData 指令入口的初始化状态
     * @param blockScopeVariableInfo 块作用域信息
     * @return 指令出口的初始化状态
     */
    private fun addVariableInitStateFromCurrentInstructionIfAny(
        instruction: Instruction,
        enterInstructionData: VariableInitControlFlowInfo,
        blockScopeVariableInfo: BlockScopeVariableInfo
    ): VariableInitControlFlowInfo {
        // 处理 match 表达式的穷尽 else 分支
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
        // 只处理写入和声明指令
        if (instruction !is WriteValueInstruction && instruction !is VariableDeclarationInstruction) {
            return enterInstructionData
        }
        // 提取变量描述符（必须是非平凡变量）
        val variable =
            PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)
                ?.takeIf { it in rootVariables.nonTrivialVariables }
                ?: return enterInstructionData
        var exitInstructionData = enterInstructionData
        if (instruction is WriteValueInstruction) {
            // 处理变量赋值：确保是对变量本身的赋值，而不是对对象属性的赋值
            if (!PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, bindingContext)) {
                return enterInstructionData
            }

            val enterInitState = enterInstructionData.getOrNull(variable)
            val initializationAtThisElement =
                VariableControlFlowState.create(instruction.element is CjProperty, enterInitState)
            exitInstructionData = exitInstructionData.put(variable, initializationAtThisElement, enterInitState)
        } else {
            // 处理变量声明：instruction instanceof VariableDeclarationInstruction
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

    // ==================== 变量使用分析 ====================

    /**
     * 变量使用状态的数据流分析结果
     *
     * 通过后向数据流分析检测变量的使用情况：
     * - [VariableUseState.READ]: 变量被读取
     * - [VariableUseState.ONLY_WRITTEN_NEVER_READ]: 变量只被写入，从未被读取
     * - [VariableUseState.WRITTEN_AFTER_READ]: 变量先被读取，后被写入
     * - [VariableUseState.UNUSED]: 变量未被使用
     *
     * 延迟计算，只有在需要时才进行后向数据流分析
     */
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

    /**
     * 计算平凡变量的使用信息
     *
     * 对于平凡变量（在声明时就已初始化的变量），使用分析非常简单：
     * - 所有 ReadValueInstruction 处的平凡变量都标记为 READ
     * - 无需后向数据流分析
     *
     * @return 平凡变量的使用状态边（入口和出口状态相同）
     */
    private fun computeUseInfoForTrivialVals(): Edges<ReadOnlyUseControlFlowInfoImpl> {
        val used = hashSetOf<VariableDescriptor>()

        // 遍历所有指令，收集被读取的平凡变量
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

    /**
     * 从指令中提取平凡初始化的 let 变量
     *
     * @param instruction 要检查的指令
     * @return 如果指令涉及平凡变量，返回变量描述符；否则返回 null
     */
    private fun extractValWithTrivialInitializer(instruction: Instruction): VariableDescriptor? {
        return PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)?.takeIf {
            it in rootVariables.valsWithTrivialInitializer
        }
    }

    /**
     * 只读使用控制流信息实现（用于平凡变量）
     *
     * 此类用于表示平凡初始化变量的使用状态。
     * 通过委托模式，可以将平凡变量的状态与非平凡变量的状态组合。
     *
     * @property used 已使用的平凡变量集合
     * @property delegate 委托给非平凡变量的使用控制流信息
     */
    private inner class ReadOnlyUseControlFlowInfoImpl(
        val used: Set<VariableDescriptor>,
        val delegate: VariableUsageReadOnlyControlInfo?
    ) : VariableUsageReadOnlyControlInfo {
        /**
         * 获取变量的使用状态
         *
         * @param key 要查询的变量描述符
         * @return 变量的使用状态，如果未找到则返回 null
         */
        override fun getOrNull(key: VariableDescriptor): VariableUseState? {
            if (key in used) return VariableUseState.READ
            return delegate?.getOrNull(key)
        }

        /**
         * 替换委托对象
         *
         * @param newDelegate 新的委托对象（非平凡变量的使用控制流信息）
         * @return 新的使用控制流信息对象
         */
        fun replaceDelegate(newDelegate: VariableUsageReadOnlyControlInfo): VariableUsageReadOnlyControlInfo =
            ReadOnlyUseControlFlowInfoImpl(used, newDelegate)

        /**
         * 将使用控制流信息转换为不可变映射
         *
         * @return 变量描述符到使用状态的映射
         */
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

    /**
     * 伴生对象：包含数据流分析的辅助方法
     */
    companion object {

        /**
         * 获取变量的默认初始化状态
         *
         * 当变量在数据流分析中没有显式状态时，使用此方法计算默认状态。
         *
         * ## 默认状态规则：
         *
         * - **声明在外部作用域**：如果变量声明在当前分析的代码块外部，默认为已初始化
         *   - 例如：函数参数、外层函数的变量
         * - **声明在当前作用域**：如果变量声明在当前代码块内，默认为未初始化
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
            //todo: think of replacing it with "MapWithDefaultValue"
            val declaredIn = blockScopeVariableInfo.declaredIn[variable]
            val declaredOutsideThisDeclaration =
                declaredIn == null //declared outside this pseudocode
                        || declaredIn.blockScopeForContainingDeclaration != instruction.blockScope.blockScopeForContainingDeclaration
            return VariableControlFlowState.create(isInitialized = declaredOutsideThisDeclaration)
        }

        /**
         * 空的初始化控制流信息（用于初始化和优化）
         */
        private val EMPTY_INIT_CONTROL_FLOW_INFO = VariableInitControlFlowInfo()

        /**
         * 合并多个入边的初始化状态
         *
         * 在控制流汇合点（如 if-else 的出口），需要合并不同路径的初始化状态。
         *
         * ## 合并规则：
         *
         * - **初始化状态**：对每个变量，合并所有入边的初始化状态
         *   - 如果所有路径都已初始化，则合并后为已初始化
         *   - 如果任一路径未初始化，则合并后为可能未初始化
         * - **声明状态**：如果任一路径未声明，则合并后为未声明
         *
         * @param instruction 当前指令（汇合点）
         * @param incomingEdgesData 所有入边的初始化状态集合
         * @param blockScopeVariableInfo 块作用域信息
         * @return 合并后的初始化状态
         */
        private fun mergeIncomingEdgesDataForInitializers(
            instruction: Instruction,
            incomingEdgesData: Collection<VariableInitControlFlowInfo>,
            blockScopeVariableInfo: BlockScopeVariableInfo
        ): VariableInitControlFlowInfo {
            // 快速路径：只有一个入边，直接返回
            if (incomingEdgesData.size == 1) return incomingEdgesData.single()
            // 快速路径：没有入边，返回空状态
            if (incomingEdgesData.isEmpty()) return EMPTY_INIT_CONTROL_FLOW_INFO

            // 收集所有入边涉及的变量
            val variablesInScope = linkedSetOf<VariableDescriptor>()
            for (edgeData in incomingEdgesData) {
                variablesInScope.addAll(edgeData.keySet())
            }

            // 对每个变量，合并其在所有入边的状态
            return variablesInScope.fold(EMPTY_INIT_CONTROL_FLOW_INFO) { result, variable ->
                var initState: InitState? = null
                var isDeclared = true
                for (edgeData in incomingEdgesData) {
                    // 获取变量在当前入边的状态，如果没有则使用默认状态
                    val varControlFlowState = edgeData.getOrNull(variable)
                        ?: getDefaultValueForInitializers(
                            variable,
                            instruction,
                            blockScopeVariableInfo
                        )
                    // 合并初始化状态（如果任一路径未初始化，则合并后为可能未初始化）
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
