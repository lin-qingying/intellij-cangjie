package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.resolve.lazy.data.CjClassInfo
import com.huawei.cangjie.resolve.source.toSourceElement


fun CjClassInfo<*>.toSourceElement(): SourceElement {
    return elementByE.toSourceElement()
}
