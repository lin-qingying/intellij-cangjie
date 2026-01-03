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

package org.cangnova.cangjie.icon
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.ClassKind.*
import org.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.psi.CjElement
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Iconable.IconFlags
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon
import org.jetbrains.annotations.Nullable
import javax.swing.Icon

/**
 * 仓颉语言描述符图标提供器
 *
 * 负责为各种仓颉语言的声明描述符提供对应的图标，包括：
 * - 类、接口、枚举、结构体等类型
 * - 函数、方法
 * - 属性、变量、参数
 * - 包和模块
 *
 * 图标可以包含可见性修饰符（public、private、protected、internal）的叠加显示
 */
object CangJieDescriptorIconProvider {
    private val LOG: Logger = Logger.getInstance(CangJieDescriptorIconProvider::class.java)

    /**
     * 获取描述符对应的图标
     *
     * 根据描述符类型、可见性和其他属性返回合适的图标
     *
     * @param descriptor 声明描述符
     * @param declaration PSI 元素（可选）
     * @param flags 图标标志，控制是否显示可见性等附加信息
     * @return 对应的图标，如果无法确定则返回 null
     */
    @Nullable
    fun getIcon(
        descriptor: DeclarationDescriptor,
        @Nullable declaration: PsiElement?,
        @IconFlags flags: Int
    ): Icon? {
        if (declaration != null && declaration !is CjElement) {
            return declaration.getIcon(flags)
        }

        var result: Icon? = getBaseIcon(descriptor)
        if (flags and Iconable.ICON_FLAG_VISIBILITY > 0) {
            val rowIcon = RowIcon(2)
            rowIcon.setIcon(result, 0)
            rowIcon.setIcon(getVisibilityIcon(descriptor), 1)
            result = rowIcon
        }

        return result
    }

    /**
     * 获取可见性图标
     *
     * 根据描述符的可见性修饰符返回对应的图标
     *
     * @param descriptor 声明描述符
     * @return 可见性图标（public、protected、private、internal），如果不适用则返回 null
     */
    private fun getVisibilityIcon(descriptor: DeclarationDescriptor): Icon? {
        if (descriptor is DeclarationDescriptorWithVisibility) {
            return when (val visibility = descriptor.visibility.normalize()) {
                DescriptorVisibilities.PUBLIC -> AllIcons.Nodes.C_public
                DescriptorVisibilities.PROTECTED -> AllIcons.Nodes.C_protected

                DescriptorVisibilities.INTERNAL -> AllIcons.Nodes.C_plocal

                else -> if (DescriptorVisibilities.isPrivate(visibility)) {
                    AllIcons.Nodes.C_private
                } else null
            }
        }
        return null
    }

    /**
     * 安全获取成员修饰性
     *
     * 尝试获取成员描述符的修饰性（modality），如果发生异常则返回 FINAL
     *
     * @param descriptor 成员描述符
     * @return 修饰性（ABSTRACT、OPEN、FINAL 等）
     */
    private fun getModalitySafe(descriptor: MemberDescriptor): Modality {
        return try {
            descriptor.modality
        } catch (ex: InvalidModuleException) {
            Modality.FINAL
        }
    }

    /**
     * 获取基础图标
     *
     * 根据描述符类型返回对应的基础图标（不包含可见性叠加）
     * 支持的描述符类型包括：
     * - 包（PackageFragmentDescriptor、PackageViewDescriptor）
     * - 函数和方法（FunctionDescriptor）
     * - 类、接口、枚举、结构体（ClassDescriptor）
     * - 变量、属性、参数（VariableDescriptor、PropertyDescriptor、ValueParameterDescriptor）
     * - 类型参数和类型别名（TypeParameterDescriptor、TypeAliasDescriptor）
     *
     * @param descriptor 声明描述符
     * @return 对应的基础图标，如果无法确定则返回 null 并记录警告
     */
    private fun getBaseIcon(descriptor: DeclarationDescriptor): Icon? {
        return when (descriptor) {
            is PackageFragmentDescriptor, is PackageViewDescriptor -> AllIcons.Nodes.Package
            is FunctionDescriptor -> {
                when {


                    descriptor.containingDeclaration is ClassDescriptor -> {
                        if (Modality.ABSTRACT == getModalitySafe(descriptor)) {
                            AllIcons.Nodes.AbstractMethod
                        } else {
                            AllIcons.Nodes.Method
                        }
                    }

                    else -> CangJieIcons.FUNCTION
                }
            }

            is ClassDescriptor -> {
                when (descriptor.kind) {
                    INTERFACE -> CangJieIcons.INTERFACE
                    ENUM  -> CangJieIcons.ENUM

                    STRUCT -> CangJieIcons.STRUCT
                    CLASS -> if (Modality.ABSTRACT == getModalitySafe(descriptor)) {
                        CangJieIcons.ABSTRACT_CLASS
                    } else {
                        CangJieIcons.CLASS
                    }

                    TUPLE -> null
                    EXTEND -> null
                    BASIC -> null
                    BUILTIN -> null
                }
            }

            is ValueParameterDescriptor -> CangJieIcons.PARAMETER
            is LocalVariableDescriptor -> if (descriptor.isVar) CangJieIcons.VAR else CangJieIcons.LET
            is PropertyDescriptor -> if (descriptor.isVar) CangJieIcons.FIELD_MPROP else CangJieIcons.FIELD_PROP
            is VariableDescriptor -> if (descriptor.isVar) CangJieIcons.FIELD_VAR else CangJieIcons.FIELD_LET
            is TypeParameterDescriptor -> AllIcons.Nodes.Class
            is TypeAliasDescriptor -> CangJieIcons.TYPE_ALIAS
            else -> {
                LOG.warn("No icon for descriptor: $descriptor")
                null
            }
        }
    }
}
