package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjVisitorVoid
import com.linqingying.cangjie.utils.CangJieFrontEndException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement




class ExceptionWrappingCjVisitorVoid(private val delegate: CjVisitorVoid) : CjVisitorVoid() {


    override fun visitElement(element: PsiElement) {
        element.accept(delegate)
    }

    override fun visitDeclaration(dcl: CjDeclaration) {
        try {
            dcl.accept(delegate)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: CangJieFrontEndException) {
            throw e
        } catch (t: Throwable) {
            val name = try {
                dcl.name
            } catch (e: Throwable) {
                "- error: ${e.message}"
            }
            throw CangJieFrontEndException("Failed to analyze declaration $name", t, dcl)
        }
    }
}
