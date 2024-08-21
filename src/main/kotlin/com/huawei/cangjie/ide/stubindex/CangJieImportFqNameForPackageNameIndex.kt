package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjImportDirective

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieImportFqNameForPackageNameIndex internal constructor() : StringStubIndexExtension<CjImportDirective>()  {
    companion object Helper : CangJieStringStubIndexHelper<CjImportDirective>(CjImportDirective::class.java) {
        override val indexKey: StubIndexKey<String, CjImportDirective> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieImportFqNameForPackageNameIndex")

        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieImportFqNameForPackageNameIndex = CangJieImportFqNameForPackageNameIndex()
    }

    override fun getKey(): StubIndexKey<String, CjImportDirective> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieImportFqNameForPackageNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjImportDirective> {
        return Helper[key, project, scope]
    }
}
