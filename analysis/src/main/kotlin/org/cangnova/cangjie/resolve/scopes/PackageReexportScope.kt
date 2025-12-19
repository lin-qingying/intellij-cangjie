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
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjImportDirectiveItem
import org.cangnova.cangjie.resolve.importVisibility
import org.cangnova.cangjie.resolve.isReexport
import org.cangnova.cangjie.stubindex.CangJieImportFqNameForPackageNameIndex
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
 * - **属性**: 顶层属性
 * - **宏**: 宏定义
 *
 * ## 可见性规则
 *
 * | 修饰符 | 可见范围 |
 * |--------|----------|
 * | public | 任何位置 |
 * | protected | 同一模块内 |
 * | internal | 同一包及子包 |
 * | private | 仅同一文件（不是重导出）|
 *
 * ## 此作用域负责
 *
 * 1. 收集包中所有文件的重导出声明
 * 2. 根据访问位置检查可见性
 * 3. 提供类型、函数、变量、属性、宏的查找方法
 *
 * @property packageFqName 包的完全限定名
 * @property project IntelliJ 项目
 * @property moduleDescriptor 模块描述符
 * @property fromPackage 访问来源的包（用于可见性检查）
 *
 * @see ReexportedDeclaration 重导出声明的包装类
 * @see LazyExplicitImportScope 显式导入作用域（不处理重导出可见性）
 */
