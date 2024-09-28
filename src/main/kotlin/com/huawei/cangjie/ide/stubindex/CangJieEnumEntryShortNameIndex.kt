package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjEnum
import com.huawei.cangjie.psi.CjEnumEntry
import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieEnumEntryShortNameIndex internal constructor() : StringStubIndexExtension<CjEnumEntry>() {
    companion object Helper : CangJieStringStubIndexHelper<CjEnumEntry>(CjEnumEntry::class.java) {
        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieEnumEntryShortNameIndex {
            return CangJieEnumEntryShortNameIndex()
        }

        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieEnumEntryShortNameIndex = CangJieEnumEntryShortNameIndex()

        override val indexKey: StubIndexKey<String, CjEnumEntry> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieEnumEntryShortNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjEnumEntry> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieEnumEntryShortNameIndex[key, project, scope]"))
    override fun get(shortName: String, project: Project, scope: GlobalSearchScope): Collection<CjEnumEntry> {
        return Helper[shortName, project, scope]
    }
}
