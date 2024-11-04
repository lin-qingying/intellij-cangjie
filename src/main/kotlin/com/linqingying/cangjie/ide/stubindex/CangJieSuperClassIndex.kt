package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjTypeStatement
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieSuperClassIndex internal constructor() : StringStubIndexExtension<CjTypeStatement>() {
    companion object Helper : CangJieStringStubIndexHelper<CjTypeStatement>(CjTypeStatement::class.java) {
        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieSuperClassIndex {
            return CangJieSuperClassIndex()
        }

        override val indexKey: StubIndexKey<String, CjTypeStatement> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieSuperClassIndex")
    }

    override fun getKey(): StubIndexKey<String, CjTypeStatement> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieSuperClassIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjTypeStatement> {
        return Helper[key, project, scope]
    }
}
