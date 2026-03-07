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

package org.cangnova.cangjie.resolve.lazy.descriptors

import com.google.common.collect.ArrayListMultimap
import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.descriptors.DeclarationProvider
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.safeNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.elements.getAllPatternDeclarations
import org.cangnova.cangjie.descriptors.data.CjClassInfoUtil
import org.cangnova.cangjie.descriptors.data.CjClassLikeInfo
import org.cangnova.cangjie.descriptors.data.CjTypeStatementInfo
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.storage.StorageManager


/**
 * 基于 PSI 的类成员声明提供者。
 *
 * 继承自 [AbstractPsiBasedDeclarationProvider]，实现 [ClassMemberDeclarationProvider]，
 * 负责为指定类（[ownerInfo]）构建其成员声明索引，包括：
 * - 类体中的普通成员声明
 * - 主构造函数中带有 `let`/`var` 修饰的参数（即自动提升为属性的参数）
 *
 * @param storageManager 用于懒加载索引的存储管理器
 * @param ownerInfo 当前类的元信息，包含声明列表和主构造函数参数
 */
class PsiBasedClassMemberDeclarationProvider(
    storageManager: StorageManager,
    override val ownerInfo: CjClassLikeInfo
) : AbstractPsiBasedDeclarationProvider(storageManager), ClassMemberDeclarationProvider {

    /**
     * 构建类成员声明索引。
     *
     * 将 [ownerInfo] 中的所有成员声明及符合条件的主构造函数参数写入索引。
     *
     * @param index 待填充的声明索引
     */
    override fun doCreateIndex(index: Index) {
        // 索引类体中的所有成员声明
        for (declaration in ownerInfo.declarations) {
            index.putToIndex(declaration)
        }

        // 仅索引主构造函数中带有 let/var 的参数（这类参数会被提升为类属性）
        for (parameter in ownerInfo.primaryConstructorParameters) {
            if (parameter.hasLetOrVar()) {
                index.putToIndex(parameter)
            }
        }
    }

    override fun toString() = "Declarations for $ownerInfo"
}

/**
 * 基于 PSI 的声明提供者抽象基类。
 *
 * 通过懒加载方式构建并缓存声明索引（[Index]），所有具体的声明查询均基于该索引完成。
 * 子类需实现 [doCreateIndex] 方法，将各自来源的声明填充到索引中。
 *
 * @param storageManager 用于管理懒加载索引生命周期的存储管理器
 */
abstract class AbstractPsiBasedDeclarationProvider(storageManager: StorageManager) : DeclarationProvider {

    /**
     * 声明索引，按声明类型分类存储，支持按名称快速查找。
     *
     * 该类的所有可变状态仅在索引构建阶段（[doCreateIndex] 内部）被修改，
     * 构建完成后对外只读，保证线程安全。
     */
    protected class Index {
        /** 所有声明的有序列表，用于全量遍历 */
        val allDeclarations = ArrayList<CjDeclaration>()

        /** 具名函数，按名称索引 */
        val functions = ArrayListMultimap.create<Name, CjNamedFunction>()

        /** main 函数，统一以 [MAIN] 为 key 索引 */
        val mainFunctions = ArrayListMultimap.create<Name, CjMainFunction>()

        /** 属性声明，按名称索引 */
        val properties = ArrayListMultimap.create<Name, CjProperty>()

        /** 变量声明（字段变量、模式变量等），按名称索引 */
        val variables = ArrayListMultimap.create<Name, CjVariable<*>>()

        /** 宏声明，按名称索引 */
        val macros = ArrayListMultimap.create<Name, CjMacroDeclaration>()

        /** 类/对象/枚举等类型声明，按名称索引（顺序敏感） */
        val classesAndObjects = ArrayListMultimap.create<Name, CjTypeStatementInfo<*>>()

        /** extend 扩展声明，按名称索引 */
        val extends = ArrayListMultimap.create<Name, CjTypeStatementInfo<CjExtend>>()

        /** 类型别名声明，按名称索引 */
        val typeAliases = ArrayListMultimap.create<Name, CjTypeAlias>()

        /** 原始类型别名声明（未经重映射），按名称索引 */
        val originalTypeAliases = ArrayListMultimap.create<Name, CjTypeAlias>()

        /** 当前索引中所有已知的声明名称集合，用于快速存在性判断 */
        val names = hashSetOf<Name>()

