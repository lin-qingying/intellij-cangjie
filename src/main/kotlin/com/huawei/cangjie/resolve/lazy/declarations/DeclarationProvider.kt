
package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter

interface DeclarationProvider {
    fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<CjDeclaration>

    fun getFunctionDeclarations(name: Name): Collection<CjNamedFunction>

    fun getVariableDeclarations(name: Name): Collection<CjProperty>

    fun getDestructuringDeclarationsEntries(name: Name): Collection<CjDestructuringDeclarationEntry>

//    fun getClassOrObjectDeclarations(name: Name): Collection<CjClassOrObjectInfo<*>>

    fun getTypeAliasDeclarations(name: Name): Collection<CjTypeAlias>

    fun getDeclarationNames(): Set<Name>
}
