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

package org.cangnova.cangjie.stubindex.resolve

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider
import org.cangnova.cangjie.descriptors.data.CjClassLikeInfo
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.util.projectSourceModules
import org.cangnova.cangjie.resolve.caches.PerModulePackageCacheService
import org.cangnova.cangjie.resolve.lazy.declarations.AbstractDeclarationProviderFactory
import org.cangnova.cangjie.resolve.lazy.declarations.CombinedPackageMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.PsiBasedClassMemberDeclarationProvider
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.cjo.CjoPackageService
import org.cangnova.cangjie.stubindex.CangJiePackageIndexUtils

/**
 * IDE 插件环境下的声明提供者工厂。
 *
 * 继承自 [AbstractDeclarationProviderFactory]，负责为指定包创建 [PackageMemberDeclarationProvider]，
 * 并将以下三类声明来源合并为统一的视图：
 *
 * - **Stub 索引**（[StubBasedPackageMemberDeclarationProvider]）：已索引文件的顶层声明，性能最优
 * - **非索引文件**（[FileBasedDeclarationProviderFactory]）：尚未进入索引的合成文件或临时文件
 * - **宏展开文件**（[macroFileBasedDeclarationProviderFactory]）：宏展开产生的合成文件声明
 *
 * 宏展开场景下的核心处理逻辑：
 * 展开文件已包含源文件的全部声明（原始 + 宏生成），因此对应源文件必须从 Stub 索引中排除，
 * 防止两份声明同时参与解析导致 REDECLARATION。
 * 这通过 [MacroStubBasedPackageMemberDeclarationProvider] 配合 [macroExcludedSourceVirtualFiles] 实现。
 *
 * @param project 当前 IDE 项目实例
 * @param indexedFilesScope 已索引文件的搜索范围，用于 Stub 索引查询
 * @param storageManager 懒加载存储管理器
 * @param nonIndexedFiles 未进入索引的文件集合（如正在编辑的合成文件）
 * @param context 当前模块信息，用于包存在性判断和诊断
 * @param macroExcludedFiles 宏展开生成的合成文件集合，其对应源文件将从 Stub 索引中排除
 */
