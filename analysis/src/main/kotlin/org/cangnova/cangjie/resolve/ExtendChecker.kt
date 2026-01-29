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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.CONFLICTING_EXTENDS
import org.cangnova.cangjie.diagnostics.infos.errors.EXTEND_MEMBER_REQUIRES_INTERFACE_IMPORT
import org.cangnova.cangjie.diagnostics.infos.errors.ORPHAN_EXTEND
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.extend.ExtendManager
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

/**
 * 扩展检查器
 *
 * 负责检查扩展声明的各种约束条件，包括：
 * 1. 孤儿规则（Orphan Rule）
 * 2. 扩展冲突检查
 */
class ExtendChecker(
    private val trace: BindingTrace
) {

    /**
     * 检查扩展是否满足孤儿规则
     *
     * 孤儿规则：扩展声明必须满足以下条件之一：
     * 1. 被扩展的类型在当前包中定义
     * 2. 至少一个实现的接口在当前包中定义
     *
     * 这个规则防止了"孤儿实例"问题：如果一个包可以为外部类型实现外部接口，
     * 那么当两个包都为同一个类型实现同一个接口时，就会产生冲突。
     *
     * 注意：检查的是包（package）级别，不是模块（module）级别。
     * 这与编译器的实现一致（TypeCheckExtend.cpp:CheckExtendOrphanRule）
     *
     * @param cjExtend PSI 扩展声明
     * @param descriptor 扩展描述符
     */
    fun checkOrphanRule(
        cjExtend: CjExtend,
        descriptor: ExtendDescriptor
    ) {
        val extendedType = descriptor.extendType
        val implementedInterfaces = descriptor.superTypes

        // 1. 纯扩展方法（不实现任何接口）不需要遵守孤儿规则
        // 因为不会产生接口实现冲突
        if (implementedInterfaces.isEmpty()) {
            return
        }

        // 2. 内置类型（如 Int64、Bool 等）不需要遵守孤儿规则
        // 内置类型属于标准库，任何包都可以为其扩展接口实现
        val extendedTypeDescriptor = extendedType.constructor.declarationDescriptor
        if (extendedTypeDescriptor != null && CangJieBuiltIns.isBuiltIn(extendedTypeDescriptor)) {
            return
        }

        val currentPackage = descriptor.containingDeclaration.fqNameSafe

        // 检查被扩展类型是否在当前包定义
        val extendedTypePackage = extendedTypeDescriptor?.containingDeclaration?.fqNameSafe
        val isExtendedTypeLocal = extendedTypePackage == currentPackage

        // 检查是否有任何实现的接口在当前包定义
        val hasLocalInterface = implementedInterfaces.any { interfaceType ->
            val interfacePackage = interfaceType.constructor.declarationDescriptor?.containingDeclaration?.fqNameSafe
            interfacePackage == currentPackage
        }

        // 如果被扩展类型和所有接口都不在当前包，报告错误
        if (!isExtendedTypeLocal && !hasLocalInterface) {
            trace.report(
                ORPHAN_EXTEND.on(
                    cjExtend,
                    extendedType,
                    implementedInterfaces.toList(),
                    descriptor.module
                )
            )
        }
    }

    /**
     * 检查扩展冲突
     *
     * 检查同一模块中是否有多个扩展为相同类型实现相同接口。
     * 这会导致二义性，因为无法确定使用哪个实现。
     *
     * @param cjExtend PSI 扩展声明
     * @param descriptor 扩展描述符
     * @param allExtends 当前模块的所有扩展描述符
     */
    fun checkExtendConflicts(
        cjExtend: CjExtend,
        descriptor: ExtendDescriptor,
        allExtends: Collection<ExtendDescriptor>
    ) {
        val currentModule = descriptor.module
        val extendedType = descriptor.extendType
        val implementedInterfaces = descriptor.superTypes

        // 检查每个实现的接口
        for (implementedInterface in implementedInterfaces) {
            // 查找其他扩展是否也为同一类型实现了同一接口
            val conflictingExtends = allExtends.filter { other ->
                // 跳过当前扩展
                other != descriptor &&
                        // 同一模块
                        other.module == currentModule &&
                        // 扩展同一类型（类型相等性检查）
                        areTypesEqual(other.extendType, extendedType) &&
                        // 实现同一接口
                        other.superTypes.any { otherInterface ->
                            areTypesEqual(otherInterface, implementedInterface)
                        }
            }

            // 如果发现冲突，报告错误
            if (conflictingExtends.isNotEmpty()) {
                val conflicting = conflictingExtends.first()
                trace.report(
                     CONFLICTING_EXTENDS.on(
                        cjExtend,
                        extendedType,
                        implementedInterface,
                        conflicting.extendId
                    )
                )
            }
        }
    }

    /**
     * 检查类型是否相等
     *
     * 使用类型检查器进行完整的类型相等性检查
     *
     * @param type1 第一个类型
     * @param type2 第二个类型
     * @return 如果类型相等返回 true
     */
    private fun areTypesEqual(type1: CangJieType, type2: CangJieType): Boolean {
        return CangJieTypeChecker.DEFAULT.equalTypes(type1, type2)
    }
}

