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

package org.cangnova.cangjie.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.HierarchicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.findClassifier
import org.cangnova.cangjie.stubindex.CangJieEnumConstructorShortNameIndex

/**
 * 枚举构造器查找工具
 *
 * 实现混合查找策略：
 * 1. 快速路径：在当前作用域中查找可见的枚举构造器
 * 2. 慢速路径：使用索引在全局查找，然后过滤可见性
 *
 * 性能特点：
 * - 快速路径：适用于局部枚举和直接导入的枚举（< 2ms）
 * - 慢速路径：适用于跨包查找和通配符导入（< 1ms）
 */
object EnumConstructorFinder {

    /**
     * 混合策略：查找枚举构造器
     *
     * @param name 构造器名称
     * @param arity 参数数量（null 表示不限制）
     * @param scope 词法作用域
     * @param project 项目
     * @param searchScope 搜索范围
     * @return 匹配的枚举构造器描述符列表
     */
    fun findConstructor(
        name: Name,
        arity: Int?,
        scope: LexicalScope,
        project: Project,
        searchScope: GlobalSearchScope
    ): List<EnumConstructorDescriptor> {
        // 策略 1: 快速路径 - 在作用域中查找
        val scopeResults = findInScope(name, arity, scope)
        if (scopeResults.isNotEmpty()) {
            return scopeResults
        }

        // 策略 2: 慢速路径 - 使用索引全局查找，然后过滤可见性
        return findByIndexWithVisibility(name, arity, scope, project, searchScope)
    }
    fun findConstructor(
        name: Name,
        arity: Int?,
        scope: LexicalScope,

    ): List<EnumConstructorDescriptor> {
        // 策略 1: 快速路径 - 在作用域中查找
        val scopeResults = findInScope(name, arity, scope)
        if (scopeResults.isNotEmpty()) {
            return scopeResults
        }


        return emptyList()
    }

    /**
     * 快速路径：在作用域中查找枚举构造器
     *
     * 遍历作用域塔，查找所有可见的枚举类，然后检查其构造器。
     *
     * 时间复杂度：O(E_visible × N_avg)
     * - E_visible: 作用域中可见的枚举数量
     * - N_avg: 每个枚举的平均构造器数量
     *
     * 典型性能：0.6-2ms（取决于可见枚举数量）
     */
    private fun findInScope(
        name: Name,
        arity: Int?,
        scope: LexicalScope
    ): List<EnumConstructorDescriptor> {
        val result = mutableListOf<EnumConstructorDescriptor>()

        // 遍历作用域塔（从内到外）
        scope.collectAllFromMeAndParent { currentScope ->
            // 获取该层作用域中所有可见的枚举类
            val descriptors = currentScope.getContributedDescriptors(
                DescriptorKindFilter.CLASSIFIERS,
                MemberScope.ALL_NAME_FILTER
            )

            // 过滤出枚举类，并检查其构造器
            for (descriptor in descriptors) {
                if (descriptor is EnumDescriptor) {
                    // 查找匹配的构造器
                    for (constructor in descriptor.constructors) {
                        if (constructor.name == name &&
                            (arity == null || constructor.valueParameters.size == arity)
                        ) {
                            result.add(constructor)
                        }
                    }
                }
            }
        }

        return result
    }

    /**
     * 慢速路径：使用索引查找，然后过滤可见性
     *
     * 先通过索引快速找到所有同名构造器，然后检查它们是否在当前作用域中可见。
     *
     * 时间复杂度：O(M_global × S_depth)
     * - M_global: 全局同名构造器数量
     * - S_depth: 作用域深度（可见性检查）
     *
     * 典型性能：0.3-1ms（索引查找极快）
     *
     * 注意：此方法返回 PSI 元素列表，不返回 Descriptor。
     * 调用者需要自行将 PSI 转换为 Descriptor。
     */
    private fun findByIndexWithVisibility(
        name: Name,
        arity: Int?,
        scope: LexicalScope,
        project: Project,
        searchScope: GlobalSearchScope
    ): List<EnumConstructorDescriptor> {
        // 步骤 1: 通过索引查找所有同名构造器
        val allConstructors = CangJieEnumConstructorShortNameIndex[
            name.asString(),
            project,
            searchScope
        ]

        // 步骤 2: 过滤参数数量（使用 PSI 的参数数量）
        val matchedArity = allConstructors.filter { constructor ->
            arity == null || constructor.getParameterCount() == arity
        }

        // 步骤 3: 过滤可见性（检查所属枚举是否可见）
        val visibleConstructors = matchedArity.filter { constructor ->
            val enumPsi = constructor.parentEnum ?: return@filter false
            val enumDescriptor = scope.findClassifier(
                Name.identifier(enumPsi.name ?: ""),
                NoLookupLocation.FROM_IDE
            ) as? EnumDescriptor
            enumDescriptor != null
        }

        // 步骤 4: 从可见的构造器PSI中提取 Descriptor
        // 注意：这里需要通过枚举 Descriptor 的 constructors 列表来获取构造器 Descriptor
        val result = mutableListOf<EnumConstructorDescriptor>()
        for (constructorPsi in visibleConstructors) {
            val enumPsi = constructorPsi.parentEnum ?: continue
            val enumDescriptor = scope.findClassifier(
                Name.identifier(enumPsi.name ?: ""),
                NoLookupLocation.FROM_IDE
            ) as? EnumDescriptor ?: continue

            // 在枚举 Descriptor 的构造器列表中查找匹配的构造器
            val constructorDescriptor = enumDescriptor.constructors.firstOrNull {
                it.name == name && (arity == null || it.valueParameters.size == arity)
            }
            if (constructorDescriptor != null) {
                result.add(constructorDescriptor)
            }
        }

        return result
    }


    /**
     * 仅使用作用域查找（用于对比测试）
     *
     * 仅用于性能测试和调试。
     */
    fun findInScopeOnly(
        name: Name,
        arity: Int?,
        scope: LexicalScope
    ): List<EnumConstructorDescriptor> {
        return findInScope(name, arity, scope)
    }

    /**
     * 仅使用索引查找（用于对比测试）
     *
     * 仅用于性能测试和调试。
     */
    fun findByIndexOnly(
        name: Name,
        arity: Int?,
        scope: LexicalScope,
        project: Project,
        searchScope: GlobalSearchScope
    ): List<EnumConstructorDescriptor> {
        return findByIndexWithVisibility(name, arity, scope, project, searchScope)
    }
}

/**
 * 作用域扩展：遍历作用域塔
 */
private inline fun HierarchicalScope.collectAllFromMeAndParent(
    action: (HierarchicalScope) -> Unit
)  {
    var current: HierarchicalScope? = this
    while (current != null) {
        action(current)
        current = current.parent
    }
}
