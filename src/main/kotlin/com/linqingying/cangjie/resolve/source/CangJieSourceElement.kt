package com.linqingying.cangjie.resolve.source

import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjPureElement

class CangJieSourceElement(override val psi: CjElement) : PsiSourceElement


fun CjPureElement?.toSourceElement(): SourceElement =
    if (this == null) SourceElement.NO_SOURCE else CangJieSourceElement(getPsiOrParent())
