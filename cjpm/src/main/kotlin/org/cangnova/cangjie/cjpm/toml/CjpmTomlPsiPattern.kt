package org.cangnova.cangjie.cjpm.toml

import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cjpm.CjpmConstants
import org.cangnova.cangjie.psi.psiUtil.psiElement
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue

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
    private fun tomlKeyValue(key: String): PsiElementPattern.Capture<TomlKeyValue> =
        psiElement<TomlKeyValue>().withChild(
            psiElement<TomlKey>().withText(key)
        )

    fun inValueWithKey(key: String): PsiElementPattern.Capture<PsiElement> {
        return cjpmTomlPsiElement<PsiElement>().inside(tomlKeyValue(key))
    }
    /** Any element inside any TomlKey in cjpm.toml */
    val inKey: PsiElementPattern.Capture<PsiElement> =
        cjpmTomlPsiElement<PsiElement>()
            .withParent(TomlKeySegment::class.java)

}