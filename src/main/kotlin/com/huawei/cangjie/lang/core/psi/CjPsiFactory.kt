package com.huawei.cangjie.lang.core.psi

import com.huawei.cangjie.lang.CjFileType
import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.huawei.cangjie.lang.core.psi.ext.descendantOfTypeStrict
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter

class CjPsiFactory
    (private val project: Project,
     private val markGenerated: Boolean = true,
     private val eventSystemEnabled: Boolean = false)
{

    fun createFile(text: CharSequence): CjFile = createPsiFile(text) as CjFile

    fun createPsiFile(text: CharSequence): PsiFile =
        PsiFileFactory.getInstance(project)
            .createFileFromText(
                "DUMMY.cj",
                CjFileType,
                text,
                 LocalTimeCounter.currentTime(),
                  eventSystemEnabled,
               markGenerated
            )
    fun createIdentifier(name: String): PsiElement? {

        return  createFile(name).descendantOfTypeStrict()




    }
//    fun createMetavarIdentifier(text: String): PsiElement =
//        createFromText<CjMetaVarIdentifier>("macro m { ($ $text) => () }")
//            ?: error("Failed to create metavar identifier: `$text`")

    private inline fun <reified T : CjElement> createFromText(code: CharSequence): T? =
        createFile(code).descendantOfTypeStrict()
    fun createQuoteIdentifier(text: String): PsiElement =
        createFromText<CjLifetimeParameter>("fn foo<$text>(_: &$text u8) {}")?.quoteIdentifier
            ?: error("Failed to create quote identifier: `$text`")
}