        /**
         * 将单个声明写入索引。
         *
         * 根据声明的具体类型，分别写入对应的分类容器，并更新 [names] 集合。
         * 构造函数声明（[CjConstructor]）不参与索引，直接跳过。
         *
         * @param declaration 待索引的声明节点
         * @throws IllegalArgumentException 遇到未知声明类型时抛出
         */
        fun putToIndex(declaration: CjDeclaration) {
            // 构造函数不参与索引
            if (declaration is CjConstructor<*>) return

            allDeclarations.add(declaration)
            when (declaration) {
                is CjNamedFunction ->
                    functions.put(declaration.safeNameForLazyResolve(), declaration)

                is CjMainFunction ->
                    mainFunctions.put(MAIN, declaration)

                is CjMacroDeclaration ->
                    macros.put(declaration.safeNameForLazyResolve(), declaration)

                is CjProperty ->
                    properties.put(declaration.safeNameForLazyResolve(), declaration)

                is CjFieldVariable ->
                    variables.put(declaration.safeNameForLazyResolve(), declaration)

                is CjPatternVariable ->
                    // 模式变量可能展开为多个子声明（如解构模式），需逐一索引
                    if (declaration.pattern != null) {
                        declaration.pattern!!.getAllPatternDeclarations().forEach {
                            variables.put(it.nameAsName.safeNameForLazyResolve(), declaration)
                        }
                    } else {
                        variables.put(declaration.safeNameForLazyResolve(), declaration)
                    }

                is CjTypeAlias ->
                    typeAliases.put(declaration.nameAsName.safeNameForLazyResolve(), declaration)

                is CjExtend ->
                    extends.put(
                        declaration.nameAsName.safeNameForLazyResolve(),
                        CjClassInfoUtil.createTypeStatementInfo(declaration) as CjTypeStatementInfo<CjExtend>
                    )

                is CjTypeStatement ->
                    classesAndObjects.put(
                        declaration.nameAsName.safeNameForLazyResolve(),
                        CjClassInfoUtil.createTypeStatementInfo(declaration)
                    )

                is CjParameter -> {
                    // 参数仅加入 allDeclarations，不单独分类索引
                }

                else -> throw IllegalArgumentException("Unknown declaration: $declaration")
            }

            // 更新名称集合：模式变量展开子声明，其余具名声明直接添加
            when (declaration) {
                is CjPatternVariable -> declaration.pattern?.getAllPatternDeclarations()?.forEach {
                    if (it.nameAsName != null) {
                        names.add(it.nameAsName.safeNameForLazyResolve())
                    }
                }

                is CjNamedDeclaration -> names.add(declaration.safeNameForLazyResolve())
            }
        }

        override fun toString() = "allDeclarations: " + allDeclarations.mapNotNull { it.name }
    }

    /**
     * 懒加载的声明索引实例。
     * 首次访问时触发 [doCreateIndex] 构建，后续复用缓存结果。
     */
    private val index = storageManager.createLazyValue {
        val index = Index()
        doCreateIndex(index)
        index
    }

    /** 返回包含索引内容的调试字符串 */
    internal fun toInfoString() = toString() + ": " + index().toString()

    /** 返回当前索引中所有已知的声明名称集合 */
    override fun getDeclarationNames() = index().names

    /**
     * 由子类实现，负责将具体来源的声明填充到 [index] 中。
     *
     * @param index 待填充的声明索引
     */
    protected abstract fun doCreateIndex(index: Index)

    /**
     * 获取满足过滤条件的所有声明。
     *
     * 当 [kindFilter] 为 [DescriptorKindFilter.CLASSIFIERS] 时，仅返回类型声明和类型别名；
     * 否则返回全部声明，[nameFilter] 参数当前未使用（由调用方过滤）。
     *
     * @param kindFilter 声明种类过滤器
     * @param nameFilter 名称过滤函数（当前实现中未使用）
     * @return 符合条件的声明列表
     */
    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<CjDeclaration> {
        val allDeclarations = index().allDeclarations
        if (kindFilter == DescriptorKindFilter.CLASSIFIERS) {
            return allDeclarations.filter { it is CjTypeStatement || it is CjTypeAlias }
        }
        return allDeclarations
    }

    /**
     * 按名称查找具名函数声明列表。
     * @param name 函数名称
     */
    override fun getFunctionDeclarations(name: Name): List<CjNamedFunction> =
        index().functions[name.safeNameForLazyResolve()].toList()

    /**
     * 获取所有 main 函数声明。
     */
    override fun getMainFunctionDeclarations(): Collection<CjMainFunction> =
        index().mainFunctions[MAIN].toList()

    /**
     * 按名称查找宏声明集合。
     * @param name 宏名称
     */
    override fun getMacroDeclarations(name: Name): Collection<CjMacroDeclaration> =
        index().macros[name.safeNameForLazyResolve()].toList()

    /**
     * 按名称查找属性声明列表。
     * @param name 属性名称
     */
    override fun getPropertyDeclarations(name: Name): List<CjProperty> =
        index().properties[name.safeNameForLazyResolve()].toList()

    /**
     * 按名称查找变量声明集合。
     * @param name 变量名称
     */
    override fun getVariableDeclarations(name: Name): Collection<CjVariable<*>> =
        index().variables[name.safeNameForLazyResolve()].toList()

    /**
     * 按名称查找类型声明信息集合（类、对象、枚举等）。
     * @param name 类型名称
     */
    override fun getTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<*>> =
        index().classesAndObjects[name.safeNameForLazyResolve()]

    /**
     * 按名称查找原始类型别名声明集合。
     * @param name 类型别名名称
     */
    override fun getAliasTypeStatementDeclarations(name: Name): Collection<CjTypeAlias> =
        index().originalTypeAliases[name.safeNameForLazyResolve()]

    /**
     * 按名称查找类型别名声明集合。
     * @param name 类型别名名称
     */
    override fun getTypeAliasDeclarations(name: Name): Collection<CjTypeAlias> =
        index().typeAliases[name.safeNameForLazyResolve()]
}