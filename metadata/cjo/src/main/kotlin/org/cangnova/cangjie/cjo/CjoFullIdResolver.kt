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

package org.cangnova.cangjie.cjo

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.metadata.PackageIndex
import org.cangnova.cangjie.metadata.deserialization.DeclTable
import org.cangnova.cangjie.metadata.model.fb.FbDecl
import org.cangnova.cangjie.metadata.model.fb.FbDeclKind
import org.cangnova.cangjie.metadata.model.fb.FbFullId
import org.cangnova.cangjie.metadata.model.wrapper.*
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * FullId 解析结果
 */
sealed class NameResolveResult {
    /**
     * 解析成功
     *
     * @param fqName 完全限定名
     * @param name 简单名称
     * @param packageFqName 所在包的完全限定名
     * @param needsQualification 是否需要包限定（跨包引用时为 true）
     * @param declIndex 声明在 DeclTable 中的索引
     */
    data class Success(
        val fqName: FqName,
        val name: Name,
        val packageFqName: FqName,
        val needsQualification: Boolean,
        val declIndex: Int
    ) : NameResolveResult()

    /**
     * 解析失败
     */
    object Failed : NameResolveResult()
}

/**
 * Decl 解析结果
 */
sealed class DeclResolveResult {
    /**
     * 解析成功
     *
     * @param declWrapper 声明包装器
     * @param packageFqName 所在包的完全限定名
     * @param needsQualification 是否需要包限定
     */
    data class Success(
        val declWrapper: DeclarationWrapper,
        val packageFqName: FqName,
        val needsQualification: Boolean
    ) : DeclResolveResult()

    /**
     * 解析失败
     */
    object Failed : DeclResolveResult()
}

/**
 * Type 解析结果
 */
sealed class TypeResolveResult {
    /**
     * 解析成功
     *
     * @param typeWrapper 类型包装器
     * @param packageFqName 所在包的完全限定名
     * @param needsQualification 是否需要包限定
     */
    data class Success(
        val typeWrapper: TypeWrapper,
        val packageFqName: FqName,
        val needsQualification: Boolean
    ) : TypeResolveResult()

    /**
     * 解析失败
     */
    object Failed : TypeResolveResult()
}

/**
 * FullId 解析器接口
 *
 * 用于解析跨包类型引用。FullId 包含：
 * - pkgId: 包索引（-1=无效，-2=当前包，>=0=导入包索引）
 * - decl: 导出ID（exportId）
 * - index: 声明索引（1-based）
 */
interface CjoFullIdResolver {

    /**
     * 解析 FullId 获取基本名称信息
     *
     * @param fullId 完整ID
     * @param currentPackage 当前包上下文
     * @return 解析结果（包含名称信息）
     */
    fun resolveName(fullId: FbFullId, currentPackage: PackageWrapper): NameResolveResult

    /**
     * 解析 FullId 获取声明包装器
     *
     * @param fullId 完整ID
     * @param currentPackage 当前包上下文
     * @return 解析结果（包含 DeclWrapper）
     */
    fun resolveDecl(fullId: FbFullId, currentPackage: PackageWrapper): DeclResolveResult

    /**
     * 解析 FullId 获取类型包装器
     *
     * @param fullId 完整ID
     * @param currentPackage 当前包上下文
     * @return 解析结果（包含 TypeWrapper）
     */
    fun resolveType(fullId: FbFullId, currentPackage: PackageWrapper): TypeResolveResult

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目实例
         * @return 服务实例
         */
        fun getInstance(project: Project): CjoFullIdResolver {
            return project.getService(CjoFullIdResolver::class.java)
        }
    }
}

/**
 * 基于 CjoPackageService 的 FullId 解析器实现
 *
 * 项目级服务，通过 CjoPackageService 进行跨包解析。
 */
internal class CjoFullIdResolverImpl(private val project: Project) : CjoFullIdResolver {

    private val packageService: CjoPackageService
        get() = CjoPackageService.getInstance(project)

    override fun resolveName(fullId: FbFullId, currentPackage: PackageWrapper): NameResolveResult {
        return when (val pkgIndex = PackageIndex.fromValue(fullId.pkgId)) {
            PackageIndex.INVALID_PACKAGE_INDEX -> NameResolveResult.Failed

            PackageIndex.CURRENT_PKG_INDEX -> {
                resolveInCurrentPackage(currentPackage, fullId.index, fullId.decl)
            }

            null -> {
                // 跨包引用
                resolveInImportedPackage(currentPackage, fullId.pkgId, fullId.index, fullId.decl)
            }

            else -> NameResolveResult.Failed
        }
    }

