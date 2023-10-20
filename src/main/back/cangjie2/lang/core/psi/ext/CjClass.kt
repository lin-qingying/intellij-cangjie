package com.huawei.cangjie.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType


interface  CjClass : CjComponent{

}




abstract  class AbstractCjPsiClass(type: IElementType) : AbstractCjComponentImpl(type), CjClass{

}
//abstract  class AbstractCjPsiClass(node: ASTNode) : AbstractCjComponentImpl(node), CjClass
