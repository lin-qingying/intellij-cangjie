package com.linqingying.cangjie.doc.parser

import com.linqingying.cangjie.lang.CangJieLanguage
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import java.lang.reflect.Constructor



class CDocElementType(debugName: String, psiClass: Class<out PsiElement?>) :
    IElementType(debugName, CangJieLanguage) {
    private var psiFactory: Constructor<out PsiElement?>? = null

    init {
        psiFactory = try {
            psiClass.getConstructor(ASTNode::class.java)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Must have a constructor with ASTNode")
        }
    }

    fun createPsi(node: ASTNode): PsiElement {
        assert(node.elementType === this)
        return try {
            psiFactory?.newInstance(node)!!
        } catch (e: Exception) {
            throw RuntimeException("Error creating psi element for node", e)
        }
    }
}

