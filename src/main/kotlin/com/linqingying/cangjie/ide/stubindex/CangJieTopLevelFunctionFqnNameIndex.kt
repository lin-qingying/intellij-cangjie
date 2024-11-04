package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjNamedFunction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

/**
 * Stores package top level function (both extension and non-extension) full qualified names.
 */
class CangJieTopLevelFunctionFqnNameIndex internal constructor() : StringStubIndexExtension<CjNamedFunction>() {
  companion  object Helper : CangJieStringStubIndexHelper<CjNamedFunction>(CjNamedFunction::class.java) {
        override val indexKey: StubIndexKey<String, CjNamedFunction> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieTopLevelFunctionFqnNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjNamedFunction> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelFunctionFqnNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjNamedFunction> {


        return Helper[key, project, scope]
    }
}
