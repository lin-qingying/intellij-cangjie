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

package org.cangnova.cangjie.resolve.scopes

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.descriptors.ReexportedDeclarationDescriptor
import org.cangnova.cangjie.descriptors.SimpleFunctionDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportDirectiveItem
import org.cangnova.cangjie.resolve.importVisibility
import org.cangnova.cangjie.resolve.isReexport
import org.cangnova.cangjie.utils.Printer

/**
 * 包级重导出作用域
 *
 * 聚合包中所有文件的重导出声明，提供统一的查找接口。
 *
 * 在仓颉语言中，重导出机制允许一个包将其导入的声明重新导出给外部使用：
 * ```cangjie
 * // 在 package foo 中
 * public import std.core.String  // 将 String 重导出为 public
 *
 * // 在 package bar 中
 * import foo.*  // 可以访问到 foo 重导出的 String
 * ```
 *
 * ## 支持的重导出类型
 *
 * - **分类器**: 类、接口、枚举、类型别名
 * - **函数**: 顶层函数
 * - **变量**: 顶层变量
 * - **宏**: 宏定义
 *
 * ## 此作用域负责
 *
 * 1. 收集包中所有文件的重导出声明
 * 2. 将原始声明包装为重导出描述符
 * 3. 提供类型、函数、变量、属性、宏的查找方法
 *
 * 注意：此作用域不负责可见性检查，可见性检查在更上层进行。
 *
 * @property packageFqName 包的完全限定名
 * @property moduleDescriptor 模块描述符
 *
 * @see ReexportedDeclarationDescriptor 重导出声明的包装类
 */
