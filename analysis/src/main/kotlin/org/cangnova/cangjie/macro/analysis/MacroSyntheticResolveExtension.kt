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
 */

package org.cangnova.cangjie.macro.analysis

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.data.CjClassInfo
import org.cangnova.cangjie.macro.psi.MacroPsiExpansionService
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension.PackageSyntheticNames
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyEnumDescriptor

/**
 * 宏展开合成解析扩展
 *
 * 将宏展开生成的声明注入到 IDE 的解析管线中。
 * 通过 [MacroPsiExpansionService.getExpandedDeclarations] 获取逐宏解析的展开声明，
 * 并将它们的名称和描述符注入到对应的作用域中。
 *
 * 注册方式：在 `cangjie-analysis.xml` 中作为 `syntheticResolveExtension` 注册。
 */
internal class MacroSyntheticResolveExtension : SyntheticResolveExtension {

    companion object {
        private val LOG = Logger.getInstance(MacroSyntheticResolveExtension::class.java)
    }

    // =========================================================================
    // 包级别：注入宏生成的顶层声明
    // =========================================================================

    override fun getSyntheticPackageNames(
        thisDescriptor: PackageFragmentDescriptor,
        declarationProvider: PackageMemberDeclarationProvider
    ): PackageSyntheticNames {
        val project = getProject(thisDescriptor) ?: return PackageSyntheticNames.EMPTY
        val expansionService = MacroPsiExpansionService.getInstance(project)
        if (!expansionService.isEnabled()) return PackageSyntheticNames.EMPTY

        val functionNames = mutableSetOf<Name>()
        val variableNames = mutableSetOf<Name>()
        val classifierNames = mutableSetOf<Name>()

        for (sourceFile in declarationProvider.getPackageFiles()) {
            for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                for (declaration in info.declarations) {
                    val name = (declaration as? CjNamedDeclaration)?.nameAsName ?: continue

                    when (declaration) {
                        is CjTypeStatement -> classifierNames.add(name)
                        is CjNamedFunction -> functionNames.add(name)
                        is CjProperty -> variableNames.add(name)
                    }
                }
            }
        }

        if (functionNames.isEmpty() && variableNames.isEmpty() && classifierNames.isEmpty()) {
            return PackageSyntheticNames.EMPTY
        }

        return PackageSyntheticNames(functionNames, variableNames, classifierNames)
    }

    override fun generateSyntheticTopLevelClasses(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        val project = getProject(thisDescriptor) ?: return
        val expansionService = MacroPsiExpansionService.getInstance(project)
        if (!expansionService.isEnabled()) return

        for (sourceFile in declarationProvider.getPackageFiles()) {
            for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                for (declaration in info.declarations) {
                    if (declaration !is CjTypeStatement) continue
                    if (declaration is CjEnum) continue  // 枚举由 generateSyntheticEnums 处理

                    val declName = declaration.nameAsName ?: continue
                    if (declName != name) continue

                    try {
                        val classInfo = CjClassInfo(declaration, declaration.getClassKind())
                        val descriptor = LazyClassDescriptor(ctx, thisDescriptor, name, classInfo, false)
                        result.add(descriptor)
                    } catch (e: Exception) {
                        LOG.debug("解析宏生成的类 $name 失败", e)
                    }
                }
            }
        }
    }

    override fun generateSyntheticEnums(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<EnumDescriptor>
    ) {
        val project = getProject(thisDescriptor) ?: return
        val expansionService = MacroPsiExpansionService.getInstance(project)
        if (!expansionService.isEnabled()) return

        for (sourceFile in declarationProvider.getPackageFiles()) {
            for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                for (declaration in info.declarations) {
                    if (declaration !is CjEnum) continue

                    val declName = declaration.nameAsName ?: continue
                    if (declName != name) continue

                    try {
                        val classInfo = CjClassInfo(declaration, ClassKind.ENUM)
                        val descriptor = LazyEnumDescriptor(ctx, thisDescriptor, name, classInfo)
                        result.add(descriptor)
                    } catch (e: Exception) {
                        LOG.debug("解析宏生成的枚举 $name 失败", e)
                    }
                }
            }
        }
    }

    // =========================================================================
    // 类成员级别：注入宏生成的成员声明
    // =========================================================================

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        // TODO: 类成员级别的宏展开暂不支持（需要类内宏展开的缓存机制）
        return emptyList()
    }

    override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> {
        return emptyList()
    }

    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        return emptyList()
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    /**
     * 从描述符获取 IntelliJ Project 实例
     */
    private fun getProject(descriptor: DeclarationDescriptor): Project? {
        return try {
            DescriptorUtils.getContainingModule(descriptor).projectDescriptor.project
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取 CjTypeStatement 的 ClassKind
     */
    private fun CjTypeStatement.getClassKind(): ClassKind {
        return when (this) {
            is CjInterface -> ClassKind.INTERFACE
            is CjEnum -> ClassKind.ENUM
            is CjStruct -> ClassKind.CLASS
            else -> ClassKind.CLASS
        }
    }
}
