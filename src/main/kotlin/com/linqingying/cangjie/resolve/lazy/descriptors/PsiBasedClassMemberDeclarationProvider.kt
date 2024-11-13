package com.linqingying.cangjie.resolve.lazy.descriptors

import com.google.common.collect.ArrayListMultimap
import com.linqingying.cangjie.builtins.StandardNames.MAIN
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.safeNameForLazyResolve
import com.linqingying.cangjie.resolve.lazy.data.CjClassInfoUtil
import com.linqingying.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.linqingying.cangjie.resolve.lazy.data.CjTypeStatementInfo
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProvider
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.storage.StorageManager


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
                    variables.put(declaration.safeNameForLazyResolve(), declaration)

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

                else -> throw IllegalArgumentException("Unknown declaration: " + declaration)
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
