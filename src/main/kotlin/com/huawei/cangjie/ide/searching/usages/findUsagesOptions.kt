package com.huawei.cangjie.ide.searching.usages

import com.intellij.find.findUsages.PersistentFindUsagesOptions
import com.intellij.openapi.project.Project

interface CangJieMemberFindUsagesOptions {
    var searchExpected: Boolean
}

interface CangJieCallableFindUsagesOptions : CangJieMemberFindUsagesOptions {
    var searchOverrides: Boolean

}

class CangJieFunctionFindUsagesOptions(project: Project) : CangJieCallableFindUsagesOptions ,
    PersistentFindUsagesOptions(project){
    override var searchExpected: Boolean = true

    var isOverridingMethods: Boolean = true

    override var searchOverrides: Boolean
        get() = isOverridingMethods
        set(value) {
            isOverridingMethods = value
        }

    override fun setDefaults(project: Project) {
//        TODO("Not yet implemented")
    }

    override fun storeDefaults(project: Project) {
//        TODO("Not yet implemented")
    }

}
