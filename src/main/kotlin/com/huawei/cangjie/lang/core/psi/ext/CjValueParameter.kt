package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CjValueParameter

val CjValueParameter.patText: String?
    get() {
        val stub = greenStub
        return if (stub != null) stub.patText else  ""
    }
