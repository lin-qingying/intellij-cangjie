package com.huawei.cangjie.idea.stubindex

import com.huawei.cangjie.psi.CjNamedFunction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class CangJieProbablyNothingFunctionShortNameIndex internal constructor() : StringStubIndexExtension<CjNamedFunction>() {
    companion object Helper : CangJieStringStubIndexHelper<CjNamedFunction>(CjNamedFunction::class.java) {
        override val indexKey: StubIndexKey<String, CjNamedFunction> =
            StubIndexKey.createIndexKey(CangJieProbablyNothingFunctionShortNameIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjNamedFunction> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieProbablyNothingFunctionShortNameIndex[shortName, project, scope]"))
    override fun get(shortName: String, project: Project, scope: GlobalSearchScope): Collection<CjNamedFunction> {
        return Helper[shortName, project, scope]
    }
}