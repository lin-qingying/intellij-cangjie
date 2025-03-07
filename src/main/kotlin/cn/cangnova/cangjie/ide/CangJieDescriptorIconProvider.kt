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

package cn.cangnova.cangjie.ide

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.ClassKind.*
import cn.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.psi.CjElement
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Iconable.IconFlags
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon
import org.jetbrains.annotations.Nullable
import javax.swing.Icon

object CangJieDescriptorIconProvider {
    private val LOG: Logger = Logger.getInstance(CangJieDescriptorIconProvider::class.java)

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

    private fun getModalitySafe(descriptor: MemberDescriptor): Modality {
        return try {
            descriptor.modality
        } catch (ex: InvalidModuleException) {
            Modality.FINAL
        }
    }

    private fun getBaseIcon(descriptor: DeclarationDescriptor): Icon? {
        return when (descriptor) {
            is PackageFragmentDescriptor, is PackageViewDescriptor -> AllIcons.Nodes.Package
            is FunctionDescriptor -> {
                when {
                    descriptor.extensionReceiverParameter != null -> {
                        if (Modality.ABSTRACT == getModalitySafe(descriptor)) {
                            CangJieIcons.ABSTRACT_EXTENSION_FUNCTION
                        } else {
                            CangJieIcons.EXTENSION_FUNCTION
                        }
                    }

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
                    ENUM, ENUM_ENTRY -> CangJieIcons.ENUM
                    ANNOTATION_CLASS -> CangJieIcons.ANNOTATION
                    STRUCT -> CangJieIcons.STRUCT
                    CLASS -> if (Modality.ABSTRACT == getModalitySafe(descriptor)) {
                        CangJieIcons.ABSTRACT_CLASS
                    } else {
                        CangJieIcons.CLASS
                    }

                    TUPLE -> null
                    EXTEND -> null
                    BASIC -> null

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
