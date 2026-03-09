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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.completion.smart.SmartCompletion
import org.cangnova.cangjie.indices.fuzzyType
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import org.cangnova.cangjie.types.isFunctionType

/**
 * 智能代码补全会话
 *
 * 基于类型推导的智能补全,提供更精确的补全建议。
 * 与基础补全不同,智能补全会:
 * - 根据预期类型过滤候选项
 * - 提供类型转换建议
 * - 建议适合的工厂方法
 * - 根据上下文排序候选项
 *
 * 智能补全通常在用户明确请求时触发(如按 Ctrl+Shift+Space)
 *
 * @property configuration 补全会话配置
 * @property parameters 补全参数
 * @property resultSet 补全结果集
 */
class SmartCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    resultSet: CompletionResultSet
) : CompletionSession(configuration, parameters, resultSet) {

    /**
     * 智能补全引擎(延迟初始化)
     *
     * 负责根据表达式的预期类型生成智能补全建议
     */
    private val smartCompletion by lazy(LazyThreadSafetyMode.NONE) {
        expression?.let {
            SmartCompletion(
                it, resolutionFacade, bindingContext, moduleDescriptor, isVisibleFilter, applicabilityFilter,
                indicesHelper(false), prefixMatcher, searchScope, toFromOriginalFileMapper,
                callTypeAndReceiver
            )
        }
    }

    /**
     * 描述符类型过滤器
     *
     * 智能补全主要关注值类型的声明,但排除 SAM 构造器(因为它们需要单独处理)
     * 如果预期类型是函数类型,还会包含类(用于构造器引用)
     */
    override val descriptorKindFilter: DescriptorKindFilter by lazy {
         
        val filter = DescriptorKindFilter.VALUES
        // 检查是否预期函数类型(用于构造器引用,如 ::MyClass)
        val referenceToConstructorIsApplicable = smartCompletion?.expectedInfos.orEmpty().any {
            it.fuzzyType?.type?.isFunctionType == true
        }

        if (referenceToConstructorIsApplicable) {
            filter.withKinds(DescriptorKindFilter.CLASSES_MASK)
        } else {
            filter
        }
    }

    /**
     * 预期信息集合
     *
     * 包含当前表达式位置预期的类型、名称等信息
     */
    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    /**
     * 执行智能补全
     *
     * 注意: 智能补全的实际逻辑在 SmartCompletion 类中
     * 此方法为空是因为智能补全通常作为基础补全的增强而不是独立使用
     */
    override fun doComplete() {
        // 智能补全通常嵌入在基础补全中
        // 如果需要独立的智能补全,应在此处调用 smartCompletion 的方法
    }
}
