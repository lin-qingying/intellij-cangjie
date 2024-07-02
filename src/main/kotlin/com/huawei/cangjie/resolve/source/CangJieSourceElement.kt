package com.huawei.cangjie.resolve.source

import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjPureElement

class CangJieSourceElement(override val psi: CjElement) : PsiSourceElement {
}


fun CjPureElement?.toSourceElement(): SourceElement = if (this == null) SourceElement.NO_SOURCE else CangJieSourceElement(getPsiOrParent())
