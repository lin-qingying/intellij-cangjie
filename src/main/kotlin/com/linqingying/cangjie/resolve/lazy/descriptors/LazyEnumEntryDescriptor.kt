package com.linqingying.cangjie.resolve.lazy.descriptors

import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.resolve.lazy.data.CjClassInfo
import com.linqingying.cangjie.resolve.source.toSourceElement


fun CjClassInfo<*>.toSourceElement(): SourceElement {
    return elementByE.toSourceElement()
}
