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

package org.cangnova.cangjie.stubindex.resolve

import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.safeNameForLazyResolve
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.CommonProcessors
import com.intellij.util.indexing.FileBasedIndex
import org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider
import org.cangnova.cangjie.descriptors.data.CjClassInfoUtil
import org.cangnova.cangjie.descriptors.data.CjTypeStatementInfo
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.stubindex.CangJieMacroDeclarationFqNameIndex
import org.cangnova.cangjie.stubindex.*
import org.cangnova.cangjie.stubindex.CangJieTopLevelClassByPackageIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelFunctionByPackageIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelFunctionFqNameIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelTypeAliasByPackageIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelVariableByPackageIndex
import org.cangnova.cangjie.utils.isApplicationInternalMode

/**
 * 短名称过滤开关，从 IDE Registry 中懒加载读取。
 * 对应注册表键：`cangjie.indices.short.names.filtering.enabled`
 * 启用后可在类型声明查询时通过短名称缓存提前过滤，减少不必要的索引查询。
 */
private val isShortNameFilteringEnabled: Boolean by lazy { Registry.`is`("cangjie.indices.short.names.filtering.enabled") }


/**
 * 基于 Stub 索引的包成员声明提供者。
 *
 * 实现 [PackageMemberDeclarationProvider]，通过 IntelliJ 的 Stub 索引机制
 * 查询指定包（[fqName]）内的顶层声明，无需解析完整 PSI 树，具有较高的查询性能。
 *
 * 各类声明分别对应不同的 Stub 索引：
 * - 类/对象 → [CangJieTopLevelClassByPackageIndex] / [CangJieFullClassNameIndex]
 * - 函数    → [CangJieTopLevelFunctionByPackageIndex] / [CangJieTopLevelFunctionFqNameIndex]
 * - 变量    → [CangJieTopLevelVariableByPackageIndex] / [CangJieTopLevelVariableFqNameIndex]
 * - 类型别名 → [CangJieTopLevelTypeAliasByPackageIndex] / [CangJieTopLevelTypeAliasFqNameIndex]
 * - 宏      → [CangJieMacroDeclarationByPackageIndex] / [CangJieMacroDeclarationFqNameIndex]
 *
 * @param fqName 当前提供者对应的包完全限定名
 * @param project 当前 IDE 项目实例
 * @param searchScope 声明查询的搜索范围
 */
