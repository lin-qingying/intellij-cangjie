package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjMacroDeclaration
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieMacroDeclarationByPackageIndex internal constructor() : StringStubIndexExtension<CjMacroDeclaration>() {
    companion object Helper : CangJieStringStubIndexHelper<CjMacroDeclaration>(CjMacroDeclaration::class.java) {
        override val indexKey: StubIndexKey<String, CjMacroDeclaration> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieMacroDeclarationByPackageIndex")
    }

    override fun getKey(): StubIndexKey<String, CjMacroDeclaration> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieMacroDeclarationByPackageIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjMacroDeclaration> {


        return Helper[key, project, scope]
    }
}
