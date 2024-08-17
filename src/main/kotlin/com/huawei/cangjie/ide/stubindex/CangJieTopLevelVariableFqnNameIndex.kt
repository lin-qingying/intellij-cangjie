package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjVariable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelVariableFqnNameIndex internal constructor() : StringStubIndexExtension<CjVariable>() {
    companion object Helper : CangJieStringStubIndexHelper<CjVariable>(CjVariable::class.java) {
        override val indexKey: StubIndexKey<String, CjVariable> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieTopLevelVariableFqnNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjVariable> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelVariableFqnNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjVariable> {
        return Helper[key, project, scope]
    }
}
