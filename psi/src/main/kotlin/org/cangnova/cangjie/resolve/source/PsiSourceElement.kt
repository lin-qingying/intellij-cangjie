/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.source

import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.SourceFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface PsiSourceElement : SourceElement {
    val psi: PsiElement?
    override val containingFile: SourceFile
        get() = psi?.containingFile?.let(::PsiSourceFile) ?: SourceFile.NO_SOURCE_FILE
}

class PsiSourceFile(val psiFile: PsiFile) : SourceFile {
    override fun equals(other: Any?): Boolean = other is PsiSourceFile && psiFile == other.psiFile

    override fun hashCode(): Int = psiFile.hashCode()

    override fun toString(): String = psiFile.virtualFile.path
    override val name: String?
        get() =  psiFile.virtualFile?.name

}
fun SourceElement.getPsi(): PsiElement? = (this as? PsiSourceElement)?.psi
