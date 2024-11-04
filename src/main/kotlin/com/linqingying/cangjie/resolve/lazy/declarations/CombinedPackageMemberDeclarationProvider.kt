package com.linqingying.cangjie.resolve.lazy.declarations

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.lazy.data.CjTypeStatementInfo
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter

class CombinedPackageMemberDeclarationProvider(
    val providers: Collection<PackageMemberDeclarationProvider>
) : PackageMemberDeclarationProvider {
    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        providers.flatMap { it.getDeclarations(kindFilter, nameFilter) }

    override fun getPackageFiles() = providers.flatMap { it.getPackageFiles() }
    override fun getFunctionDeclarations(name: Name) = providers.flatMap { it.getFunctionDeclarations(name) }
    override fun getMainFunctionDeclarations(): Collection<CjMainFunction> = providers.flatMap { it.getMainFunctionDeclarations( ) }


    override fun getVariableDeclarations(name: Name) = providers.flatMap { it.getVariableDeclarations(name) }
    override fun getPropertyDeclarations(name: Name) = providers.flatMap { it.getPropertyDeclarations(name) }


    override fun getDestructuringDeclarationsEntries(name: Name): Collection<CjDestructuringDeclarationEntry> {
        return providers.flatMap { it.getDestructuringDeclarationsEntries(name) }
    }

    override fun getEnumEntryDeclarations(name: Name): Collection<CjEnumEntry> {
        return providers.flatMap { it.getEnumEntryDeclarations(name) }

    }
    override fun getTypeStatementDeclarations(name: Name) = providers.flatMap { it.getTypeStatementDeclarations(name) }

    override fun getExtendTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<CjExtend>> =
        providers.flatMap { it.getExtendTypeStatementDeclarations(name) }

    override fun getAliasTypeStatementDeclarations(name: Name): Collection<CjTypeAlias> = providers.flatMap {
        it.getAliasTypeStatementDeclarations(name)
    }

    override fun getTypeAliasDeclarations(name: Name) = providers.flatMap { it.getTypeAliasDeclarations(name) }


    override fun getDeclarationNames(): Set<Name> = providers.flatMapTo(HashSet()) { it.getDeclarationNames() }

    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean) =
        providers.flatMap { it.getAllDeclaredSubPackages(nameFilter) }


    override fun containsFile(file: CjFile) = providers.any { it.containsFile(file) }

}
