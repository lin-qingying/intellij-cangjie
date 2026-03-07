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

package org.cangnova.cangjie.resolve.lazy.declarations

import org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.lazy.descriptors.AbstractPsiBasedDeclarationProvider
import org.cangnova.cangjie.storage.StorageManager

/**
 * 基于文件的包成员声明提供者。
 *
 * 继承自 [AbstractPsiBasedDeclarationProvider]，实现 [PackageMemberDeclarationProvider]，
 * 以一组 [CjFile] 文件作为数据来源，为指定包（[fqName]）构建其成员声明索引。
 *
 * 主要职责：
 * - 遍历包内所有文件，将文件中的顶层声明写入索引
 * - 提供子包枚举、包文件查询、文件归属判断等能力
 *
 * @param storageManager 用于管理懒加载值生命周期的存储管理器
 * @param fqName 当前提供者所对应的包的完全限定名
 * @param factory 用于查询子包信息的工厂，负责跨文件的包结构分析
 * @param packageFiles 属于该包的所有 [CjFile] 文件集合
 */
class FileBasedPackageMemberDeclarationProvider(
    storageManager: StorageManager,
    private val fqName: FqName,
    private val factory: FileBasedDeclarationProviderFactory,
    private val packageFiles: Collection<CjFile>
) : AbstractPsiBasedDeclarationProvider(storageManager), PackageMemberDeclarationProvider {

    /**
     * 当前包下所有已声明子包的完全限定名集合，懒加载缓存。
     * 首次访问时通过 [factory] 查询，后续复用缓存结果。
     */
    private val allDeclaredSubPackages = storageManager.createLazyValue<Collection<FqName>> {
        factory.getAllDeclaredSubPackagesOf(fqName)
    }

    /**
     * 构建包成员声明索引。
     *
     * 遍历 [packageFiles] 中的每个文件，将其顶层声明逐一写入索引。
     * 写入前会断言文件的包名与 [fqName] 一致，防止错误文件混入。
     *
     * @param index 待填充的声明索引
     * @throws AssertionError 若某个文件的包名与 [fqName] 不匹配
     */
    override fun doCreateIndex(index: Index) {
        for (file in packageFiles) {
            for (declaration in file.declarations) {
                // 校验文件包名与当前提供者的包名一致，防止文件归属错误
                assert(fqName == file.packageFqName) { "Files declaration utils contains file with invalid package" }
                index.putToIndex(declaration)
            }
        }
    }

    /**
     * 获取当前包下所有已声明的子包集合。
     *
     * 注意：当前实现忽略了 [nameFilter] 参数，返回全量子包列表，
     * 过滤逻辑由调用方自行处理。
     *
     * @param nameFilter 名称过滤函数（当前实现未使用）
     * @return 子包完全限定名集合
     */
    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName> = allDeclaredSubPackages()

    /**
     * 返回属于当前包的所有文件集合。
     */
    override fun getPackageFiles() = packageFiles

    /**
     * 判断指定文件是否属于当前包。
     *
     * @param file 待判断的文件
     * @return 若该文件在 [packageFiles] 中则返回 true，否则返回 false
     */
    override fun containsFile(file: CjFile) = file in packageFiles

    /**
     * 返回该提供者的调试描述字符串，包含包名、文件列表及其内部声明名称。
     */
    override fun toString() = "Declarations for package $fqName with files ${packageFiles.map { it.name }} " +
            "with declarations inside ${packageFiles.flatMap { it.declarations }.map { it.name ?: "???" }}"
}