open class StubBasedPackageMemberDeclarationProvider(
    private val fqName: FqName,
    private val project: Project,
    private val searchScope: GlobalSearchScope
) : PackageMemberDeclarationProvider {

    /**
     * 获取满足过滤条件的所有顶层声明。
     *
     * 根据 [kindFilter] 决定查询哪些索引，仅查询必要的类型，避免无效的索引访问。
     * 每条声明还需通过 [nameFilter] 进行名称过滤。
     *
     * @param kindFilter 声明种类过滤器（类、函数、变量等）
     * @param nameFilter 名称过滤函数，返回 true 表示保留该声明
     * @return 满足条件的声明列表
     */
    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<CjDeclaration> {
        val fqNameAsString = fqName.asString()
        val result = ArrayList<CjDeclaration>()

        // 辅助函数：从指定索引中按包名查询声明，并通过 nameFilter 过滤
        fun addFromIndex(helper: CangJieStringStubIndexHelper<out CjNamedDeclaration>) {
            helper.processElements(fqNameAsString, project, searchScope) {
                if (nameFilter(it.nameAsSafeName)) {
                    result.add(it)
                }
                true
            }
        }

        // 按需查询各类型索引，避免不必要的 IO 开销
        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            addFromIndex(CangJieTopLevelClassByPackageIndex)
            addFromIndex(CangJieTopLevelTypeAliasByPackageIndex)
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            addFromIndex(CangJieTopLevelFunctionByPackageIndex)
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.MACROS_MASK)) {
            addFromIndex(CangJieMacroDeclarationByPackageIndex)
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            addFromIndex(CangJieTopLevelVariableByPackageIndex)
        }

        return result
    }

    /**
     * 按完全限定名查找顶层具名函数声明集合。
     * 在读锁保护下访问 Stub 索引，保证线程安全。
     *
     * @param name 函数名称
     */
    override fun getFunctionDeclarations(name: Name): Collection<CjNamedFunction> = runReadAction {
        CangJieTopLevelFunctionFqNameIndex[childName(name), project, searchScope]
    }

    /**
     * 按完全限定名查找宏声明集合。
     *
     * @param name 宏名称
     */
    override fun getMacroDeclarations(name: Name): Collection<CjMacroDeclaration> = runReadAction {
        CangJieMacroDeclarationFqNameIndex[childName(name), project, searchScope]
    }

    /**
     * 查找当前模块下的所有 main 函数声明。
     *
     * main 函数的索引 key 使用模块级路径（取包名首段），而非完整包路径，
     * 因为 main 函数在仓颉中属于模块级入口，不绑定到具体子包。
     */
    override fun getMainFunctionDeclarations(): Collection<CjMainFunction> = runReadAction {
        CangJieMainFunctionFqNameIndex[moduleChildName(MAIN), project, searchScope]
    }

    /**
     * 构造模块级子名称（取包名首段 + name）。
     *
     * 用于 main 函数等模块级声明的索引 key 生成。
     * 若包名无首段（根包），则直接使用 name 本身。
     *
     * @param name 声明名称
     * @return 模块级完全限定名字符串
     */
    private fun moduleChildName(name: Name): String {
        val firstSegment = fqName.firstSegment() ?: return name.safeNameForLazyResolve().asString()
        return FqName(firstSegment.asString()).child(name.safeNameForLazyResolve()).asString()
    }

    /**
     * 构造当前包下指定名称的完全限定名字符串。
     * 用于各类 Stub 索引的精确查询 key。
     *
     * @param name 声明名称
     * @return 完全限定名字符串，格式为 `<fqName>.<name>`
     */
    private fun childName(name: Name): String {
        return fqName.child(name.safeNameForLazyResolve()).asString()
    }

    /**
     * 按完全限定名查找顶层变量声明集合。
     *
     * @param name 变量名称
     */
    override fun getVariableDeclarations(name: Name): Collection<CjVariable<*>> = runReadAction {
        CangJieTopLevelVariableFqNameIndex[childName(name), project, searchScope]
    }

    /**
     * 顶层属性查询，始终返回空列表。
     *
     * 仓颉中 prop 只能声明在类成员中，不存在顶层属性，因此无需查询索引。
     */
    override fun getPropertyDeclarations(name: Name): Collection<CjProperty> {
        return emptyList()
    }

    /**
     * 按展开短名称查找类型别名声明集合（原始别名）。
     *
     * 与 [getTypeAliasDeclarations] 不同，此方法使用展开短名称索引
     * [CangJieTypeAliasByExpansionShortNameIndex] 进行查询，
     * 适用于需要通过别名目标类型名称反查别名声明的场景。
     *
     * @param name 展开类型的短名称
     */
    override fun getAliasTypeStatementDeclarations(name: Name): Collection<CjTypeAlias> {
        return runReadAction {
            CangJieTypeAliasByExpansionShortNameIndex[name.asString(), project, searchScope]
        }
    }

    /**
     * 按名称查找类型声明信息集合（类、struct、枚举、接口等）。
     *
     * 采用两阶段查询策略以优化性能：
     * 1. **短名称预过滤**（可选）：若启用 [isShortNameFilteringEnabled]，
     *    先查询 [ShortNamesCacheService] 缓存，若名称在项目中根本不存在则直接返回空列表，
     *    避免触发代价更高的全量索引查询。
     * 2. **全类名索引精查**：在读锁保护下通过 [CangJieFullClassNameIndex] 精确查询，
     *    每次迭代检查取消信号以支持用户中断。
     *
     * @param name 类型名称
     * @return 匹配的类型声明信息集合
     */
    override fun getTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<*>> {
        val childName = childName(name)

        // 第一阶段：短名称缓存预过滤，拦截项目中根本不存在的类名，避免无效索引查询
        if (isShortNameFilteringEnabled && !name.isSpecial) {
            val shortNames = ShortNamesCacheService.getInstance(project).getShortNameCandidates(name.asString())
            if (childName !in shortNames) {
                return emptyList()
            }
        }

        // 第二阶段：全类名索引精确查询
        val cjTypeStatements = runReadAction {
            val results = arrayListOf<CjTypeStatementInfo<*>>()
            CangJieFullClassNameIndex.processElements(childName, project, searchScope) {
                ProgressManager.checkCanceled() // 支持用户取消长时间查询
                results += CjClassInfoUtil.createTypeStatementInfo(it)
                true
            }
            results
        }
        return cjTypeStatements
    }

    /**
     * 按完全限定名查找顶层类型别名声明集合。
     *
     * @param name 类型别名名称
     */
    override fun getTypeAliasDeclarations(name: Name): Collection<CjTypeAlias> {
        return CangJieTopLevelTypeAliasFqNameIndex[childName(name), project, searchScope]
    }

    /**
     * 当前包内所有已知声明名称集合，懒加载并以发布模式保证线程安全。
     *
     * 通过 [CangJiePackageSourcesMemberNamesIndex] 的 FileBasedIndex 接口批量读取，
     * 避免逐声明类型分别查询的开销。首次访问后结果被缓存，后续调用直接复用。
     */
    private val _declarationNames: Set<Name> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val names = hashSetOf<Name>()
        runReadAction {
            FileBasedIndex.getInstance()
                .processValues(
                    CangJiePackageSourcesMemberNamesIndex.NAME,
                    fqName.asString(),
                    null,
                    FileBasedIndex.ValueProcessor { _, values ->
                        ProgressManager.checkCanceled()
                        for (value in values) {
                            names += Name.identifier(value).safeNameForLazyResolve()
                        }
                        true
                    }, searchScope
                )
        }
        names
    }

    /** 返回当前包内所有已知声明名称集合 */
    override fun getDeclarationNames(): Set<Name> = _declarationNames

    /**
     * 获取当前包下所有已声明子包的完全限定名集合，并通过 [nameFilter] 过滤。
     *
     * @param nameFilter 子包名称过滤函数
     * @return 满足过滤条件的子包完全限定名集合
     */
    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName> {
        return CangJiePackageIndexUtils.getSubPackageFqNames(fqName, searchScope, nameFilter)
    }

    /**
     * 返回属于当前包的所有源文件集合。
     * 通过包索引工具精确匹配包名，仅返回包名严格等于 [fqName] 的文件。
     */
    override fun getPackageFiles(): Collection<CjFile> {
        return CangJiePackageIndexUtils.findFilesWithExactPackage(fqName, searchScope, project)
    }

    /**
     * 判断指定文件是否属于当前搜索范围（即是否属于当前包）。
     *
     * 若文件没有关联的虚拟文件（如内存文件），直接返回 false。
     *
     * @param file 待判断的文件
     * @return 若文件在 [searchScope] 内则返回 true，否则返回 false
     */
    override fun containsFile(file: CjFile): Boolean {
        return searchScope.contains(file.virtualFile ?: return false)
    }

    /**
     * 诊断方法：检查类/struct 声明的索引完整性。
     *
     * 若在当前 [searchScope] 内查不到指定名称的类声明，则进行更广泛的诊断：
     * 1. 扫描全局索引键，确认键是否存在但值缺失（索引损坏）
     * 2. 查询全量 Scope，确认声明是否存在于其他范围
     *
     * 若发现索引损坏迹象，发布 [CangJieCorruptedIndexListener] 事件并抛出
     * [IllegalStateException]，提示用户尝试修复 IDE 索引。
     *
     * 此方法仅在内部调试模式下使用（[@OptIn(IntellijInternalApi::class)]），
     * 不应在正常解析流程中调用。
     *
     * @param name 待检查的类型名称
     * @throws IllegalStateException 检测到索引损坏时抛出，附带详细诊断信息
     */
    @OptIn(IntellijInternalApi::class)
    fun checkClassOrStructDeclarations(name: Name) {
        val childName = childName(name)

        // 在当前 Scope 内查不到声明，开始诊断
        if (CangJieFullClassNameIndex.get(childName, project, searchScope).isEmpty()) {

            // 扫描全局索引键，确认 childName 是否在任何 Scope 下存在对应的键
            val processor = object : CommonProcessors.FindFirstProcessor<String>() {
                override fun accept(t: String?): Boolean = childName == t
            }
            CangJieFullClassNameIndex.processAllKeys(searchScope, null, processor)

            // 在全量 Scope 下查询，确认声明是否存在于当前 Scope 之外
            val everyObjects =
                CangJieFullClassNameIndex.get(childName, project, GlobalSearchScope.everythingScope(project))

            if (processor.isFound || everyObjects.isNotEmpty()) {
                // 键存在但当前 Scope 内无值，或声明存在于其他 Scope，判定为索引损坏
                project.messageBus.syncPublisher(CangJieCorruptedIndexListener.TOPIC).corruptionDetected()

                throw IllegalStateException(
                    """
                     | CangJieFullClassNameIndex ${if (processor.isFound) "has" else "has not"} '$childName' key.
                     | No value for it in $searchScope.
                     | Everything scope has ${everyObjects.size} objects${if (everyObjects.isNotEmpty()) " locations: ${everyObjects.map { it.containingFile.virtualFile }}" else ""}.
                     | 
                     | ${if (everyObjects.isEmpty()) "Please try File -> ${if (isApplicationInternalMode()) "Cache recovery -> " else ""}Repair IDE" else ""}
                    """.trimMargin()
                )
            }
        }
    }
}