class PackageReexportScope(
    private val packageFqName: FqName,
    private val moduleDescriptor: ModuleDescriptor,
) : MemberScopeImpl() {
    private val project: Project get() = moduleDescriptor.projectDescriptor.project

    /**
     * 懒加载的重导出导入指令集合
     *
     * 从包的 fragments 直接获取源文件中的重导出导入指令，
     * 而不依赖 stub 索引。这种方式更加可靠，因为：
     * 1. 不依赖索引的构建状态
     * 2. 直接从 PSI 获取数据，保证实时性
     * 3. 与仓颉编译器的实现方式一致
     */
    private val reexportImports: Collection<CjImportDirectiveItem> by lazy {
        collectReexportImportsFromFragments()
    }

    /**
     * 从包的所有 fragments 收集重导出导入指令
     */
    private fun collectReexportImportsFromFragments(): List<CjImportDirectiveItem> {
        val packageView = moduleDescriptor.getPackage(packageFqName)
        if (packageView.isEmpty()) return emptyList()

        val result = mutableListOf<CjImportDirectiveItem>()

        for (fragment in packageView.fragments) {
            // 尝试从 declarationProvider 获取源文件
            val declarationProvider = fragment.declarationProvider
            if (declarationProvider is PackageMemberDeclarationProvider) {
                val packageFiles = declarationProvider.getPackageFiles()
                for (file in packageFiles) {
                    collectReexportImportsFromFile(file, result)
                }
            }
        }

        return result
    }

    /**
     * 从单个文件收集重导出导入指令
     */
    private fun collectReexportImportsFromFile(file: CjFile, result: MutableList<CjImportDirectiveItem>) {
        for (importDirective in file.importDirectives) {
            for (importItem in importDirective.items) {
                if (importItem.isReexport) {
                    result.add(importItem)
                }
            }
        }
    }

    /**
     * 通过延迟解析获取指定名称的重导出声明
     *
     * 不预先构建所有声明的映射，而是在需要时才解析
     */
    private fun resolveReexportedDeclarations(name: Name): List<DeclarationDescriptor> {
        val result = mutableListOf<DeclarationDescriptor>()

        for (importDirective in reexportImports) {
            if (importDirective.isAllUnder) {
                // 通配符导入: public import a.*
                // 直接查询目标包中的指定名称
                val wrappedDeclarations = resolveWildcardImportTarget(importDirective, name)
                result.addAll(wrappedDeclarations)
            } else {
                // 命名导入: public import a.B 或 public import a.B as C
                // 检查这个 import 是否匹配查询的名称
                val effectiveName = importDirective.aliasName?.let { Name.identifier(it) }
                    ?: importDirective.importedFqName?.shortName()
                    ?: continue

                if (effectiveName != name) continue

                // 延迟解析：只在需要时才查找目标包的声明并包装
                val wrappedDeclarations = resolveNamedImportTarget(importDirective, name)
                result.addAll(wrappedDeclarations)
            }
        }

        return result
    }

    /**
     * 解析命名导入的目标声明: public import a.B 或 public import a.B as C
     */
    private fun resolveNamedImportTarget(importDirective: CjImportDirectiveItem, effectiveName: Name): List<DeclarationDescriptor> {
        val importedFqName = importDirective.importedFqName ?: return emptyList()

        // 解析导入的目标
        val parentFqName = importedFqName.parent()
        val importedName = importedFqName.shortName()

        // 获取目标包
        val targetPackage = moduleDescriptor.getPackage(parentFqName)
        if (targetPackage.isEmpty()) return emptyList()

        // 检查是否导入的是包本身（包不能被重导出）
        val childPackage = moduleDescriptor.getPackage(importedFqName)
        if (!childPackage.isEmpty()) return emptyList()

        // 获取重导出的可见性和源文件
        val reexportVisibility = importDirective.importVisibility
        val sourceFile = importDirective.containingFile as? CjFile ?: return emptyList()
        val aliasName = importDirective.aliasName?.let { Name.identifier(it) }

        // 从目标包查找所有类型的声明并包装
        return wrapDeclarationsFromPackage(targetPackage, importedName, reexportVisibility, sourceFile, importDirective, aliasName)
    }

    /**
     * 解析通配符导入的目标声明: public import a.*
     */
    private fun resolveWildcardImportTarget(importDirective: CjImportDirectiveItem, requestedName: Name): List<DeclarationDescriptor> {
        val importedFqName = importDirective.importedFqName ?: return emptyList()

        // 对于通配符导入，importedFqName 是包名
        val targetPackage = moduleDescriptor.getPackage(importedFqName)
        if (targetPackage.isEmpty()) return emptyList()

        // 获取重导出的可见性和源文件
        val reexportVisibility = importDirective.importVisibility
        val sourceFile = importDirective.containingFile as? CjFile ?: return emptyList()

        // 从目标包查找请求的名称，不使用别名（通配符导入不支持别名）
        return wrapDeclarationsFromPackage(targetPackage, requestedName, reexportVisibility, sourceFile, importDirective, null)
    }

    /**
     * 从目标包中查找指定名称的声明并包装为重导出描述符
     */
    private fun wrapDeclarationsFromPackage(
        targetPackage: org.cangnova.cangjie.descriptors.PackageViewDescriptor,
        searchName: Name,
        reexportVisibility: org.cangnova.cangjie.descriptors.DescriptorVisibility,
        sourceFile: CjFile,
        importDirective: CjImportDirectiveItem,
        aliasName: Name?
    ): List<DeclarationDescriptor> {
        val result = mutableListOf<DeclarationDescriptor>()

        // 查找并包装分类器
        targetPackage.memberScope.getContributedClassifier(searchName, NoLookupLocation.FROM_REEXPORT)?.let { classifier ->
            result.add(
                ReexportedDeclarationDescriptor.Classifier(
                    classifier,
                    reexportVisibility,
                    sourceFile,
                    importDirective,
                    aliasName
                )
            )
        }

        // 查找并包装函数
        targetPackage.memberScope.getContributedFunctions(searchName, NoLookupLocation.FROM_REEXPORT).forEach { function ->

                result.add(
                    ReexportedDeclarationDescriptor.Function(
                        function,
                        reexportVisibility,
                        sourceFile,
                        importDirective,
                        aliasName
                    )
                )

        }

        // 查找并包装变量
        targetPackage.memberScope.getContributedVariables(searchName, NoLookupLocation.FROM_REEXPORT).forEach { variable ->
            result.add(
                ReexportedDeclarationDescriptor.Variable(
                    variable,
                    reexportVisibility,
                    sourceFile,
                    importDirective,
                    aliasName
                )
            )
        }

        // 查找并包装属性
        targetPackage.memberScope.getContributedPropertys(searchName, NoLookupLocation.FROM_REEXPORT).forEach { property ->
            result.add(
                ReexportedDeclarationDescriptor.Property(
                    property,
                    reexportVisibility,
                    sourceFile,
                    importDirective,
                    aliasName
                )
            )
        }

        // 查找并包装宏
        targetPackage.memberScope.getContributedMacros(searchName, NoLookupLocation.FROM_REEXPORT).forEach { macro ->
            result.add(
                ReexportedDeclarationDescriptor.Macro(
                    macro,
                    reexportVisibility,
                    sourceFile,
                    importDirective,
                    aliasName
                )
            )
        }

        return result
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return resolveReexportedDeclarations(name)
            .filterIsInstance<ClassifierDescriptor>()
            .firstOrNull()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return resolveReexportedDeclarations(name)
            .filterIsInstance<SimpleFunctionDescriptor>()
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()
        p.println("packageFqName = $packageFqName")
        p.popIndent()
        p.println("}")
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return resolveReexportedDeclarations(name)
            .filterIsInstance<VariableDescriptor>()
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return resolveReexportedDeclarations(name)
            .filterIsInstance<PropertyDescriptor>()
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return resolveReexportedDeclarations(name)
            .filterIsInstance<MacroDescriptor>()
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val result = mutableListOf<DeclarationDescriptor>()

        for (importDirective in reexportImports) {
            if (importDirective.isAllUnder) {
                // 通配符导入: public import a.*
                // 需要枚举目标包的所有成员
                val importedFqName = importDirective.importedFqName ?: continue
                val targetPackage = moduleDescriptor.getPackage(importedFqName)
                if (targetPackage.isEmpty()) continue

                val reexportVisibility = importDirective.importVisibility
                val sourceFile = importDirective.containingFile as? CjFile ?: continue

                // 获取目标包中所有符合条件的声明
                val targetDescriptors = targetPackage.memberScope.getContributedDescriptors(kindFilter, nameFilter)

                // 包装每个声明
                for (descriptor in targetDescriptors) {
                    when (descriptor) {
                        is ClassifierDescriptor -> result.add(
                            ReexportedDeclarationDescriptor.Classifier(
                                descriptor, reexportVisibility, sourceFile, importDirective, null
                            )
                        )
                        is SimpleFunctionDescriptor -> result.add(
                            ReexportedDeclarationDescriptor.Function(
                                descriptor, reexportVisibility, sourceFile, importDirective, null
                            )
                        )
                        is VariableDescriptor -> result.add(
                            ReexportedDeclarationDescriptor.Variable(
                                descriptor, reexportVisibility, sourceFile, importDirective, null
                            )
                        )
                        is PropertyDescriptor -> result.add(
                            ReexportedDeclarationDescriptor.Property(
                                descriptor, reexportVisibility, sourceFile, importDirective, null
                            )
                        )
                        is MacroDescriptor -> result.add(
                            ReexportedDeclarationDescriptor.Macro(
                                descriptor, reexportVisibility, sourceFile, importDirective, null
                            )
                        )
                    }
                }
            } else {
                // 命名导入: public import a.B 或 public import a.B as C
                // 获取有效名称
                val effectiveName = importDirective.aliasName?.let { Name.identifier(it) }
                    ?: importDirective.importedFqName?.shortName()
                    ?: continue

                // 应用名称过滤器
                if (!nameFilter(effectiveName)) continue

                // 解析目标声明并包装
                val wrappedDeclarations = resolveNamedImportTarget(importDirective, effectiveName)

                // 应用类型过滤器
                wrappedDeclarations.filter { descriptor ->
                    when {
                        kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK) && descriptor is ClassifierDescriptor -> true
                        kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK) && descriptor is FunctionDescriptor -> true
                        kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK) && descriptor is VariableDescriptor -> true
                        kindFilter.acceptsKinds(DescriptorKindFilter.PROPERTYS_MASK) && descriptor is PropertyDescriptor -> true
                        else -> false
                    }
                }.let { result.addAll(it) }
            }
        }

        return result
    }

    override val functionNames: Set<Name>
        get() = collectEffectiveNames()

    override val variableNames: Set<Name>
        get() = collectEffectiveNames()

    override val classifierNames: Set<Name>
        get() = collectEffectiveNames()

    override val propertyNames: Set<Name>
        get() = collectEffectiveNames()

    /**
     * 收集所有重导出的有效名称
     */
    private fun collectEffectiveNames(): Set<Name> {
        val names = mutableSetOf<Name>()

        for (importDirective in reexportImports) {
            if (importDirective.isAllUnder) {
                // 通配符导入: public import a.*
                // 需要枚举目标包的所有成员名称
                val importedFqName = importDirective.importedFqName ?: continue
                val targetPackage = moduleDescriptor.getPackage(importedFqName)
                if (targetPackage.isEmpty()) continue

                // 收集目标包的所有名称
                names.addAll(targetPackage.memberScope.classifierNames ?: emptySet())
                names.addAll(targetPackage.memberScope.functionNames)
                names.addAll(targetPackage.memberScope.variableNames)
                names.addAll(targetPackage.memberScope.propertyNames)
            } else {
                // 命名导入: public import a.B 或 public import a.B as C
                val effectiveName = importDirective.aliasName?.let { Name.identifier(it) }
                    ?: importDirective.importedFqName?.shortName()
                    ?: continue
                names.add(effectiveName)
            }
        }

        return names
    }
}
