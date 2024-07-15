package com.huawei.cangjie.idea.stubindex

import com.huawei.cangjie.psi.CjClassOrStruct
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieSuperClassIndex internal constructor() : StringStubIndexExtension<CjClassOrStruct>() {
    companion object Helper : CangJieStringStubIndexHelper<CjClassOrStruct>(CjClassOrStruct::class.java) {
        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieSuperClassIndex {
            return CangJieSuperClassIndex()
        }

        override val indexKey: StubIndexKey<String, CjClassOrStruct> =
            StubIndexKey.createIndexKey("com.huawei.cnagjie.idea.stubindex.CangJieSuperClassIndex")
    }

    override fun getKey(): StubIndexKey<String, CjClassOrStruct> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieSuperClassIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjClassOrStruct> {
        return Helper[key, project, scope]
    }
}
