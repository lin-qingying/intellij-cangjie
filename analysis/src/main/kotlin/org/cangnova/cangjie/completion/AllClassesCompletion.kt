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

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.getDescriptorsFiltered
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher
import org.cangnova.cangjie.indices.CangJieIndicesHelper


/**
 * 全量类补全收集器
 *
 * 负责从多个来源收集所有可用的类/枚举/类型别名候选项：
 * 1. 内置作用域（builtIns）中的嵌套类
 * 2. 全局索引中的仓颉类（通过 CangJieIndicesHelper）
 * 3. 顶层类型别名（可选）
 *
 * 使用场景：
 * - 类型位置补全（val a: ▌）
 * - 未导入类的补全（NON_IMPORTED 阶段）
 * - DOT 调用时将接收者解析为枚举类
 *
 * @param parameters IntelliJ 补全参数（包含光标位置、触发方式等）
 * @param cangjieIndicesHelper 仓颉索引查询助手（基于 StubIndex，不需要完整解析源码）
 * @param prefixMatcher 前缀匹配器（根据用户已输入的前缀过滤候选）
 * @param resolutionFacade 解析门面（用于访问内置类型作用域）
 * @param kindFilter 类类型过滤器（如只要枚举、只要接口等）
 * @param includeTypeAliases 是否包含顶层类型别名
 */
class AllClassesCompletion(
    private val parameters: CompletionParameters,
    private val cangjieIndicesHelper: CangJieIndicesHelper,
    private val prefixMatcher: PrefixMatcher,
    private val resolutionFacade: ResolutionFacade,
    private val kindFilter: (ClassKind) -> Boolean,
    private val includeTypeAliases: Boolean,
) {
    /**
     * 收集所有符合条件的类描述符，通过回调逐个交给调用方处理。
     *
     * 收集顺序：
     * 1. 内置作用域嵌套类（如 Int.Companion 等）
     * 2. 全局索引中的用户定义类
     * 3. 顶层类型别名（若 includeTypeAliases 为 true）
     *
     * 使用回调而非返回列表，是为了支持流式处理：
     * 调用方可以在收到每个描述符时立即处理（如立即加入补全列表并 flush），
     * 避免将所有类一次性加载到内存。
     *
     * @param classifierDescriptorCollector 每找到一个符合条件的类描述符时调用的回调
     */
    fun collect(classifierDescriptorCollector: (ClassifierDescriptorWithTypeParameters) -> Unit) {

        // ── 来源1：内置作用域中的嵌套类 ──
        // TODO: 这是一个临时方案，等待内置类型被纳入索引后可以统一处理
        // 只收集嵌套类（containingDeclaration is ClassDescriptor），
        // 因为顶层内置类（Int、String 等）已通过默认导入自动进入作用域，
        // 在 REFERENCE_BASIC 阶段就能找到，不需要在这里重复添加。
        collectClassesFromScope(resolutionFacade.moduleDescriptor.builtIns.BASIC_SCOPE) {
            if (it.containingDeclaration is ClassDescriptor) {
                classifierDescriptorCollector(it)
            }
        }

        // ── 来源2：全局索引中的仓颉类 ──
        // 基于 StubIndex 查询，不需要完整解析源码，速度快。
        // prefixMatcher 过滤名称前缀，kindFilter 过滤类类型（CLASS/ENUM/INTERFACE 等）
        cangjieIndicesHelper.processCangJieClasses(
            { prefixMatcher.prefixMatches(it) },
            kindFilter = kindFilter,
            processor = classifierDescriptorCollector
        )

        // ── 来源3：顶层类型别名 ──
        // typealias MyList = ArrayList<String> 这类声明
        if (includeTypeAliases) {
            cangjieIndicesHelper.processTopLevelTypeAliases(
                prefixMatcher.asStringNameFilter(),
                classifierDescriptorCollector
            )
        }
    }

    /**
     * 递归收集指定 MemberScope 中所有符合条件的类描述符。
     *
     * 递归逻辑：
     * - 遍历当前作用域的所有分类器（CLASSIFIERS）
     * - 若是 ClassDescriptor 且通过 kindFilter 和前缀匹配 → 回调收集
     * - 无论是否收集，都递归进入其 unsubstitutedMemberScope
     *   （确保嵌套类如 Outer.Inner 也能被找到）
     *
     * 注意：只用于内置作用域（来源1），用户定义的类通过索引（来源2）处理，
     * 不走此方法，避免重复收集。
     *
     * @param scope 要搜索的成员作用域
     * @param collector 找到符合条件的类时的回调
     */
    private fun collectClassesFromScope(scope: MemberScope, collector: (ClassDescriptor) -> Unit) {
        for (descriptor in scope.getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS)) {
            if (descriptor is ClassDescriptor) {
                // 当前类符合类型过滤和前缀匹配时才收集
                if (kindFilter(descriptor.kind) && prefixMatcher.prefixMatches(descriptor.name.asString())) {
                    collector(descriptor)
                }

                // 无论当前类是否被收集，都递归搜索其成员作用域（处理嵌套类）
                collectClassesFromScope(descriptor.unsubstitutedMemberScope, collector)
            }
        }
    }
}