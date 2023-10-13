package com.huawei.cangjie.lang.core.stubs

import com.huawei.cangjie.lang.core.stubs.index.CjNamedElementIndex
import com.huawei.cangjie.resolve.CjLangItemIndex
import com.intellij.psi.stubs.IndexSink

fun IndexSink.indexFunction(stub: CjFunctionStub) {
    indexNamedStub(stub)
//    CjLangItemIndex.index(stub.psi, this)
}
private fun IndexSink.indexNamedStub(stub: CjNamedStub) {
    stub.name?.let {
        occurrence(CjNamedElementIndex.KEY, it)
    }
}
