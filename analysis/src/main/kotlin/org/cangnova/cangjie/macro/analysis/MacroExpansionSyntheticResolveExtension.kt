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
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.registry.Registry

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension.PackageSyntheticNames
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyEnumDescriptor
import org.cangnova.cangjie.resolve.source.getPsi

// PackageMemberDeclarationProvider 与 ClassMemberDeclarationProvider 来自不同包，用别名区分
private typealias PkgDeclProvider = org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider

/**
 * 宏展开合成解析扩展
 *
 * 实现 [SyntheticResolveExtension]，将宏展开产生的描述符注入到两个层级的作用域：
 *
 * 1. **包级别** (`generateSyntheticXxx(PackageFragmentDescriptor, ...)`):
 *    处理顶层注解宏，如 `@abc(c)` 生成 `class c / enum c / func f() / let x` 等顶层声明。
 *
 * 2. **类成员级别** (`generateSyntheticMethods/Properties`):
 *    处理类上的注解宏，如 `@Derive(Eq)` 生成 `equals()` 方法。
 *
 * 通过 Registry key `cangjie.macro.expansion.analysis.enabled` 控制开关（默认关闭）。
 *
 * @deprecated 已废弃，随时可删除。
 * 新架构使用基于磁盘的展开文件，展开后的 .cj 文件作为源码根注册到 IntelliJ，
 * 声明通过 Stub 索引自动发现，无需通过 SyntheticResolveExtension 手动注入。
 * @see org.cangnova.cangjie.macro.expanded.MacroExpandedFileManager
 */
@Deprecated("已废弃：新架构使用 MacroExpandedFileManager 基于磁盘的展开文件，Stub 索引自动处理声明注入")
internal class MacroExpansionSyntheticResolveExtension : SyntheticResolveExtension {

    private val log = Logger.getInstance(MacroExpansionSyntheticResolveExtension::class.java)

    private val packageRecursionGuard = RecursionManager.createGuard<PackageFragmentDescriptor>(
        "MacroExpansionSyntheticResolve.package"
    )

    private val classRecursionGuard = RecursionManager.createGuard<ClassDescriptor>(
        "MacroExpansionSyntheticResolve.class"
    )

    private fun isEnabled(): Boolean = try {
        Registry.`is`("cangjie.macro.expansion.analysis.enabled", false)
    } catch (_: Exception) {
        false
    }

    // -------------------------------------------------------------------------
    // 包级别：处理顶层宏生成的声明
    // -------------------------------------------------------------------------

    /**
     * 获取包级宏展开描述符。
     *
     * [PackageFragmentDescriptor.source] 固定为 NO_SOURCE，无法通过 getPsi() 获取文件，
     * 改为从 [declarationProvider] 取第一个源文件。
     */
    private fun getPackageDescriptors(
        thisDescriptor: PackageFragmentDescriptor,
        declarationProvider: PkgDeclProvider
    ): MacroExpandedDescriptorProvider.FileDescriptors? {
        if (!isEnabled()) return null
        val file = declarationProvider.getPackageFiles().firstOrNull() ?: return null
        val virtualFile = file.virtualFile ?: return null
        val project = file.project

        return packageRecursionGuard.doPreventingRecursion(thisDescriptor, true) {
            try {
                MacroExpandedDescriptorProvider.getInstance(project)
                    .getPackageLevelDescriptors(virtualFile, thisDescriptor)
            } catch (e: Exception) {
                log.debug("获取包级宏展开描述符失败", e)
                null
            }
        }
    }

    override fun generateSyntheticTopLevelClasses(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PkgDeclProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        val classInfos = getPackageDescriptors(thisDescriptor, declarationProvider)
            ?.classes?.get(name) ?: return
        for (macroTypeInfo in classInfos) {
            try {
                result.add(LazyClassDescriptor(ctx, thisDescriptor, name, macroTypeInfo.typeInfo, false))
            } catch (e: Exception) {
                log.debug("创建宏展开 LazyClassDescriptor 失败: $name", e)
            }
        }
    }

    override fun generateSyntheticEnums(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        ctx: LazyClassContext,
        declarationProvider: PkgDeclProvider,
        result: MutableSet<EnumDescriptor>
    ) {
        val enumInfos = getPackageDescriptors(thisDescriptor, declarationProvider)
            ?.enums?.get(name) ?: return
        for (macroTypeInfo in enumInfos) {
            try {
                result.add(LazyEnumDescriptor(ctx, thisDescriptor, name, macroTypeInfo.typeInfo))
            } catch (e: Exception) {
                log.debug("创建宏展开 LazyEnumDescriptor 失败: $name", e)
            }
        }
    }

    override fun generateSyntheticFunctions(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        declarationProvider: PkgDeclProvider,
        result: MutableSet<SimpleFunctionDescriptor>
    ) {
        getPackageDescriptors(thisDescriptor, declarationProvider)
            ?.functions?.get(name)?.let { result.addAll(it) }
    }

    override fun generateSyntheticVariables(
        thisDescriptor: PackageFragmentDescriptor, name: Name,
        declarationProvider: PkgDeclProvider,
        result: MutableSet<VariableDescriptor>
    ) {
        getPackageDescriptors(thisDescriptor, declarationProvider)
            ?.variables?.get(name)?.let { result.addAll(it) }
    }

    override fun getSyntheticPackageNames(
        thisDescriptor: PackageFragmentDescriptor,
        declarationProvider: PkgDeclProvider
    ): PackageSyntheticNames {
        val d = getPackageDescriptors(thisDescriptor, declarationProvider) ?: return PackageSyntheticNames.EMPTY
        return PackageSyntheticNames(
            functionNames = d.functionNames,
            variableNames = d.variableNames,
            classifierNames = d.classNames + d.enumNames
        )
    }

    // -------------------------------------------------------------------------
    // 类成员级别：处理类上注解宏生成的方法/属性（@Derive(Eq) → equals()）
    // -------------------------------------------------------------------------

    /**
     * 获取类成员级宏展开描述符。
     *
     * [ClassDescriptor.source] 指向实际 PSI，可通过 getPsi() 获取宿主类所在文件。
     */
    private fun getClassDescriptors(
        thisDescriptor: ClassDescriptor
    ): MacroExpandedDescriptorProvider.FileDescriptors? {
        if (!isEnabled()) return null
        val psi = thisDescriptor.source.getPsi() ?: return null
        val virtualFile = (psi.containingFile as? CjFile)?.virtualFile ?: return null

        return classRecursionGuard.doPreventingRecursion(thisDescriptor, true) {
            try {
                MacroExpandedDescriptorProvider.getInstance(psi.project)
                    .getClassLevelDescriptors(virtualFile, thisDescriptor)
            } catch (e: Exception) {
                log.debug("获取类级宏展开描述符失败", e)
                null
            }
        }
    }

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        getClassDescriptors(thisDescriptor)?.functionNames?.toList() ?: emptyList()

    override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> =
        getClassDescriptors(thisDescriptor)?.propertyNames?.toList() ?: emptyList()

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor, name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        getClassDescriptors(thisDescriptor)?.functions?.get(name)?.let { result.addAll(it) }
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassAndEnumDescriptor, name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) {
        if (thisDescriptor !is ClassDescriptor) return
        getClassDescriptors(thisDescriptor)?.properties?.get(name)?.let { result.addAll(it) }
    }
}