class PackageReexportScope(
    private val packageFqName: FqName,
    private val project: Project,
    private val moduleDescriptor: ModuleDescriptor,
    private val fromPackage: PackageFragmentDescriptor?
) : MemberScopeImpl() {

    /**
     * 懒加载的重导出导入指令集合
     *
     * 从包索引中获取当前包的所有导入指令，然后过滤出重导出的部分
     */
    private val reexportImports: Collection<CjImportDirectiveItem> by lazy {
        val scope = GlobalSearchScope.allScope(project)
        CangJieImportFqNameForPackageNameIndex[packageFqName.asString(), project, scope]
            .filter { it.isReexport }
    }

    /**
     * 懒加载的重导出声明缓存
     *
     * Key: 声明名称
     * Value: 重导出声明列表
     */
    private val reexportedDeclarations: Map<Name, List<ReexportedDeclaration>> by lazy {
        buildReexportedDeclarationsMap()
    }

    /**
     * 构建重导出声明映射
     */
    private fun buildReexportedDeclarationsMap(): Map<Name, MutableList<ReexportedDeclaration>> {
        val result = mutableMapOf<Name, MutableList<ReexportedDeclaration>>()

        for (importDirective in reexportImports) {
            val reexported = resolveImportToReexportedDeclarations(importDirective)
            for (decl in reexported) {
                result.getOrPut(decl.effectiveName) { mutableListOf() }.add(decl)
            }
        }

        return result
    }

    /**
     * 将导入指令解析为重导出声明
     */
    private fun resolveImportToReexportedDeclarations(
        importDirective: CjImportDirectiveItem
    ): List<ReexportedDeclaration> {
        val importedFqName = importDirective.importedFqName ?: return emptyList()
        val visibility = importDirective.importVisibility
        val sourceFile = importDirective.containingFile as? org.cangnova.cangjie.psi.CjFile ?: return emptyList()

        // 获取源包片段
        val sourcePackage = moduleDescriptor.getPackage(sourceFile.packageFqName)
            .fragments.firstOrNull() ?: return emptyList()

        // 解析导入的目标
        val parentFqName = importedFqName.parent()
        val importedName = importedFqName.shortName()

        // 获取目标包
        val targetPackage = moduleDescriptor.getPackage(parentFqName)
        if (targetPackage.isEmpty()) {
            return emptyList()
        }

        // 检查是否导入的是包本身（包不能被重导出）
        val childPackage = moduleDescriptor.getPackage(importedFqName)
        if (!childPackage.isEmpty()) {
            // 这是一个包，不应该被重导出
            // 错误检查应该在其他地方处理
            return emptyList()
        }

        // 查找导入的声明
        val aliasName = importDirective.aliasName?.let { Name.identifier(it) }
        val descriptors = mutableListOf<ReexportedDeclaration>()

        // 查找类型
        targetPackage.memberScope.getContributedClassifier(
            importedName,
            NoLookupLocation.FROM_REEXPORT
        )?.let { classifier ->
            descriptors.add(
                ReexportedDeclaration(
                    originalDescriptor = classifier,
                    visibility = visibility,
                    sourceFile = sourceFile,
                    sourcePackage = sourcePackage,
                    importDirective = importDirective,
                    aliasName = aliasName
                )
            )
        }

        // 查找函数
        targetPackage.memberScope.getContributedFunctions(
            importedName,
            NoLookupLocation.FROM_REEXPORT
        ).forEach { function ->
            descriptors.add(
                ReexportedDeclaration(
                    originalDescriptor = function,
                    visibility = visibility,
                    sourceFile = sourceFile,
                    sourcePackage = sourcePackage,
                    importDirective = importDirective,
                    aliasName = aliasName
                )
            )
        }

        // 查找变量
        targetPackage.memberScope.getContributedVariables(
            importedName,
            NoLookupLocation.FROM_REEXPORT
        ).forEach { variable ->
            descriptors.add(
                ReexportedDeclaration(
                    originalDescriptor = variable,
                    visibility = visibility,
                    sourceFile = sourceFile,
                    sourcePackage = sourcePackage,
                    importDirective = importDirective,
                    aliasName = aliasName
                )
            )
        }

        // 查找属性
        targetPackage.memberScope.getContributedPropertys(
            importedName,
            NoLookupLocation.FROM_REEXPORT
        ).forEach { property ->
            descriptors.add(
                ReexportedDeclaration(
                    originalDescriptor = property,
                    visibility = visibility,
                    sourceFile = sourceFile,
                    sourcePackage = sourcePackage,
                    importDirective = importDirective,
                    aliasName = aliasName
                )
            )
        }

        // 查找宏
        targetPackage.memberScope.getContributedMacros(
            importedName,
            NoLookupLocation.FROM_REEXPORT
        ).forEach { macro ->
            descriptors.add(
                ReexportedDeclaration(
                    originalDescriptor = macro,
                    visibility = visibility,
                    sourceFile = sourceFile,
                    sourcePackage = sourcePackage,
                    importDirective = importDirective,
                    aliasName = aliasName
                )
            )
        }

        return descriptors
    }

    /**
     * 获取对访问者可见的重导出声明
     */
    private fun getVisibleReexports(name: Name): List<ReexportedDeclaration> {
        return reexportedDeclarations[name]?.filter { reexport ->
            reexport.isVisibleFrom(fromPackage, fromPackage?.containingDeclaration as? ModuleDescriptor)
        } ?: emptyList()
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return getVisibleReexports(name)
            .mapNotNull { it.originalDescriptor as? ClassifierDescriptor }
            .firstOrNull()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return getVisibleReexports(name)
            .mapNotNull { it.originalDescriptor as? SimpleFunctionDescriptor }
    }

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return getVisibleReexports(name)
            .mapNotNull { it.originalDescriptor as? VariableDescriptor }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return getVisibleReexports(name)
            .mapNotNull { it.originalDescriptor as? PropertyDescriptor }
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<org.cangnova.cangjie.descriptors.macro.MacroDescriptor> {
        return getVisibleReexports(name)
            .mapNotNull { it.originalDescriptor as? org.cangnova.cangjie.descriptors.macro.MacroDescriptor }
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return reexportedDeclarations.values
            .flatten()
            .filter { reexport ->
                nameFilter(reexport.effectiveName) &&
                        reexport.isVisibleFrom(fromPackage, fromPackage?.containingDeclaration as? ModuleDescriptor)
            }
            .map { it.originalDescriptor }
            .filter { descriptor ->
                when {
                    kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK) && descriptor is ClassifierDescriptor -> true
                    kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK) && descriptor is FunctionDescriptor -> true
                    kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK) && descriptor is VariableDescriptor -> true
                    kindFilter.acceptsKinds(DescriptorKindFilter.PROPERTYS_MASK) && descriptor is PropertyDescriptor -> true
//                    kindFilter.acceptsKinds(DescriptorKindFilter.MACROS_MASK) && descriptor is org.cangnova.cangjie.descriptors.macro.MacroDescriptor -> true
                    else -> false
                }
            }
    }

    override val functionNames: Set<Name>
        get(){
            return reexportedDeclarations.entries
                .filter { (_, decls) -> decls.any { it.originalDescriptor is FunctionDescriptor } }
                .map { it.key }
                .toSet()
        }


    override val variableNames : Set<Name>   get() {
        return reexportedDeclarations.entries
            .filter { (_, decls) -> decls.any { it.originalDescriptor is VariableDescriptor } }
            .map { it.key }
            .toSet()
    }

    override val classifierNames : Set<Name>    get(){
        return reexportedDeclarations.entries
            .filter { (_, decls) -> decls.any { it.originalDescriptor is ClassifierDescriptor } }
            .map { it.key }
            .toSet()
    }

    override val propertyNames: Set<Name>
        get() {
            return reexportedDeclarations.entries
                .filter { (_, decls) -> decls.any { it.originalDescriptor is PropertyDescriptor } }
                .map { it.key }
                .toSet()
        }
}