    override fun resolveDecl(fullId: FbFullId, currentPackage: PackageWrapper): DeclResolveResult {
        return when (val pkgIndex = PackageIndex.fromValue(fullId.pkgId)) {
            PackageIndex.INVALID_PACKAGE_INDEX -> DeclResolveResult.Failed

            PackageIndex.CURRENT_PKG_INDEX -> {
                resolveDeclInCurrentPackage(currentPackage, fullId.index, fullId.decl)
            }

            null -> {
                // 跨包引用
                resolveDeclInImportedPackage(currentPackage, fullId.pkgId, fullId.index, fullId.decl)
            }

            else -> DeclResolveResult.Failed
        }
    }

    override fun resolveType(fullId: FbFullId, currentPackage: PackageWrapper): TypeResolveResult {
        return when (val pkgIndex = PackageIndex.fromValue(fullId.pkgId)) {
            PackageIndex.INVALID_PACKAGE_INDEX -> TypeResolveResult.Failed

            PackageIndex.CURRENT_PKG_INDEX -> {
                resolveTypeInCurrentPackage(currentPackage, fullId.index, fullId.decl)
            }

            null -> {
                // 跨包引用
                resolveTypeInImportedPackage(currentPackage, fullId.pkgId, fullId.index, fullId.decl)
            }

            else -> TypeResolveResult.Failed
        }
    }

    /**
     * 在当前包中解析声明
     */
    private fun resolveInCurrentPackage(
        currentPackage: PackageWrapper,
        index: Int,
        exportId: String
    ): NameResolveResult {
        val declTable = currentPackage.declTable
        val packageFqName = currentPackage.packageName

        // 优先使用 index 查找
        if (index > 0) {
            val decl = declTable.getOrNull(index)
            if (decl != null) {
                val name = Name.identifier(decl.identifier)
                return NameResolveResult.Success(
                    fqName = packageFqName.child(name),
                    name = name,
                    packageFqName = packageFqName,
                    needsQualification = false,
                    declIndex = index
                )
            }
        }

        // 使用 exportId 查找
        if (exportId.isNotEmpty()) {
            val foundResult = declTable.findByExportId(exportId)
            if (foundResult != null) {
                val (foundIndex, decl) = foundResult
                val name = Name.identifier(decl.identifier)
                return NameResolveResult.Success(
                    fqName = packageFqName.child(name),
                    name = name,
                    packageFqName = packageFqName,
                    needsQualification = false,
                    declIndex = foundIndex
                )
            }
        }

        return NameResolveResult.Failed
    }

    /**
     * 在导入包中解析声明
     */
    private fun resolveInImportedPackage(
        currentPackage: PackageWrapper,
        pkgId: Int,
        index: Int,
        exportId: String
    ): NameResolveResult {
        val importPackageFqNames = currentPackage.importPackageFqNames
        if (pkgId < 0 || pkgId >= importPackageFqNames.size) {
            return NameResolveResult.Failed
        }

        val packageFqName = importPackageFqNames[pkgId]

        // 尝试从服务获取包
        val importedPackage = packageService.getPackage(packageFqName)

        if (importedPackage != null) {
            val declTable = importedPackage.declTable

            // 优先使用 index 查找
            if (index > 0) {
                val decl = declTable.getOrNull(index)
                if (decl != null) {
                    val name = Name.identifier(decl.identifier)
                    return NameResolveResult.Success(
                        fqName = packageFqName.child(name),
                        name = name,
                        packageFqName = packageFqName,
                        needsQualification = true,
                        declIndex = index
                    )
                }
            }

            // 使用 exportId 查找
            if (exportId.isNotEmpty()) {
                val foundResult = declTable.findByExportId(exportId)
                if (foundResult != null) {
                    val (foundIndex, decl) = foundResult
                    val name = Name.identifier(decl.identifier)
                    return NameResolveResult.Success(
                        fqName = packageFqName.child(name),
                        name = name,
                        packageFqName = packageFqName,
                        needsQualification = true,
                        declIndex = foundIndex
                    )
                }
            }
        }

        // 即使无法加载包，也返回基于 exportId 的结果
        if (exportId.isNotEmpty()) {
            val name = Name.identifier(exportId)
            return NameResolveResult.Success(
                fqName = packageFqName.child(name),
                name = name,
                packageFqName = packageFqName,
                needsQualification = true,
                declIndex = -1
            )
        }

        return NameResolveResult.Failed
    }

