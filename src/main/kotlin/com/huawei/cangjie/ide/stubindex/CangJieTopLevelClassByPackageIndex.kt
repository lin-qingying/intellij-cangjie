package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjClassOrStruct
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelClassByPackageIndex internal constructor() : StringStubIndexExtension<CjClassOrStruct>() {
    companion object Helper : CangJieStringStubIndexHelper<CjClassOrStruct>(CjClassOrStruct::class.java) {
        override val indexKey: StubIndexKey<String, CjClassOrStruct> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieTopLevelClassByPackageIndex")
    }

    override fun getKey(): StubIndexKey<String, CjClassOrStruct> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelClassByPackageIndex[fqName, project, scope]"))
    override fun get(fqName: String, project: Project, scope: GlobalSearchScope): Collection<CjClassOrStruct> {
        return Helper[fqName, project, scope]
    }
}
