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

package org.cangnova.cangjie.psi.dummpholder

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.util.CharTable

class CangJieDummyHolder : DummyHolder {

  

    constructor(
        manager: PsiManager,
        contentElement: TreeElement?,
        context: PsiElement?,
    ) : super(manager, contentElement, context, null, null, language(context, CangJieLanguage))

    constructor(
        manager: PsiManager,
        table: CharTable?,
        validity: Boolean,
    ) : super(manager, null, null, table, validity, CangJieLanguage)

    constructor(manager: PsiManager, context: PsiElement?) : super(
        manager,
        null,
        context,
        null,
        null,
        language(context, CangJieLanguage),
    )

    constructor(manager: PsiManager, contentElement: TreeElement?, context: PsiElement?, table: CharTable?) : super(
        manager,
        contentElement,
        context,
        table,
        null,
        language(context, CangJieLanguage),
    )

    constructor(manager: PsiManager, context: PsiElement?, table: CharTable?) : super(
        manager,
        null,
        context,
        table,
        null,
        language(context, CangJieLanguage),
    )

    constructor(manager: PsiManager, table: CharTable?) : super(manager, null, null, table, null, CangJieLanguage)
}