    /**
     * 在当前包中解析 Decl
     */
    private fun resolveDeclInCurrentPackage(
        currentPackage: PackageWrapper,
        index: Int,
        exportId: String
    ): DeclResolveResult {
        val declTable = currentPackage.declTable
        val packageFqName = currentPackage.packageName

        // 优先使用 index 查找
        if (index > 0) {
            val decl = declTable.getOrNull(index)
            if (decl != null) {
                // 从 PackageWrapper 中查找已有声明
                val declWrapper = currentPackage.findDeclaration(decl)
                if (declWrapper != null) {
                    return DeclResolveResult.Success(
                        declWrapper = declWrapper,
                        packageFqName = packageFqName,
                        needsQualification = false
                    )
                }
            }
        }

        // 使用 exportId 查找
        if (exportId.isNotEmpty()) {
            val foundResult = declTable.findByExportId(exportId)
            if (foundResult != null) {
                val (_, decl) = foundResult
                // 从 PackageWrapper 中查找已有声明
                val declWrapper = currentPackage.findDeclaration(decl)
                if (declWrapper != null) {
                    return DeclResolveResult.Success(
                        declWrapper = declWrapper,
                        packageFqName = packageFqName,
                        needsQualification = false
                    )
                }
            }
        }

        return DeclResolveResult.Failed
    }

    /**
     * 在导入包中解析 Decl
     */
    private fun resolveDeclInImportedPackage(
        currentPackage: PackageWrapper,
        pkgId: Int,
        index: Int,
        exportId: String
    ): DeclResolveResult {
        val importPackageFqNames = currentPackage.importPackageFqNames
        if (pkgId < 0 || pkgId >= importPackageFqNames.size) {
            return DeclResolveResult.Failed
        }

        val packageFqName = importPackageFqNames[pkgId]
        val importedPackage = packageService.getPackage(packageFqName) ?: return DeclResolveResult.Failed

        val declTable = importedPackage.declTable

        // 优先使用 index 查找
        if (index > 0) {
            val decl = declTable.getOrNull(index)
            if (decl != null) {
                // 从 PackageWrapper 中查找已有声明
                val declWrapper = importedPackage.findDeclaration(decl)
                if (declWrapper != null) {
                    return DeclResolveResult.Success(
                        declWrapper = declWrapper,
                        packageFqName = packageFqName,
                        needsQualification = true
                    )
                }
            }
        }

        // 使用 exportId 查找
        if (exportId.isNotEmpty()) {
            val foundResult = declTable.findByExportId(exportId)
            if (foundResult != null) {
                val (_, decl) = foundResult
                // 从 PackageWrapper 中查找已有声明
                val declWrapper = importedPackage.findDeclaration(decl)
                if (declWrapper != null) {
                    return DeclResolveResult.Success(
                        declWrapper = declWrapper,
                        packageFqName = packageFqName,
                        needsQualification = true
                    )
                }
            }
        }

        return DeclResolveResult.Failed
    }

    /**
     * 在当前包中解析 Type
     */
    private fun resolveTypeInCurrentPackage(
        currentPackage: PackageWrapper,
        index: Int,
        exportId: String
    ): TypeResolveResult {
        val declTable = currentPackage.declTable
        val typeTable = currentPackage.typeTable
        val packageFqName = currentPackage.packageName

        // 优先使用 index 查找
        if (index > 0) {
            val decl = declTable.getOrNull(index)
            if (decl != null && decl.type > 0u) {
                val fbType = typeTable[decl.type.toInt()]
                val typeWrapper = TypeWrapper(fbType, declTable, typeTable)
                return TypeResolveResult.Success(
                    typeWrapper = typeWrapper,
                    packageFqName = packageFqName,
                    needsQualification = false
                )
            }
        }

        // 使用 exportId 查找
        if (exportId.isNotEmpty()) {
            val foundResult = declTable.findByExportId(exportId)
            if (foundResult != null) {
                val (_, decl) = foundResult
                if (decl.type > 0u) {
                    val fbType = typeTable[decl.type.toInt()]
                    val typeWrapper = TypeWrapper(fbType, declTable, typeTable)
                    return TypeResolveResult.Success(
                        typeWrapper = typeWrapper,
                        packageFqName = packageFqName,
                        needsQualification = false
                    )
                }
            }
        }

        return TypeResolveResult.Failed
    }

