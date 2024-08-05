package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjNamedFunction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelFunctionByPackageIndex internal constructor() : StringStubIndexExtension<CjNamedFunction>() {
    companion object Helper : CangJieStringStubIndexHelper<CjNamedFunction>(CjNamedFunction::class.java) {
        override val indexKey: StubIndexKey<String, CjNamedFunction> =
            StubIndexKey.createIndexKey(CangJieTopLevelFunctionByPackageIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjNamedFunction> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelFunctionByPackageIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjNamedFunction> {
        return Helper[key, project, scope]
    }
}
