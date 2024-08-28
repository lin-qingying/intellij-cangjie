package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieFullClassNameIndex internal constructor() : StringStubIndexExtension<CjTypeStatement>() {
    companion object Helper : CangJieStringStubIndexHelper<CjTypeStatement>(CjTypeStatement::class.java) {
        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieFullClassNameIndex = CangJieFullClassNameIndex()

        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieFullClassNameIndex {

            return CangJieFullClassNameIndex()
        }

        override val indexKey: StubIndexKey<String, CjTypeStatement> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieFullClassNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjTypeStatement> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieFullClassNameIndex[fqName, project, scope]"))
    override fun get(fqName: String, project: Project, scope: GlobalSearchScope): Collection<CjTypeStatement> {
        return Helper[fqName, project, scope]
    }
}