    /**
     * 在导入包中解析 Type
     */
    private fun resolveTypeInImportedPackage(
        currentPackage: PackageWrapper,
        pkgId: Int,
        index: Int,
        exportId: String
    ): TypeResolveResult {
        val importPackageFqNames = currentPackage.importPackageFqNames
        if (pkgId < 0 || pkgId >= importPackageFqNames.size) {
            return TypeResolveResult.Failed
        }

        val packageFqName = importPackageFqNames[pkgId]
        val importedPackage = packageService.getPackage(packageFqName) ?: return TypeResolveResult.Failed

        val declTable = importedPackage.declTable
        val typeTable = importedPackage.typeTable

        // 优先使用 index 查找
        if (index > 0) {
            val decl = declTable.getOrNull(index)
            if (decl != null && decl.type > 0u) {
                val fbType = typeTable[decl.type.toInt()]
                val typeWrapper = TypeWrapper(fbType, declTable, typeTable)
                return TypeResolveResult.Success(
                    typeWrapper = typeWrapper,
                    packageFqName = packageFqName,
                    needsQualification = true
                )
            }
        }

        // 使用 exportId 查找
        if (exportId.isNotEmpty()) {
            val foundResult = declTable.findByExportId(exportId)
            if (foundResult != null) {
                val (_, decl) = foundResult
                if (decl.type > 0u) {
                    val fbType = typeTable[decl.type.toInt()]
                    val typeWrapper = TypeWrapper(fbType, declTable, typeTable)
                    return TypeResolveResult.Success(
                        typeWrapper = typeWrapper,
                        packageFqName = packageFqName,
                        needsQualification = true
                    )
                }
            }
        }

        return TypeResolveResult.Failed
    }

    companion object {
        /**
         * 安全获取 DeclTable 中的声明
         */
        private fun DeclTable.getOrNull(index: Int): org.cangnova.cangjie.metadata.model.fb.FbDecl? {
            return try {
                if (index > 0 && index <= decls.size) {
                    this[index]
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 通过 exportId 查找声明
         * @return Pair of (1-based index, FbDecl) or null if not found
         */
        private fun DeclTable.findByExportId(exportId: String): Pair<Int, org.cangnova.cangjie.metadata.model.fb.FbDecl>? {
            for ((index, decl) in decls.withIndex()) {
                if (decl.exportId == exportId) {
                    return Pair(index + 1, decl)  // 返回 1-based index
                }
            }
            return null
        }

        /**
         * 从 PackageWrapper 中查找已有的声明包装器
         *
         * 对于顶层声明，从 PackageWrapper 的已有列表中查找；
         * 对于非顶层声明，使用扩展函数创建。
         *
         * @param fbDecl FlatBuffers 声明对象
         * @return 对应的 DeclarationWrapper，如果找不到返回 null
         */
        private fun PackageWrapper.findDeclaration(fbDecl: FbDecl): DeclarationWrapper? {
            // 非顶层声明，使用扩展函数创建
            if (!fbDecl.isTopLevel) {
                return fbDecl.toDeclarationWrapper(declTable, typeTable)
            }

            // 顶层声明，从已有列表中查找
            return when (fbDecl.kind) {
                FbDeclKind.ClassDecl -> classs.find { it.original == fbDecl }
                FbDeclKind.InterfaceDecl -> interfaces.find { it.original == fbDecl }
                FbDeclKind.StructDecl -> structs.find { it.original == fbDecl }
                FbDeclKind.EnumDecl -> enums.find { it.original == fbDecl }
                FbDeclKind.FuncDecl -> functions.find { it.original == fbDecl }
                FbDeclKind.VarDecl -> variables.find { it.original == fbDecl }
                FbDeclKind.TypeAliasDecl -> typeAliass.find { it.original == fbDecl }
                FbDeclKind.ExtendDecl -> extends.find { it.original == fbDecl }
                else -> null
            }
        }
    }
}
