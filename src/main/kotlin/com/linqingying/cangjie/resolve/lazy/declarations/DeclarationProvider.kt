
package com.linqingying.cangjie.resolve.lazy.declarations

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.lazy.data.CjTypeStatementInfo
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter

interface DeclarationProvider {
    fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<CjDeclaration>

    fun getFunctionDeclarations(name: Name): Collection<CjNamedFunction>
    fun getMainFunctionDeclarations( ): Collection<CjMainFunction>

    fun getVariableDeclarations(name: Name): Collection<CjVariable>
    fun getPropertyDeclarations(name: Name): Collection<CjProperty>

    fun getDestructuringDeclarationsEntries(name: Name): Collection<CjDestructuringDeclarationEntry>
    fun getTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<*>>

    /**
     * 获取枚举项
     */
    fun getEnumEntryDeclarations(name: Name): Collection<CjEnumEntry>

    /**
     * 获取扩展
     */
    fun getExtendTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<CjExtend>>

    /**
     * 通过原类型名获取别名，需要验证其正确性
     */
    fun getAliasTypeStatementDeclarations(name: Name): Collection< CjTypeAlias >


//    通过别名获取别名
    fun getTypeAliasDeclarations(name: Name): Collection<CjTypeAlias>


    fun getDeclarationNames(): Set<Name>
}
