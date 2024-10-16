package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjMacroFunction

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieMacroFqnNameIndex internal constructor() : StringStubIndexExtension<CjMacroFunction>() {
    companion  object Helper : CangJieStringStubIndexHelper<CjMacroFunction>(CjMacroFunction::class.java) {
        override val indexKey: StubIndexKey<String, CjMacroFunction> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieMacroFqnNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjMacroFunction> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieMacroFqnNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjMacroFunction> {


        return Helper[key, project, scope]
    }
}
