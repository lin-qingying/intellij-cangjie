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

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider
import org.cangnova.cangjie.descriptors.data.CjClassLikeInfo
import org.cangnova.cangjie.macro.file.CjMacroCallFile
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.PsiBasedClassMemberDeclarationProvider
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager

/**
 * 基于文件集合的声明提供者工厂。
 *
 * 继承自 [AbstractDeclarationProviderFactory]，以一组 [CjFile] 文件作为唯一数据来源，
 * 在内存中构建包名到文件的映射索引，适用于**尚未进入 Stub 索引**的文件，例如：
 * - 正在编辑但未保存的合成文件
 * - 宏展开产生的临时文件
 *
 * 与 [StubBasedPackageMemberDeclarationProvider] 相比，此工厂直接解析 PSI 树，
 * 无需依赖持久化的文件索引，因此适合处理增量或动态产生的文件。
 *
 * @param storageManager 懒加载存储管理器，用于延迟构建内部索引
 * @param files 参与索引构建的文件集合
 */
class FileBasedDeclarationProviderFactory(
    val storageManager: StorageManager,
    files: Collection<CjFile>
) : AbstractDeclarationProviderFactory(storageManager) {

    /**
     * 内部索引结构，存储包名到文件的映射及所有已声明包的集合。
     * 仅在 [computeFilesByPackage] 构建阶段写入，构建完成后只读。
     */
    private class Index {
        /** 包完全限定名 → 该包下所有文件的多值映射，保持插入顺序 */
        val filesByPackage: Multimap<FqName, CjFile> =
            LinkedHashMultimap.create<FqName, CjFile>()

        /**
         * 所有已声明的包名集合，包含叶子包及其所有祖先包。
         * 用于快速判断包是否存在，以及枚举子包。
         */
        val declaredPackages: MutableSet<FqName> = HashSet<FqName>()
    }

    /**
     * 获取指定父包下所有直接子包的完全限定名集合。
     *
     * 从 [Index.declaredPackages] 中过滤出父包为 [parent] 的非根包。
     *
     * @param parent 父包完全限定名
     * @return 直属子包的完全限定名集合
     */
    fun getAllDeclaredSubPackagesOf(parent: FqName): Collection<FqName> {
        return index.invoke().declaredPackages.filter { fqName: FqName ->
            !fqName.isRoot && fqName.parent() == parent
        }
    }

    companion object {

        /**
         * 递归地将指定包名及其所有祖先包名加入 [Index.declaredPackages]。
         *
         * 例如，对于包名 `a.b.c`，会依次添加 `a.b.c`、`a.b`、`a`（直到根包为止）。
         * 这确保了 [packageExists] 对中间层级的包名也能正确返回 true。
         *
         * @param index 待写入的索引对象
         * @param name 当前需要添加的包名
         */
        private fun addMeAndParentPackages(index: Index, name: FqName) {
            index.declaredPackages.add(name)
            if (!name.isRoot) {
                addMeAndParentPackages(index, name.parent())
            }
        }

        /**
         * 遍历文件集合，构建包名到文件的完整索引。
         *
         * 对每个文件：
         * 1. 读取其包名（[CjFile.packageFqName]）
         * 2. 将该包名及所有祖先包名加入 [Index.declaredPackages]
         * 3. 将文件写入 [Index.filesByPackage] 对应的包名槽位
         *
         * @param files 待索引的文件集合
         * @return 构建完成的 [Index] 实例
         */
        private fun computeFilesByPackage(files: Collection<CjFile>): Index {
            val index = Index()
            for (file in files) {
                val packageFqName: FqName = file.packageFqName
                addMeAndParentPackages(index, packageFqName)
                index.filesByPackage.put(packageFqName, file)
            }
            return index
        }
    }

    /**
     * 懒加载的内部索引，首次访问时触发 [computeFilesByPackage] 构建，后续复用缓存。
     */
    private val index: NotNullLazyValue<Index> = storageManager.createLazyValue {
        computeFilesByPackage(files)
    }

    /**
     * 判断指定包是否存在于当前文件集合中。
     *
     * @param packageFqName 待检查的包完全限定名
     * @return 若该包（或其子包对应的祖先包）存在于索引中则返回 true
     */
    override fun packageExists(packageFqName: FqName): Boolean {
        return index.invoke().declaredPackages.contains(packageFqName)
    }

    /**
     * 为指定包创建基于文件的包成员声明提供者。
     *
     * 若包存在，则创建 [FileBasedPackageMemberDeclarationProvider] 并传入该包下的所有文件；
     * 若包不存在，返回 null。
     *
     * @param name 包完全限定名
     * @return 对应的声明提供者，若包不存在则返回 null
     */
    override fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider? {
        if (packageExists(name)) {
            return FileBasedPackageMemberDeclarationProvider(
                storageManager, name, this, index.invoke().filesByPackage[name]
            )
        }
        return null
    }

    /**
     * 为指定类创建基于 PSI 的类成员声明提供者。
     *
     * 要求该类所在的包必须已在当前工厂的索引中，否则说明该类不属于此工厂管理的文件范围，
     * 直接抛出异常以快速暴露调用方的错误。
     *
     * @param classLikeInfo 目标类的元信息
     * @return [PsiBasedClassMemberDeclarationProvider] 实例
     * @throws IllegalStateException 若该类所在包不在当前工厂的索引中
     */
    override fun getClassMemberDeclarationProvider(classLikeInfo: CjClassLikeInfo): ClassMemberDeclarationProvider {
        check(index.invoke().filesByPackage.containsKey(classLikeInfo.containingPackageFqName)) {
            "This factory doesn't know about this class: $classLikeInfo"
        }
        return PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo)
    }
}