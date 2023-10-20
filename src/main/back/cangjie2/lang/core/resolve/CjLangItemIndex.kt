package com.huawei.cangjie.lang.core.resolve

import com.huawei.cangjie.lang.core.psi.ext.CjItemElement
import com.huawei.cangjie.lang.core.stubs.CjFileStub
import com.intellij.psi.stubs.AbstractStubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor


class CjLangItemIndex : AbstractStubIndex<String, CjItemElement>() {
    override fun getVersion(): Int = CjFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, CjItemElement> = KEY
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    companion object {
//        fun findLangItem(
//            project: Project,
//            langAttribute: String,
//            crateName: String = AutoInjectedCrates.CORE
//        ): CjNamedElement? {
//            checkCommitIsNotInProgress(project)
//            return getElements(KEY, langAttribute, project, GlobalSearchScope.allScope(project))
//                .filterIsInstance<CjNamedElement>()
//                .singleOrFilter { it.containingCrate.normName == crateName }
//                .singleOrFilter { it.existsAfterExpansion }
//                .firstOrNull()
//        }
//
//        fun index(psi: CjItemElement, sink: IndexSink) {
//            for (key in psi.getTraversedRawAttributes().langAttributes) {
//                sink.occurrence(KEY, key)
//            }
//        }

        private val KEY: StubIndexKey<String, CjItemElement> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.lang.core.resolve.CjLangItemIndex")
    }
}
