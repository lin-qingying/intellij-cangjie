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

package com.linqingying.cangjie.resolve

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.Errors.REDECLARATION
import com.linqingying.cangjie.diagnostics.reportOnDeclaration
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.resolve.lazy.TopLevelDescriptorProvider
import com.linqingying.cangjie.utils.addIfNotNull

class DeclarationResolver(
    private val annotationResolver: AnnotationResolver,
    private val trace: BindingTrace
) {
    private fun getTopLevelDescriptorsByFqName(
        topLevelDescriptorProvider: TopLevelDescriptorProvider,
        fqName: FqName,
        location: LookupLocation
    ): Set<DeclarationDescriptor> {
        val descriptors = HashSet<DeclarationDescriptor>()

        descriptors.addIfNotNull(topLevelDescriptorProvider.getPackageFragment(fqName))
        descriptors.addAll(topLevelDescriptorProvider.getTopLevelClassifierDescriptors(fqName, location))
        return descriptors
    }

    /**
     * 检查并报告具有相同名称但不同声明的描述符重新声明错误
     * 此函数专注于识别具有分类描述符的重新声明
     *
     * @param descriptorMap 一个多重映射，将名称映射到声明描述符列表，用于检查重新声明
     */
    private fun reportRedeclarationsWithClassifiers(descriptorMap: Multimap<Name, DeclarationDescriptor>) {
        // 遍历所有名称，以识别可能的重新声明
        for (name in descriptorMap.keySet()) {
            // 获取具有相同名称的所有声明描述符
            val descriptors = descriptorMap[name]
            // 检查是否存在多个描述符，并且其中至少有一个是分类描述符
            if (descriptors.size > 1 && descriptors.any { it is ClassifierDescriptor }) {
                // 遍历每个描述符，准备报告重新声明错误
                for (descriptor in descriptors) {
                    // 报告每个描述符的重新声明错误，使用REDECLARATION诊断工厂
                    reportOnDeclaration(trace, descriptor) { REDECLARATION.on(it, descriptors) }
                }
            }
        }
    }

    /**
     * 检查在给定的上下文中是否有重复声明的类、属性或变量
     * 此函数旨在防止在同一个类中出现名称相同但类型不同的成员，这可能导致混淆或编译错误
     *
     * @param c 一个表示顶级向下分析上下文的对象，包含了待检查的类和它们的成员信息
     */
    fun checkRedeclarations(c: TopDownAnalysisContext) {
        // 遍历上下文中所有已声明的类
        for (classDescriptor in c.declaredClasses.values) {
            // 创建一个多功能映射，用于存储名称到声明描述符的映射
            val descriptorMap = HashMultimap.create<Name, DeclarationDescriptor>()

            // 遍历当前类的所有成员声明
            for (desc in classDescriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
                // 检查声明是否为类、属性或变量，并且不是枚举条目
                if ((desc is ClassifierDescriptor || desc is PropertyDescriptor || desc is VariableDescriptor) && desc !is EnumEntryDescriptor) {
                    // 将符合条件的声明添加到映射中
                    descriptorMap.put(desc.name, desc)
                }
            }

            // 报告所有在当前类中具有相同名称的声明
            reportRedeclarationsWithClassifiers(descriptorMap)
        }
    }

    /**
     * 检查包中的重复声明
     *
     * 此函数旨在检测在同一个包中是否存在重复的顶级声明，包括类、函数和属性等
     * 它通过比较预期的声明和实际的声明来确定是否存在重复
     *
     * @param topLevelDescriptorProvider 顶级描述符提供者，用于获取顶级声明的描述符
     * @param topLevelFqNames 包含完全限定名和相应元素的多重映射
     */
    fun checkRedeclarationsInPackages(
        topLevelDescriptorProvider: TopLevelDescriptorProvider,
        topLevelFqNames: Multimap<FqName, CjElement>
    ) {
        // 遍历所有顶级完全限定名及其对应的声明或包指令
        for ((fqName, declarationsOrPackageDirectives) in topLevelFqNames.asMap()) {
            // 如果是根包，则跳过
            if (fqName.isRoot) continue

            // TODO: report error on expected class and actual val, or vice versa
            // 分区获取预期的和实际的顶级描述符
            val (expected, actual) =
                getTopLevelDescriptorsByFqName(
                    topLevelDescriptorProvider,
                    fqName,
                    NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS
                )
                    .partition { it is MemberDescriptor && it.isExpect }

            // 检查预期声明和实际声明中是否存在重复
            for (descriptors in listOf(expected, actual)) {
                if (descriptors.size > 1) {
                    // 如果存在重复声明，报告错误
                    for (directive in declarationsOrPackageDirectives) {
                        val reportAt = (directive as? CjPackageDirective)?.nameIdentifier ?: directive
                        trace.report(
                            Errors.PACKAGE_OR_CLASSIFIER_REDECLARATION.on(
                                reportAt,
                                fqName.shortName().asString()
                            )
                        )
                    }
                }
            }
        }
    }
}
