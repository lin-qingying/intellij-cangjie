package com.huawei.cangjie.idea.stubindex

import com.huawei.cangjie.psi.CjCallableDeclaration
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieOverridableInternalMembersShortNameIndex internal constructor() : StringStubIndexExtension<CjCallableDeclaration>() {
    companion object Helper : CangJieStringStubIndexHelper<CjCallableDeclaration>(CjCallableDeclaration::class.java) {
        override val indexKey: StubIndexKey<String, CjCallableDeclaration> =
            StubIndexKey.createIndexKey(CangJieOverridableInternalMembersShortNameIndex::class.java.simpleName)
    }

    override fun getKey() = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieOverridableInternalMembersShortNameIndex[name, project, scope]"))
    override fun get(name: String, project: Project, scope: GlobalSearchScope): Collection<CjCallableDeclaration> {
        return Helper[name, project, scope]
    }
}