package com.huawei.cangjie.idea.stubindex

import com.huawei.cangjie.psi.CjClassOrStruct
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieFullClassNameIndex internal constructor() : StringStubIndexExtension<CjClassOrStruct>() {
    companion object Helper : CangJieStringStubIndexHelper<CjClassOrStruct>(CjClassOrStruct::class.java) {
        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieFullClassNameIndex = CangJieFullClassNameIndex()

        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieFullClassNameIndex {
            return CangJieFullClassNameIndex()
        }

        override val indexKey: StubIndexKey<String, CjClassOrStruct> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.idea.stubindex.CangJieFullClassNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjClassOrStruct> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieFullClassNameIndex[fqName, project, scope]"))
    override fun get(fqName: String, project: Project, scope: GlobalSearchScope): Collection<CjClassOrStruct> {
        return Helper[fqName, project, scope]
    }
}