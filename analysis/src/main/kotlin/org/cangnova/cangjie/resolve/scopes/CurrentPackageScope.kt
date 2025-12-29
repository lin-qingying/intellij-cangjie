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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.Printer

/**
 * 当前包作用域
 *
 * 仅负责当前包的直接成员访问，不处理重导出逻辑。
 * 重导出逻辑由 LazyPackageMemberScope 在 getNonDeclaredXXX 方法中处理。
 *
 * ## 职责
 *
 * - 提供当前包中直接声明的类型、函数、变量等的访问
 * - 根据可见性规则过滤成员
 * - 处理别名导入导致的名称排除
 *
 * @property packageView 包视图描述符
 * @property packageFragment 包片段描述符（用于可见性检查）
 * @property aliasImportNames 别名导入的完全限定名列表（用于排除）
 * @property fromDescriptor 访问来源描述符（用于可见性检查）
 * @property filteringKind 过滤类型（可见/不可见类）
 * @property parentScope 父作用域
 * @property languageVersionSettings 语言版本设置
 */
class CurrentPackageScope(
    private val packageView: PackageViewDescriptor,
    private val packageFragment: PackageFragmentDescriptor,
    private val aliasImportNames: Collection<FqName>,
    private val fromDescriptor: DeclarationDescriptor,
    private val filteringKind: FilteringKind,
    override val parent: ImportingScope,
    private val languageVersionSettings: LanguageVersionSettings
) : ImportingScope {

    /** 包的成员作用域 */
    private val scope = packageView.memberScope

    /** 包中所有名称的缓存 */
    private val names by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scope.computeAllNames()?.let(::ObjectOpenHashSet)
    }

    /** 包的完全限定名 */
    private val packageName = packageView.fqName

    /** 由于别名导入而需要排除的名称 */
    private val excludedNames = aliasImportNames.mapNotNull {
        if (it.parent() == packageName) it.shortName() else null
    }

    override fun getContributedPackage(name: Name): Nothing? = null

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        if (name in excludedNames) return null

        // 只查找本包成员
        val classifier = scope.getContributedClassifier(name, location) ?: return null

        val visible = DescriptorVisibilityUtils.isVisibleIgnoringReceiver(
            classifier as DeclarationDescriptorWithVisibility,
            fromDescriptor,
            languageVersionSettings
        )
        return classifier.takeIf {
            filteringKind == if (visible) FilteringKind.VISIBLE_CLASSES else FilteringKind.INVISIBLE_CLASSES
        }
    }

    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        if (name in excludedNames) return emptyList()
        return scope.getContributedClassifiers(name, location)
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<VariableDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return emptyList()
        if (name in excludedNames) return emptyList()
        return scope.getContributedVariables(name, location)
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return emptyList()
        if (name in excludedNames) return emptyList()
        return scope.getContributedPropertys(name, location)
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return emptyList()
        if (name in excludedNames) return emptyList()
        return scope.getContributedFunctions(name, location)
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return emptyList()
        if (name in excludedNames) return emptyList()
        return scope.getContributedMacros(name, location)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        if (filteringKind == FilteringKind.INVISIBLE_CLASSES) return emptyList()

        return scope.getContributedDescriptors(
            kindFilter.withoutKinds(DescriptorKindFilter.PACKAGES_MASK)
        ) { name -> name !in excludedNames && nameFilter(name) }
            .filter { it !is PackageViewDescriptor } // 子包不能通过短名称访问
    }

    override fun computeImportedNames(): Set<Name>? {
        return packageView.memberScope.computeAllNames()
    }

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return names?.let { name !in it } == true
    }

    override fun toString() = "CurrentPackageScope(${packageView.fqName}, ${filteringKind.name})"

    override fun printStructure(p: Printer) {
        p.println(this.toString())
    }

    /**
     * 过滤类型
     *
     * 用于区分可见和不可见的类，这是仓颉符号解析的一部分策略。
     */
    enum class FilteringKind {
        /** 只包含可见的类 */
        VISIBLE_CLASSES,

        /** 只包含不可见的类（用于诊断） */
        INVISIBLE_CLASSES
    }
}
