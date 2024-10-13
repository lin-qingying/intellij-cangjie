package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjProperty
import com.huawei.cangjie.psi.CjVariable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelPropertyFqnNameIndex internal constructor() : StringStubIndexExtension<CjProperty>() {
    companion object Helper : CangJieStringStubIndexHelper<CjProperty>(CjProperty::class.java) {
        override val indexKey: StubIndexKey<String, CjProperty> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieTopLevelPropertyFqnNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjProperty> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelPropertyFqnNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjProperty> {
        return Helper[key, project, scope]
    }
}
