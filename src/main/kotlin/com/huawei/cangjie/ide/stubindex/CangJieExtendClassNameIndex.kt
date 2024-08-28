package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjExtend
import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieExtendClassNameIndex internal constructor() : StringStubIndexExtension<CjExtend>() {
    companion object Helper : CangJieStringStubIndexHelper<CjExtend>(CjExtend::class.java) {
        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieExtendClassNameIndex = CangJieExtendClassNameIndex()

        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieExtendClassNameIndex {

            return CangJieExtendClassNameIndex()
        }

        override val indexKey: StubIndexKey<String, CjExtend> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieExtendClassNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjExtend> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieExtendClassNameIndex[fqName, project, scope]"))
    override fun get(fqName: String, project: Project, scope: GlobalSearchScope): Collection<CjExtend> {
        return Helper[fqName, project, scope]
    }
}
