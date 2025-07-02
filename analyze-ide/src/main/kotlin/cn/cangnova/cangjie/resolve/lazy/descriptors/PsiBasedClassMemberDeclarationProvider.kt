/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.lazy.descriptors

import com.google.common.collect.ArrayListMultimap
import cn.cangnova.cangjie.builtins.StandardNames.MAIN
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.safeNameForLazyResolve
import cn.cangnova.cangjie.psi.stubs.elements.getAllBindings
import cn.cangnova.cangjie.resolve.lazy.data.CjClassInfoUtil
import cn.cangnova.cangjie.resolve.lazy.data.CjClassLikeInfo
import cn.cangnova.cangjie.resolve.lazy.data.CjTypeStatementInfo
import cn.cangnova.cangjie.resolve.lazy.declarations.DeclarationProvider
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.storage.StorageManager


class PsiBasedClassMemberDeclarationProvider(
    storageManager: StorageManager,
    override val ownerInfo: CjClassLikeInfo
) : AbstractPsiBasedDeclarationProvider(storageManager), ClassMemberDeclarationProvider {

    override fun doCreateIndex(index: Index) {
        for (declaration in ownerInfo.declarations) {
            index.putToIndex(declaration)
        }

        for (parameter in ownerInfo.primaryConstructorParameters) {
            if (parameter.hasLetOrVar()) {
                index.putToIndex(parameter)
            }
        }
    }

    override fun toString() = "Declarations for $ownerInfo"
}

abstract class AbstractPsiBasedDeclarationProvider(storageManager: StorageManager) : DeclarationProvider {

    protected class Index {
        // This mutable state is only modified under inside the computable
        val allDeclarations = ArrayList<CjDeclaration>()
        val functions = ArrayListMultimap.create<Name, CjNamedFunction>()
        val mainFunctions = ArrayListMultimap.create<Name, CjMainFunction>()
        val properties = ArrayListMultimap.create<Name, CjProperty>()
        val variables = ArrayListMultimap.create<Name, CjVariable>()
        val macros = ArrayListMultimap.create<Name, CjMacroDeclaration>()
        val classesAndObjects = ArrayListMultimap.create<Name, CjTypeStatementInfo<*>>() // order matters here
        val extends = ArrayListMultimap.create<Name, CjTypeStatementInfo<CjExtend>>()

        //        val scripts = ArrayListMultimap.create<Name, CjScriptInfo>()
        val typeAliases = ArrayListMultimap.create<Name, CjTypeAlias>()

        val originalTypeAliases = ArrayListMultimap.create<Name, CjTypeAlias>()
        val destructuringDeclarationsEntries = ArrayListMultimap.create<Name, CjDestructuringDeclarationEntry>()
        val names = hashSetOf<Name>()

        fun putToIndex(declaration: CjDeclaration) {
            if (declaration is CjAnonymousInitializer || declaration is CjConstructor<*>) return

            allDeclarations.add(declaration)
            when (declaration) {
                is CjNamedFunction ->
                    functions.put(declaration.safeNameForLazyResolve(), declaration)

                is CjMainFunction ->
                    mainFunctions.put(MAIN, declaration)


                is CjMacroDeclaration -> macros.put(declaration.safeNameForLazyResolve(), declaration)
                is CjProperty ->
                    properties.put(declaration.safeNameForLazyResolve(), declaration)

                is CjVariable ->
                    if (declaration.isPattern) {
                        declaration.pattern.getAllBindings().forEach {
                            variables.put(it.safeNameForLazyResolve(), declaration)

                        }
                    } else {
                        variables.put(declaration.safeNameForLazyResolve(), declaration)

                    }

//
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

                is CjDestructuringDeclaration -> {
                    for (entry in declaration.entries) {
                        val name = entry.nameAsName.safeNameForLazyResolve()
                        destructuringDeclarationsEntries.put(name, entry)
                        names.add(name)
                    }
                }

                is CjParameter -> {
                    // Do nothing, just put it into allDeclarations is enough
                }



                else -> throw IllegalArgumentException("Unknown declaration: $declaration")
            }

            when (declaration) {
                is CjNamedDeclaration -> names.add(declaration.safeNameForLazyResolve())
            }
        }

        override fun toString() = "allDeclarations: " + allDeclarations.mapNotNull { it.name }
    }

    private val index = storageManager.createLazyValue {
        val index = Index()
        doCreateIndex(index)
        index
    }

    internal fun toInfoString() = toString() + ": " + index().toString()
    override fun getDeclarationNames() = index().names

    protected abstract fun doCreateIndex(index: Index)
    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<CjDeclaration> {
        val allDeclarations = index().allDeclarations
        if (kindFilter == DescriptorKindFilter.CLASSIFIERS) {
            return allDeclarations.filter { it is CjTypeStatement || it is CjTypeAlias }
        }
        return allDeclarations
    }

    override fun getFunctionDeclarations(name: Name): List<CjNamedFunction> =
        index().functions[name.safeNameForLazyResolve()].toList()

    override fun getMainFunctionDeclarations(): Collection<CjMainFunction> =
        index().mainFunctions[MAIN].toList()

    override fun getMacroDeclarations(name: Name): Collection<CjMacroDeclaration> =
        index().macros[name.safeNameForLazyResolve()].toList()

    override fun getPropertyDeclarations(name: Name): List<CjProperty> =
        index().properties[name.safeNameForLazyResolve()].toList()

    override fun getVariableDeclarations(name: Name): Collection<CjVariable> =
        index().variables[name.safeNameForLazyResolve()].toList()

    override fun getDestructuringDeclarationsEntries(name: Name): Collection<CjDestructuringDeclarationEntry> =
        index().destructuringDeclarationsEntries[name.safeNameForLazyResolve()].toList()


    override fun getTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<*>> =
        index().classesAndObjects[name.safeNameForLazyResolve()]

    override fun getEnumEntryDeclarations(name: Name): Collection<CjEnumEntry> {
        return emptyList()
    }

    override fun getExtendTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<CjExtend>> =
        index().extends[name.safeNameForLazyResolve()]

    override fun getAliasTypeStatementDeclarations(name: Name): Collection<CjTypeAlias> =
        index().originalTypeAliases[name.safeNameForLazyResolve()]


//    override fun getScriptDeclarations(name: Name): MutableList<CjScriptInfo> =
//        index().scripts[name.safeNameForLazyResolve()]

    override fun getTypeAliasDeclarations(name: Name): Collection<CjTypeAlias> =
        index().typeAliases[name.safeNameForLazyResolve()]

}