/**
 * 扩展可见性检查器
 *
 * 检查扩展成员的使用是否满足可见性要求：
 * 使用扩展成员时，必须导入扩展实现的至少一个接口
 */
class ExtendAccessibilityChecker(
    private val trace: BindingTrace
) {

    /**
     * 检查扩展成员是否可访问
     *
     * 当解析到扩展成员时，检查扩展实现的接口是否在当前作用域中可见。
     *
     * @param member 扩展成员描述符
     * @param containingExtend 包含该成员的扩展描述符
     * @param callSite 调用点 PSI 元素
     * @param currentScope 当前词法作用域
     */
    fun checkExtendMemberAccessibility(
        member: CallableMemberDescriptor,
        containingExtend: ExtendDescriptor,
        callSite: com.intellij.psi.PsiElement,
        currentScope: LexicalScope
    ) {
        // 获取扩展实现的接口列表
        val implementedInterfaces = containingExtend.superTypes

        // 检查是否有任何接口在当前作用域中可见
        val hasVisibleInterface = implementedInterfaces.any { interfaceType ->
            val interfaceDescriptor = interfaceType.constructor.declarationDescriptor as? ClassDescriptor
                ?: return@any false

            // 检查接口是否通过 import 在作用域中可见
            isDescriptorAccessibleInScope(interfaceDescriptor, currentScope)
        }

        // 如果没有接口可见，报告错误
        if (!hasVisibleInterface) {
            trace.report(
                EXTEND_MEMBER_REQUIRES_INTERFACE_IMPORT.on(
                    callSite,
                    member,
                    implementedInterfaces.toList()
                )
            )
        }
    }

    /**
     * 检查描述符在作用域中是否可访问
     *
     * 遍历词法作用域链，检查目标描述符是否通过 import 导入或在作用域中可见。
     *
     * @param descriptor 要检查的描述符
     * @param scope 当前词法作用域
     * @return 如果描述符可访问返回 true
     */
    private fun isDescriptorAccessibleInScope(
        descriptor: DeclarationDescriptor,
        scope: LexicalScope
    ): Boolean {
        val targetFqName = DescriptorUtils.getFqNameSafe(descriptor)
        var currentScope: LexicalScope? = scope

        while (currentScope != null) {
            val classifier = currentScope.getContributedClassifier(
                descriptor.name,
                NoLookupLocation.FROM_IDE
            )
            if(classifier == descriptor) return true

            if (classifier != null && DescriptorUtils.getFqNameSafe(classifier) == targetFqName) {
                return true
            }

            currentScope = currentScope.parent as? LexicalScope
        }

        return false
    }
}