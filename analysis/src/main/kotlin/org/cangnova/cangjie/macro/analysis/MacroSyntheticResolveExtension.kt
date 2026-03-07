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
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.data.CjClassInfo
import org.cangnova.cangjie.macro.psi.MacroPsiExpansionService
import org.cangnova.cangjie.macro.psi.MacroSourceInfo
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension.PackageSyntheticNames
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyEnumDescriptor
import org.cangnova.cangjie.resolve.source.getPsi

/**
 * 宏展开合成解析扩展
 *
 * 将宏展开生成的声明注入到 IDE 的解析管线中。
 * 通过 [MacroPsiExpansionService] 获取展开后的 PSI 文件副本，
 * 从中提取由宏生成的声明（带有 [MacroSourceInfo] 标记），
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
            val expandedFile = expansionService.getExpandedFile(sourceFile) ?: continue

            for (declaration in expandedFile.declarations) {
                if (!isMacroGenerated(declaration)) continue

                val name = (declaration as? CjNamedDeclaration)?.nameAsName ?: continue

                when (declaration) {
                    is CjTypeStatement -> classifierNames.add(name)
                    is CjNamedFunction -> functionNames.add(name)
                    is CjProperty -> variableNames.add(name)
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
            val expandedFile = expansionService.getExpandedFile(sourceFile) ?: continue

            for (declaration in expandedFile.declarations) {
                if (declaration !is CjTypeStatement) continue
                if (declaration is CjEnum) continue  // 枚举由 generateSyntheticEnums 处理
                if (!isMacroGenerated(declaration)) continue

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
            val expandedFile = expansionService.getExpandedFile(sourceFile) ?: continue

            for (declaration in expandedFile.declarations) {
                if (declaration !is CjEnum) continue
                if (!isMacroGenerated(declaration)) continue

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

    // =========================================================================
    // 类成员级别：注入宏生成的成员声明
    // =========================================================================

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        return extractMacroMemberNames(thisDescriptor) { it is CjNamedFunction }
    }

    override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> {
        return extractMacroMemberNames(thisDescriptor) { it is CjProperty }
    }

    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        return extractMacroMemberNames(thisDescriptor) { it is CjTypeStatement }
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
     * 判断 PSI 元素是否由宏展开生成
     */
    private fun isMacroGenerated(element: CjDeclaration): Boolean {
        return element.getUserData(MacroSourceInfo.KEY) != null
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

    /**
     * 从展开后的 PSI 提取宏生成的类成员名称
     */
    private fun extractMacroMemberNames(
        classDescriptor: ClassDescriptor,
        filter: (CjDeclaration) -> Boolean
    ): List<Name> {
        val project = getProject(classDescriptor) ?: return emptyList()
        val expansionService = MacroPsiExpansionService.getInstance(project)
        if (!expansionService.isEnabled()) return emptyList()

        val sourcePsi = (classDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() as? CjTypeStatement ?: return emptyList()
        val sourceFile = sourcePsi.containingFile as? CjFile ?: return emptyList()
        val expandedFile = expansionService.getExpandedFile(sourceFile) ?: return emptyList()

        val expandedClass = findCorrespondingClass(expandedFile, sourcePsi) ?: return emptyList()

        val names = mutableListOf<Name>()
        for (declaration in expandedClass.declarations) {
            if (!filter(declaration)) continue
            if (!isMacroGenerated(declaration)) continue

            val name = (declaration as? CjNamedDeclaration)?.nameAsName ?: continue
            names.add(name)
        }

        return names
    }

    /**
     * 在展开后的文件中查找与原始类对应的类声明
     */
    private fun findCorrespondingClass(expandedFile: CjFile, sourceClass: CjTypeStatement): CjTypeStatement? {
        val sourceName = sourceClass.name ?: return null
        val sourceOffset = sourceClass.textOffset

        val candidates = PsiTreeUtil.findChildrenOfType(expandedFile, CjTypeStatement::class.java)

        return candidates.filter { it.name == sourceName }
            .minByOrNull { kotlin.math.abs(it.textOffset - sourceOffset) }
    }
}
