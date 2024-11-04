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
