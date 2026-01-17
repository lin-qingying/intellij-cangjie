/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker


/**
 * 约束系统接口
 *
 * 用于类型推断过程中管理和求解类型约束的核心系统。
 * 约束系统负责收集、存储和解决类型变量之间的各种约束关系。
 */
interface ConstraintSystem {
    /**
     * 标识约束系统是否包含矛盾
     *
     * 当系统中存在无法同时满足的约束时，此属性为 true。
     * 例如：要求某个类型变量既是 Int 又是 String
     */
    val hasContradiction: Boolean

    /**
     * 约束系统中检测到的所有错误列表
     *
     * 包含在约束求解过程中发现的各种错误，如类型不匹配、
     * 循环依赖、无法推断的类型变量等。
     */
    val errors: List<ConstraintSystemError>


    /**
     * 将当前约束系统转换为约束系统完成上下文
     *
     * 完成上下文用于约束系统求解的最终阶段，提供完成类型推断所需的操作。
     *
     * @return 约束系统完成上下文对象
     */
    fun asConstraintSystemCompleterContext(): ConstraintSystemCompletionContext

    /**
     * 将约束系统转换为只读存储
     *
     * 调用此方法后，不应再通过 ConstraintSystemBuilder 修改系统状态。
     * 这确保了约束系统在某个时间点被"冻结"，防止意外修改。
     *
     * @return 只读的约束存储对象
     */
    fun asReadOnlyStorage(): ConstraintStorage

    /**
     * 获取约束系统构建器
     *
     * 构建器用于向约束系统添加新的约束和修改系统状态。
     *
     * @return 约束系统构建器对象
     */
    fun getBuilder(): ConstraintSystemBuilder

    /**
     * 将约束系统转换为延迟参数分析器上下文
     *
     * 某些参数（如 lambda 表达式）的类型推断需要延迟到收集足够的类型信息之后。
     * 此上下文提供分析这些延迟参数所需的功能。
     *
     * @return 延迟参数分析器上下文对象
     */
    fun asPostponedArgumentsAnalyzerContext(): PostponedArgumentsAnalyzerContext

    /**
     * 获取空交集类型的种类信息
     *
     * 当多个类型进行交集操作时，如果它们没有公共的子类型，
     * 则会产生空交集。此方法用于诊断和报告这种情况。
     *
     * 例如：Int & String 会产生空交集，因为没有类型同时是 Int 和 String
     *
     * @param types 参与交集操作的类型集合
     * @return 空交集类型信息，如果不是空交集则返回 null
     */
    fun getEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo?

}


/**
 * 分支点（Fork Point）机制说明：
 *
 * 在某些情况下，约束不是线性地添加到系统中的，而是需要考虑多种可能的约束变体。
 *
 * 【示例场景】
 * 假设通过智能类型转换得到了一个类型为 A<Int, String> & A<E, F> 的值，
 * 我们想将它作为参数传递给类型为 A<Xv, Yv> 的形参（其中 Xv 和 Yv 是当前调用的类型变量）。
 *
 * 因此，我们得到一个子类型约束：
 * A<Int, String> & A<E, F> <: A<Xv, Yv>
 *
 * 【分支选择】
 * 方案 1：考虑第一个交集分量，得到变量约束集：{Xv=Int, Yv=String}
 * 方案 2：考虑第二个交集分量，得到变量约束集：{Xv=E, Yv=F}
 *
 * 【复杂性问题】
 * 所有现有的和未来的约束都可能根据我们选择的方案而产生不同的效果。
 * 理论上，我们需要创建两个版本的约束系统并尝试分别解决它们。
 * 但这会导致指数级的复杂度（每个分支点都会使可能性翻倍）。
 *
 * 【解决方案】
 * 因此，我们只使用一组启发式规则来处理这种情况，在保证合理性能的前提下
 * 尽可能找到正确的类型推断结果。
 *
 * 【术语定义】
 * - 分支点（Fork Point）：需要在多个约束方案中做出选择的位置
 * - 分支点分支（Fork Point Branch）：每个可选的约束方案
 * - 每个分支由一组约束定义，如果选择该分支，这些约束需要添加到系统中
 */

/**
 * 分支点数据类型
 *
 * 表示一个分支点的所有可能分支的列表。
 * 每个元素代表一个可选的分支方案。
 */
typealias ForkPointData = List<ForkPointBranchDescription>

/**
 * 分支点分支描述类型
 *
 * 描述单个分支的具体内容，即选择该分支时需要添加的约束集合。
 * 每个约束表示为"类型变量-约束"对，指明某个类型变量应满足的约束条件。
 *
 * 使用 Set 确保约束的唯一性，避免重复添加相同的约束。
 */
typealias ForkPointBranchDescription = Set<Pair<TypeVariableMarker, Constraint>>