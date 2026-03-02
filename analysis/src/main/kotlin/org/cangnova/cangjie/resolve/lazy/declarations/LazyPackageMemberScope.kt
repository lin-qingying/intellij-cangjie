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

package org.cangnova.cangjie.resolve.lazy.declarations

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.incremental.record
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.resolve.isReexport
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.LexicalScope


/**
 * 懒加载的包成员作用域
 *
 * 负责包级别的成员解析，包括：
 * 1. 直接声明的包成员
 * 2. 通过 public/protected/internal import 重导出的成员
 *
 * 重导出机制与仓颉编译器一致：重导出的声明作为包的成员之一，
 * 通过 getNonDeclaredXXX 方法集成到包的作用域中。
 */
class LazyPackageMemberScope(
    private val resolveSession: ResolveSession,
    declarationProvider: PackageMemberDeclarationProvider,
    thisPackage: PackageFragmentDescriptor
) : AbstractLazyMemberScope<PackageFragmentDescriptor, PackageMemberDeclarationProvider>(
    resolveSession,
    declarationProvider,
    thisPackage,
    resolveSession.trace
) {
    /**
     * 懒加载的重导出导入指令集合
     *
     * 从当前包的所有文件中收集 public/protected/internal import 语句
     */
    private val reexportImports: Collection<CjImportItem> by lazy {
        collectReexportImports()
    }

    /**
     * 从当前包的所有文件收集重导出导入指令
     */
    private fun collectReexportImports(): List<CjImportItem> {
        val result = mutableListOf<CjImportItem>()
        val packageFiles = declarationProvider.getPackageFiles()

        for (file in packageFiles) {
            for (importDirective in file.importDirectives) {
                for (importItem in importDirective.importItems) {
                    if (importItem.isReexport) {
                        result.add(importItem)
                    }
                }
            }
        }

        return result
    }

    /**
     * 解析重导出的导入目标，返回实际的声明描述符
     */
    private fun resolveReexportTarget(importDirective: CjImportItem): List<DeclarationDescriptor> {
        val importedFqName = importDirective.importedFqName ?: return emptyList()

        // 解析导入的目标
        val parentFqName = importedFqName.parent()
        val importedName = importedFqName.shortName()

        // 获取目标包
        val moduleDescriptor = thisDescriptor.containingDeclaration
        val targetPackage = moduleDescriptor.getPackage(parentFqName)
        if (targetPackage.isEmpty()) return emptyList()

        // 检查是否导入的是包本身（包不能被重导出）
        val childPackage = moduleDescriptor.getPackage(importedFqName)
        if (!childPackage.isEmpty()) return emptyList()

        // 从目标包查找所有类型的声明
        val result = mutableListOf<DeclarationDescriptor>()

        // 查找分类器
        targetPackage.memberScope.getContributedClassifier(importedName, NoLookupLocation.FROM_REEXPORT)?.let {
            result.add(it)
        }

        // 查找函数
        result.addAll(targetPackage.memberScope.getContributedFunctions(importedName, NoLookupLocation.FROM_REEXPORT))

        // 查找变量
        result.addAll(targetPackage.memberScope.getContributedVariables(importedName, NoLookupLocation.FROM_REEXPORT))

        // 查找属性
        result.addAll(targetPackage.memberScope.getContributedPropertys(importedName, NoLookupLocation.FROM_REEXPORT))

        // 查找宏
        result.addAll(targetPackage.memberScope.getContributedMacros(importedName, NoLookupLocation.FROM_REEXPORT))

        return result
    }



    override fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration) =
        resolveSession.fileScopeProvider.getFileResolutionScope(declaration.getContainingCjFile())

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {
        val syntheticFunctions = mutableSetOf<SimpleFunctionDescriptor>()
        resolveSession.syntheticResolveExtension.generateSyntheticFunctions(
            thisDescriptor, name, declarationProvider, syntheticFunctions
        )
        result.addAll(syntheticFunctions)
    }

    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassAndEnumDescriptor>) {
        val syntheticClasses = mutableSetOf<ClassDescriptor>()
        resolveSession.syntheticResolveExtension.generateSyntheticTopLevelClasses(
            thisDescriptor, name, c, declarationProvider, syntheticClasses
        )
        result.addAll(syntheticClasses)

        val syntheticEnums = mutableSetOf<EnumDescriptor>()
        resolveSession.syntheticResolveExtension.generateSyntheticEnums(
            thisDescriptor, name, c, declarationProvider, syntheticEnums
        )
        result.addAll(syntheticEnums)
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.lookupTracker.record(location, thisDescriptor, name)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val declared = computeDescriptorsFromDeclaredElements(
            kindFilter,
            nameFilter,
            NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
        )

        return appendSyntheticPackageDescriptors(declared, kindFilter, nameFilter)
    }

    /**
     * 将宏合成的包级符号追加到已声明的描述符列表中，
     * 使代码补全全量列表也能包含宏生成的顶层声明。
     */
    private fun appendSyntheticPackageDescriptors(
        declared: Collection<DeclarationDescriptor>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val syntheticNames = resolveSession.syntheticResolveExtension.getSyntheticPackageNames(thisDescriptor, declarationProvider)
        if (syntheticNames.isEmpty()) return declared

        val result = declared.toMutableList()

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            for (name in syntheticNames.functionNames) {
                if (!nameFilter(name)) continue
                val funcs = mutableSetOf<SimpleFunctionDescriptor>()
                getNonDeclaredFunctions(name, funcs)
                result.addAll(funcs)
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            for (name in syntheticNames.variableNames) {
                if (!nameFilter(name)) continue
                val vars = mutableSetOf<VariableDescriptor>()
                getNonDeclaredVariables(name, vars)
                result.addAll(vars)
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            for (name in syntheticNames.classifierNames) {
                if (!nameFilter(name)) continue
                val classifiers = mutableSetOf<ClassAndEnumDescriptor>()
                getNonDeclaredClasses(name, classifiers)
                result.addAll(classifiers)
            }
        }

        return result
    }

    override fun getNonDeclaredMacros(name: Name, result: MutableSet<MacroDescriptor>) {

    }

    override fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope =
        getScopeForMemberDeclarationResolution(declaration)


    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {

    }

    override fun getNonDeclaredVariables(name: Name, result: MutableSet<VariableDescriptor>) {
        val syntheticVariables = mutableSetOf<VariableDescriptor>()
        resolveSession.syntheticResolveExtension.generateSyntheticVariables(
            thisDescriptor, name, declarationProvider, syntheticVariables
        )
        result.addAll(syntheticVariables)
    }

    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? {


        val packageView = thisDescriptor.containingDeclaration.getPackage(thisDescriptor.fqName.child(name))
        return if (packageView.isEmpty()) null else packageView

    }

    // Do not add details here, they may compromise the laziness during debugging
    override fun toString() = "lazy scope for package " + thisDescriptor.name
}
