package com.linqingying.cangjie.toml

import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.psi.psiUtil.psiElement
import org.toml.lang.psi.TomlKeySegment

object CjpmTomlPsiPattern {
    private const val TOML_KEY_CONTEXT_NAME = "key"
    private const val TOML_KEY_VALUE_CONTEXT_NAME = "keyValue"
    private val PACKAGE_URL_ATTRIBUTES = setOf("homepage", "repository", "documentation")
    private inline fun <reified I : PsiElement> cjpmTomlPsiElement(): PsiElementPattern.Capture<I> {
        return psiElement<I>().inVirtualFile(
            StandardPatterns.or(
                VirtualFilePattern().withName(CjpmConstants.MANIFEST_FILE),

                )
        )
    }

    /** Any element inside any TomlKey in cjpm.toml */
    val inKey: PsiElementPattern.Capture<PsiElement> =
        cjpmTomlPsiElement<PsiElement>()
            .withParent(TomlKeySegment::class.java)

}