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

package org.cangnova.cangjie.refactoring.move

import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.deleteSingle
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl


interface CangJieMover : (CjNamedDeclaration, CjElement) -> CjNamedDeclaration {

    object Default : CangJieMover {
        override fun invoke(originalElement: CjNamedDeclaration, targetContainer: CjElement): CjNamedDeclaration {
            return when (targetContainer) {
                is CjFile -> {
                    val declarationContainer: CjElement = targetContainer
                    declarationContainer.add(originalElement) as CjNamedDeclaration
                }

                is CjTypeStatement -> targetContainer.addDeclaration(originalElement)
                else -> throw CangJieExceptionWithAttachmentsImpl("Unexpected element")
                    .withAttachment("context", targetContainer.getElementTextWithContext())
            }.apply {

                    originalElement.deleteSingle()


            }
        }
    }
}
