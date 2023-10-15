package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CjElementTypes
import com.huawei.cangjie.lang.core.psi.CjPsiFactory
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.tree.IElementType


interface CjComponent : CjNameIdentifierOwner


abstract  class  AbstractCjComponentImpl(type: IElementType) : CjNamedElementImpl(type), CjComponent


//abstract class AbstractCjComponentImpl(node: ASTNode) : CjPsiCompositeElementImpl(node), CjComponent {
//    override fun getNameIdentifier(): PsiElement? {
//        return findChildByType(CjElementTypes.IDENTIFIER)
//    }
//
//    override fun setName(name: String): PsiElement {
//        val id = CjPsiFactory(project).createIdentifier(name)
//        nameIdentifier?.replace(id!!)
//        return this
//    }
//

//}

