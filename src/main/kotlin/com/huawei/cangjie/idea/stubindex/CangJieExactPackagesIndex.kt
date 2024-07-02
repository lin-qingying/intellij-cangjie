package com.huawei.cangjie.idea.stubindex

import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey

class CangJieExactPackagesIndex internal constructor() : StringStubIndexExtension<CjFile>() {
    companion object {
        @JvmStatic
        private val LOG = Logger.getInstance(CangJieExactPackagesIndex::class.java)

        @JvmField
        val NAME: StubIndexKey<String, CjFile> = StubIndexKey.createIndexKey("com.huawei.cangjie.idea.stubindex.CangJieExactPackagesIndex")

        @JvmStatic
        @JvmName("getFiles")
        fun get(fqName: String, project: Project, scope: GlobalSearchScope): Collection<CjFile> {
            return getByKeyAndMeasure(NAME, LOG) { StubIndex.getElements (NAME, fqName, project, scope, CjFile::class.java) }
        }
    }

    override fun getKey(): StubIndexKey<String, CjFile> = NAME

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieExactPackagesIndex.get(fqName, project, scope)"))
    override fun get(fqName: String, project: Project, scope: GlobalSearchScope): Collection<CjFile> {
        return Companion.get(fqName, project, scope)
    }
}
