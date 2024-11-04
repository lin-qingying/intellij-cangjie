package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjMacroFunction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieMacroByPackageIndex internal constructor() : StringStubIndexExtension<CjMacroFunction>() {
    companion object Helper : CangJieStringStubIndexHelper<CjMacroFunction>(CjMacroFunction::class.java) {
        override val indexKey: StubIndexKey<String, CjMacroFunction> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieMacroByPackageIndex")
    }

    override fun getKey(): StubIndexKey<String, CjMacroFunction> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieMacroByPackageIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjMacroFunction> {


        return Helper[key, project, scope]
    }
}