class PluginDeclarationProviderFactory(
    private val project: Project,
    private val indexedFilesScope: GlobalSearchScope,
    private val storageManager: StorageManager,
    private val nonIndexedFiles: Collection<CjFile>,
    private val context: ModuleInfo,
    private val macroExcludedFiles: Collection<CjFile> = emptyList()
) : AbstractDeclarationProviderFactory(storageManager) {

    /** 非索引文件的声明提供者工厂，处理尚未进入 Stub 索引的合成文件 */
    private val fileBasedDeclarationProviderFactory =
        FileBasedDeclarationProviderFactory(storageManager, nonIndexedFiles)

    /**
     * 宏展开文件的声明提供者工厂。
     *
     * 仅在存在宏展开文件时创建，为展开文件提供基于文件的声明。
     * 展开文件作为 syntheticFile 不在 Stub 索引中，因此通过此工厂提供声明。
     */
    private val macroFileBasedDeclarationProviderFactory =
        if (macroExcludedFiles.isNotEmpty())
            FileBasedDeclarationProviderFactory(storageManager, macroExcludedFiles)
        else null

    /**
     * 判断指定包是否存在。
     *
     * 按优先级依次检查三个来源：非索引文件工厂、宏展开文件工厂、Stub 索引。
     * 任意一个来源确认包存在即返回 true。
     */
    override fun packageExists(fqName: FqName) =
        fileBasedDeclarationProviderFactory.packageExists(fqName)
                || macroFileBasedDeclarationProviderFactory?.packageExists(fqName) == true
                || stubBasedPackageExists(fqName)

    /**
     * 通过 Stub 索引判断包是否存在。
     *
     * 采用两级检查策略：
     * 1. 对源码模块（[ModuleSourceInfo]），通过 [PerModulePackageCacheService] 按模块查询缓存
     * 2. 对库模块（非 [ModuleSourceInfo]），通过 [CjoPackageService] 查询 .cjo 二进制包信息，
     *    解决库文件不在 CangJiePartialPackageNamesIndex 中的问题
     *
     * @param name 待检查的包完全限定名
     * @return 包存在返回 true，否则返回 false
     */
    private fun stubBasedPackageExists(name: FqName): Boolean {
        // 第一级：通过模块包缓存检查源码模块
        val sourceModuleExists = (context as? IdeaModuleInfo)?.projectSourceModules()
            ?.any { PerModulePackageCacheService.getInstance(project).packageExists(name, it) }
            ?: false

        if (sourceModuleExists) return true

        // 第二级：对库模块使用 CjoPackageService 检查
        if (context !is ModuleSourceInfo) {
            return CjoPackageService.getInstance(project).hasPackage(name)
        }

        return false
    }

    /**
     * 诊断：CangJiePartialPackageNamesIndex 索引不一致导致的包片段缺失。
     *
     * 强制清理 [PerModulePackageCacheService] 缓存后，抛出 [InconsistencyIndexException]
     * 以触发上层的重试或错误上报逻辑。
     *
     * @param message 诊断详情
     * @throws InconsistencyIndexException 始终抛出
     */
    private fun diagnoseMissingPackageFragmentPartialPackageIndexCorruption(message: String): Nothing {
        PerModulePackageCacheService.getInstance(project).onTooComplexChange()
        throw InconsistencyIndexException("CangJiePartialPackageNamesIndex inconsistency.\n$message")
    }

    /**
     * 诊断：PerModulePackageCache 缓存未命中导致的包片段缺失。
     *
     * 延迟触发缓存重建（[PerModulePackageCacheService.onTooComplexChange]）后抛出异常。
     *
     * @param message 诊断详情
     * @throws InconsistencyIndexException 始终抛出
     */
    private fun diagnoseMissingPackageFragmentPerModulePackageCacheMiss(message: String): Nothing {
        PerModulePackageCacheService.getInstance(project).onTooComplexChange()
        throw InconsistencyIndexException("PerModulePackageCache miss.\n$message")
    }

    /**
     * 为指定包创建合并后的包成员声明提供者。
     *
     * 将 Stub 索引、非索引文件、宏展开文件三类来源的提供者合并为
     * [CombinedPackageMemberDeclarationProvider]，始终使用 Combined 包装，
     * 确保下游代码类型一致，简化处理逻辑。
     *
     * @param name 包完全限定名
     * @return 合并后的声明提供者，若无来源则返回 null
     */
    public override fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider? {
        val stubBasedProvider = StubBasedPackageMemberDeclarationProvider(name, project, indexedFilesScope)
        val fileBasedProvider = fileBasedDeclarationProviderFactory.getPackageMemberDeclarationProvider(name)
        val macroFileBasedProvider = macroFileBasedDeclarationProviderFactory?.getPackageMemberDeclarationProvider(name)

        val providers = listOfNotNull(stubBasedProvider, fileBasedProvider, macroFileBasedProvider)
        if (providers.isEmpty()) return null
        return CombinedPackageMemberDeclarationProvider(providers)
    }

    /**
     * 生成当前非索引文件的调试信息字符串。
     *
     * 输出每个非索引文件的名称、物理文件标志及修改戳，用于问题诊断。
     * 若无非索引文件则返回占位字符串。
     */
    private fun debugInfo(): String {
        if (nonIndexedFiles.isEmpty()) return "-no synthetic files-\n"

        return buildString {
            nonIndexedFiles.forEach {
                append(it.name)
                append(" isPhysical=${it.isPhysical}")
                append(" modStamp=${it.modificationStamp}")
                appendLine()
            }
        }
    }

    /**
     * 诊断：未知原因导致的包片段缺失，直接抛出 [IllegalStateException]。
     *
     * @param message 诊断详情
     * @throws IllegalStateException 始终抛出
     */
    private fun diagnoseMissingPackageFragmentUnknownReason(message: String): Nothing {
        throw IllegalStateException(message)
    }

    /**
     * 通过旧版包索引工具检查包是否存在（仅用于诊断对比）。
     */
    private fun oldPackageExists(packageFqName: FqName): Boolean =
        CangJiePackageIndexUtils.packageExists(packageFqName, indexedFilesScope)

    /** 工厂创建时的调试信息快照，用于事后诊断与创建时状态的对比 */
    private val onCreationDebugInfo = debugInfo()

    /**
     * 诊断包片段缺失问题，根据不同失败原因路由到对应的诊断分支。
     *
     * 收集多维度状态信息后，按以下逻辑判断失败原因：
     * - Scope 非空且包文件在 Scope 内，但包在索引中不存在 → 索引不一致（[diagnoseMissingPackageFragmentPartialPackageIndexCorruption]）
     * - Scope 非空且包文件在 Scope 内，包在索引中存在但缓存未命中 → 缓存问题（[diagnoseMissingPackageFragmentPerModulePackageCacheMiss]）
     * - 其他情况 → 未知原因（[diagnoseMissingPackageFragmentUnknownReason]）
     *
     * @param fqName 缺失的包完全限定名
     * @param file 触发缺失的源文件，为 null 表示来源不明
     * @throws InconsistencyIndexException 或 [IllegalStateException]，始终抛出
     */
    override fun diagnoseMissingPackageFragment(fqName: FqName, file: CjFile?) {
        val moduleSourceInfo = context as? ModuleSourceInfo

        // 收集多维度状态，用于精确定位失败原因
        val packageExists = CangJiePackageIndexUtils.packageExists(fqName, indexedFilesScope)
        val spiPackageExists = CangJiePackageIndexUtils.packageExists(fqName, project)
        val oldPackageExists = oldPackageExists(fqName)
        val cachedPackageExists =
            moduleSourceInfo?.let { project.service<PerModulePackageCacheService>().packageExists(fqName, it) }

        val common = """
                packageExists = $packageExists, cachedPackageExists = $cachedPackageExists,
                oldPackageExists = $oldPackageExists,
                SPI.packageExists = $spiPackageExists,
                context = ${context.name}
            """.trimIndent()

        // 构造详细诊断消息，区分有无关联文件的情况
        val message = if (file != null) {
            val virtualFile = file.virtualFile
            val inScope = virtualFile in indexedFilesScope
            val packageFqName = file.packageFqName
            """
                |Cannot find package fragment '$fqName' for file ${file.name}, file package = '$packageFqName':
                |vFile: $virtualFile,
                |nonIndexedFiles = $nonIndexedFiles, isNonIndexed = ${file in nonIndexedFiles},
                |scope = $indexedFilesScope, isInScope = $inScope,
                |$common,
                |packageFqNameByTree = '${file.packageFqNameByTree}', packageDirectiveText = '${file.packageDirective?.text}'
            """.trimMargin()
        } else {
            """
                |Cannot find package fragment '$fqName' for unspecified file:
                |nonIndexedFiles = $nonIndexedFiles,
                |scope = $indexedFilesScope,
                |$common
            """.trimMargin()
        }

        // Scope 非空且文件在 Scope 内，才具备路由到具体诊断分支的前提条件
        val scopeNotEmptyAndContainsFile =
            !GlobalSearchScope.isEmptyScope(indexedFilesScope) && (file == null || file.virtualFile in indexedFilesScope)

        when {
            // 包不在索引中 → 索引不一致
            scopeNotEmptyAndContainsFile
                    && !packageExists && !oldPackageExists ->
                diagnoseMissingPackageFragmentPartialPackageIndexCorruption(message)

            // 包在索引中但缓存未命中 → 缓存问题
            scopeNotEmptyAndContainsFile
                    && packageExists && cachedPackageExists == false ->
                diagnoseMissingPackageFragmentPerModulePackageCacheMiss(message)

            // 其他情况 → 未知原因
            else -> diagnoseMissingPackageFragmentUnknownReason(message)
        }
    }

    /**
     * 为指定类创建基于 PSI 的类成员声明提供者。
     *
     * @param classLikeInfo 目标类的元信息
     * @return [PsiBasedClassMemberDeclarationProvider] 实例
     */
    override fun getClassMemberDeclarationProvider(classLikeInfo: CjClassLikeInfo): ClassMemberDeclarationProvider {
        return PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo)
    }

    /**
     * 返回工厂的完整调试字符串，包含创建时和当前的非索引文件快照。
     * 用于在问题复现时对比工厂状态的前后变化。
     */
    fun debugToString(): String {
        return arrayOf(
            "PluginDeclarationProviderFactory", "On failure:", debugInfo(), "On creation:", onCreationDebugInfo
        ).joinToString("\n")
    }
}

/**
 * 索引不一致异常。
 *
 * 继承自 [ProcessCanceledException]，使 IntelliJ 平台将其识别为可重试的取消信号，
 * 触发上层的任务重启机制，而非直接报错给用户。
 *
 * @param message 异常详情，包含具体的索引不一致描述
 */
private class InconsistencyIndexException(message: String) : ProcessCanceledException(message)