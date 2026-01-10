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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.psi.CjPsiUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil

/**
 * Deletes this single PSI element from the PSI tree without removing other elements.
 * This method is mostly used as a substitute for [PsiElement.delete] because [PsiElement.delete] can delete more than the element it is
 * called on. When for example calling it on a [CjClassOrObject], [PsiElement.delete] will delete the class or object but when this class
 * or object is the only declaration in the file, it will also delete the file. On the contrary [PsiElement.deleteSingle] will only delete
 * the class or object here, but not the file.
 */
fun PsiElement.deleteSingle() {
    CodeEditUtil.removeChild(parent?.node ?: return, node ?: return)
}
fun String.unquoteCangJieIdentifier(): String = CjPsiUtil.unquoteIdentifier(this)
