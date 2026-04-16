/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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