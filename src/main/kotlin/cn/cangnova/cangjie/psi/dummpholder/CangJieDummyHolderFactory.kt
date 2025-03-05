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

package cn.cangnova.cangjie.psi.dummpholder

import cn.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.psi.impl.source.HolderFactory
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.util.CharTable

class CangJieDummyHolderFactory : HolderFactory {
    override fun createHolder(manager: PsiManager, contentElement: TreeElement?, context: PsiElement?): DummyHolder {
        return CangJieDummyHolder(manager, contentElement, context)
    }

    override fun createHolder(manager: PsiManager, table: CharTable?, validity: Boolean): DummyHolder {
        return CangJieDummyHolder(manager, table, validity)
    }

    override fun createHolder(manager: PsiManager, context: PsiElement?): DummyHolder {
        return CangJieDummyHolder(manager, context)
    }

    override fun createHolder(manager: PsiManager, language: Language?, context: PsiElement?): DummyHolder {
        return if (language === CangJieLanguage) {
            CangJieDummyHolder(manager, context)
        } else {
            DummyHolder(
                manager,
                language,
                context,
            )
        }
    }

    override fun createHolder(
        manager: PsiManager,
        contentElement: TreeElement?,
        context: PsiElement?,
        table: CharTable?,
    ): DummyHolder {
        return CangJieDummyHolder(manager, contentElement, context, table)
    }

    override fun createHolder(manager: PsiManager, context: PsiElement?, table: CharTable?): DummyHolder {
        return CangJieDummyHolder(manager, context, table)
    }

    override fun createHolder(manager: PsiManager, table: CharTable?, language: Language?): DummyHolder {
        return CangJieDummyHolder(manager, table)
    }
}
