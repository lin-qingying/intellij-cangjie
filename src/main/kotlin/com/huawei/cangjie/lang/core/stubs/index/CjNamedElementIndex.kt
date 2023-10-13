package com.huawei.cangjie.lang.core.stubs.index

import com.huawei.cangjie.lang.core.psi.ext.CjNamedElement
import com.huawei.cangjie.lang.core.stubs.CjFileStub
import com.huawei.cangjie.openapiext.checkCommitIsNotInProgress
import com.huawei.cangjie.openapiext.getElements
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CjNamedElementIndex : StringStubIndexExtension<CjNamedElement>() {
    override fun getVersion(): Int = CjFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, CjNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, CjNamedElement> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.lang.core.stubs.index.CjNamedElementIndex")

        fun findElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<CjNamedElement> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, target, project, scope)
        }
    }
}
