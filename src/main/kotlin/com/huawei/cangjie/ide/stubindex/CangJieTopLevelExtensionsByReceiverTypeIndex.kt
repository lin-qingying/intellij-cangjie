package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjCallableDeclaration
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelExtensionsByReceiverTypeIndex internal constructor() : StringStubIndexExtension<CjCallableDeclaration>() {
    companion object Helper : CangJieExtensionsByReceiverTypeStubIndexHelper() {
        @JvmField
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieTopLevelExtensionsByReceiverTypeIndex = CangJieTopLevelExtensionsByReceiverTypeIndex()

        override val indexKey: StubIndexKey<String, CjCallableDeclaration> =
            StubIndexKey.createIndexKey(CangJieTopLevelExtensionsByReceiverTypeIndex::class.java.simpleName)
    }

    override fun getKey() = indexKey

    override fun getVersion(): Int = super.getVersion() + 1

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelExtensionsByReceiverTypeIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjCallableDeclaration> {
        return Helper[key, project, scope]
    }
}
