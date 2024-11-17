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

package com.linqingying.cangjie.ide.searching.usages

import com.intellij.find.findUsages.PersistentFindUsagesOptions
import com.intellij.openapi.project.Project

interface CangJieMemberFindUsagesOptions {
    var searchExpected: Boolean
}

interface CangJieCallableFindUsagesOptions : CangJieMemberFindUsagesOptions {
    var searchOverrides: Boolean

}
class CangJieClassFindUsagesOptions(project: Project) : CangJieMemberFindUsagesOptions,  PersistentFindUsagesOptions(project) {
    override var searchExpected: Boolean = true

    var searchConstructorUsages: Boolean = true
    var isDerivedInterfaces: Boolean = false
    var isCheckDeepInheritance: Boolean = true
    var isSkipImportStatements: Boolean = false
    var isDerivedClasses: Boolean = false
    var isFieldsUsages: Boolean = false
    var isMethodsUsages: Boolean = false

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is CangJieClassFindUsagesOptions && other.searchConstructorUsages == searchConstructorUsages
    }

    override fun hashCode(): Int {
        return 31 * super.hashCode() + if (searchConstructorUsages) 1 else 0
    }

    override fun setDefaults(project: Project) {

    }

    override fun storeDefaults(project: Project) {

    }
}

class CangJieFunctionFindUsagesOptions(project: Project) : CangJieCallableFindUsagesOptions ,
    PersistentFindUsagesOptions(project){
    override var searchExpected: Boolean = true
    var isSearchForBaseMethod: Boolean = true
    var isIncludeOverloadUsages: Boolean = false

    var isOverridingMethods: Boolean = true

    override var searchOverrides: Boolean
        get() = isOverridingMethods
        set(value) {
            isOverridingMethods = value
        }


    init {
        isUsages = true
    }
    override fun setDefaults(project: Project) {
//        TODO("Not yet implemented")
    }

    override fun storeDefaults(project: Project) {
//        TODO("Not yet implemented")
    }

}
class CangJiePropertyFindUsagesOptions(
    project: Project
) : CangJieCallableFindUsagesOptions , PersistentFindUsagesOptions(project){
    override var searchExpected: Boolean = true
    var isReadWriteAccess: Boolean = true
    override var searchOverrides: Boolean = false
    var isSearchForBaseAccessors: Boolean = false
    var isSearchInOverridingMethods: Boolean = false
    var isReadAccess: Boolean = true
    var isWriteAccess: Boolean = true
    var isSkipImportStatements: Boolean = false
    override fun setDefaults(project: Project) {


    }

    override fun storeDefaults(project: Project) {

    }

}
