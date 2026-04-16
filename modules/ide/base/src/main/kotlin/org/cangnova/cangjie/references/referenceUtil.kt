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

package org.cangnova.cangjie.references
import org.cangnova.cangjie.idea.references.CDocReference
import org.cangnova.cangjie.idea.references.CjReference
import org.cangnova.cangjie.idea.references.CjSimpleNameReference
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.utils.firstIsInstance
import org.cangnova.cangjie.utils.firstIsInstanceOrNull

val CjElement.mainReference: CjReference?
    get() = when (this) {
        is CjReferenceExpression -> mainReference
        is CDocName -> mainReference
        else -> references.firstIsInstanceOrNull()
    }
val CDocName.mainReference: CDocReference
    get() = references.firstIsInstance()
val CjReferenceExpression.mainReference: CjReference
    get() = if (this is CjSimpleNameExpression) mainReference else references.firstIsInstance()
val CjSimpleNameExpression.mainReference: CjSimpleNameReference
    get() = references.firstIsInstance()
