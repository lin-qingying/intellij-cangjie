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

package org.cangnova.cangjie.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.EXTEND_MEMBER_REQUIRES_INTERFACE_IMPORT
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.resolve.scopes.LexicalScope

/**
 * 扩展成员可见性检查器
 *
 * 检查使用扩展成员时，是否导入了扩展实现的至少一个接口。
 * 这是仓颉语言的扩展可见性规则：
 *
 * 1. 如果 extend 与被扩展类型在**同一个包**：不需要导入接口也能访问扩展成员
 * 2. 如果 extend 与被扩展类型在**不同包**：必须导入接口才能访问扩展成员
 *
 * 这与编译器实现一致（TypeCheckExtend.cpp:SetExtendExternalAttr）
 *
 * 工作原理：
 * 1. 检查解析的调用对象是否是扩展的成员（containingDeclaration 是 ExtendDescriptor）
 * 2. 如果是，获取扩展实现的所有接口
 * 3. 检查 extend 和被扩展类型是否在同一个包
 * 4. 如果在不同包，检查当前作用域中是否有任何接口可见
 * 5. 如果在不同包且没有接口可见，报告错误
 *
 * 示例：
 * ```cangjie
 * // package a
 * package a
 * public class Foo {}
 *
 * // package b
 * package b
 * import a.Foo
 * public interface Printable {
 *     func print(): Unit
 * }
 * extend Foo <: Printable {  // extend 在 b 包，Foo 在 a 包（不同包）
 *     public func print() { println("Foo") }
 * }
 *
 * // package c
 * package c
 * import a.Foo
 * import b.Printable  // 必须导入接口
 *
 * func test() {
 *     let x = Foo()
 *     x.print()  // OK，因为 Printable 已导入
 * }
 *
 * // package d
 * package d
 * import a.Foo
 * // 没有导入 Printable
 *
 * func test() {
 *     let y = Foo()
 *     y.print()  // 错误：EXTEND_MEMBER_REQUIRES_INTERFACE_IMPORT
 * }
 * ```
 */
class ExtendMemberAccessibilityChecker : CallChecker {
    /**
     * 检查已解析的调用是否满足扩展成员可见性要求
     *
     * @param resolvedCall 已解析的调用
     * @param reportOn 用于报告错误的 PSI 元素
     * @param context 调用检查上下文，提供 trace、scope 等信息
     */
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        // 获取调用的目标描述符
        val member = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return

        // 检查成员的包含声明是否是扩展描述符
        val containingExtend = member.containingDeclaration as? ExtendDescriptor ?: return

        // 获取扩展实现的接口列表
        val implementedInterfaces = containingExtend.superTypes

        // 如果没有实现接口，则无需检查（直接扩展，不是接口扩展）
        if (implementedInterfaces.isEmpty()) {
            return
        }

        // 获取被扩展类型
        val extendedType = containingExtend.extendType
        val extendedTypeDescriptor = extendedType.constructor.declarationDescriptor

        // 获取 extend 声明所在的包
        val extendPackage = containingExtend.containingDeclaration.fqNameSafe

        // 获取被扩展类型所在的包
        val extendedTypePackage = extendedTypeDescriptor?.containingDeclaration?.fqNameSafe

        // 如果 extend 和被扩展类型在同一个包，不需要导入接口
        // （根据编译器 SetExtendExternalAttr 的逻辑）
        if (extendPackage == extendedTypePackage) {
            return
        }

        // extend 和被扩展类型在不同包，需要检查接口导入
        val currentScope = context.scope

        // 检查是否有任何接口在当前作用域中可见
        val hasVisibleInterface = implementedInterfaces.any { interfaceType ->
            val interfaceDescriptor = interfaceType.constructor.declarationDescriptor
                ?: return@any false

            // 检查接口是否通过 import 在作用域中可见
            isDescriptorAccessibleInScope(interfaceDescriptor.name, currentScope)
        }

        // 如果没有接口可见，报告错误
        if (!hasVisibleInterface) {
            context.trace.report(
                 EXTEND_MEMBER_REQUIRES_INTERFACE_IMPORT.on(
                    reportOn,
                    member,
                    implementedInterfaces.toList()
                )
            )
        }
    }

    /**
     * 检查指定名称的描述符在作用域中是否可访问
     *
     * 遍历词法作用域链，检查目标描述符是否通过 import 导入或在作用域中可见。
     *
     * @param name 要检查的名称
     * @param scope 当前词法作用域
     * @return 如果描述符可访问返回 true
     */
    private fun isDescriptorAccessibleInScope(
        name: org.cangnova.cangjie.name.Name,
        scope: org.cangnova.cangjie.resolve.scopes.LexicalScope
    ): Boolean {
        var currentScope: org.cangnova.cangjie.resolve.scopes.LexicalScope? = scope

        while (currentScope != null) {
            // 检查描述符是否通过名称在作用域中可见
            // 这包括直接 import 和同包可见性
            val classifier = currentScope.getContributedClassifier(
                name,
                NoLookupLocation.FROM_IDE
            )

            if (classifier != null) {
                return true
            }

            // 检查父作用域
            currentScope = currentScope.parent as? LexicalScope
        }

        return false
    }
}