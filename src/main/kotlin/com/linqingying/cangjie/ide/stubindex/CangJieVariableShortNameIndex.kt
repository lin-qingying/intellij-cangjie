package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieVariableShortNameIndex internal constructor() : StringStubIndexExtension<CjNamedDeclaration>() {
    companion object Helper : CangJieStringStubIndexHelper<CjNamedDeclaration>(CjNamedDeclaration::class.java) {
        override val indexKey: StubIndexKey<String, CjNamedDeclaration> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieVariableShortNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjNamedDeclaration> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieVariableShortNameIndex[shortName, project, scope]"))
    override fun get(shortName: String, project: Project, scope: GlobalSearchScope): Collection<CjNamedDeclaration> {
        return Helper[shortName, project, scope]
    }
}
