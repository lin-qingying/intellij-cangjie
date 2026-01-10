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
package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.psi.PsiComment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl

class CjFileStubBuilder : DefaultStubBuilder() {
    override fun buildStubTree(file: PsiFile): StubElement<*> {
        (file as? CjFile)?.customStubBuilder?.let {
            return it.buildStubTree(file)
        }

        return super.buildStubTree(file)
    }

    override fun createStubForFile(file: PsiFile): StubElement<*> {
        if (file !is CjFile) {
            return super.createStubForFile(file)
        }

        val packageFqName = file.packageFqName
        val errorMessage = findErrorMessage(file)

        val kind = when {
            errorMessage != null -> CangJieFileStubKindImpl.Invalid(errorMessage)
            file.hasTopLevelCallables() -> {
                CangJieFileStubKindImpl.Facade(
                    packageFqName = packageFqName,
                    facadeFqName = packageFqName,
                )
            }

            else -> CangJieFileStubKindImpl.File(packageFqName = packageFqName)
        }

        return CangJieFileStubImpl(file, kind)
    }
}

/**
 * Searches for a special error message from [CangJieFileStubKindImpl.Invalid].
 *
 * For now this place is aligned only with the decompiler.
 */
private fun findErrorMessage(file: CjFile): String? {
    if (!file.isCompiled) return null
    val firstComment = file.importList?.nextSibling as? PsiComment ?: return null
    if (firstComment.textMatches("// This file was compiled with a newer version of CangJie compiler and can't be decompiled.")) {
        return file.text
    }

    return null
}
