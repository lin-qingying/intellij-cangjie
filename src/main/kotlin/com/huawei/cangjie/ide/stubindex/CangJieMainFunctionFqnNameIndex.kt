package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjMainFunction
import com.huawei.cangjie.psi.CjNamedFunction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieMainFunctionFqnNameIndex internal constructor() : StringStubIndexExtension<CjMainFunction>() {

    companion  object Helper : CangJieStringStubIndexHelper<CjMainFunction>(CjMainFunction::class.java) {
        override val indexKey: StubIndexKey<String, CjMainFunction> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieMainFunctionFqnNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjMainFunction> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieMainFunctionFqnNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjMainFunction> {


        return Helper[key, project, scope]
    }
}
