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


package com.linqingying.cangjie.resolve.lazy.declarations

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.lazy.data.CjTypeStatementInfo
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter

interface DeclarationProvider {
    fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<CjDeclaration>

    fun getFunctionDeclarations(name: Name): Collection<CjNamedFunction>
    fun getMainFunctionDeclarations( ): Collection<CjMainFunction>
    fun getMacroDeclarations(name: Name): Collection<CjMacroDeclaration>

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
