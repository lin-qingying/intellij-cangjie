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

package cn.cangnova.cangjie.ide.cdoc

import cn.cangnova.cangjie.doc.psi.CDoc
import cn.cangnova.cangjie.doc.psi.impl.CDocName
import cn.cangnova.cangjie.psi.CjFunction
import cn.cangnova.cangjie.psi.CjPsiFactory
import cn.cangnova.cangjie.psi.psiUtil.getChildOfType
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil

class CDocElementFactory(val project: Project) {
    fun createCDocFromText(text: String): CDoc {
        val fileText = "$text func foo { }"
        val function = CjPsiFactory(project).createDeclaration<CjFunction>(fileText)
        return PsiTreeUtil.findChildOfType(function, CDoc::class.java)!!
    }

    fun createNameFromText(text: String): CDocName {
        val cdocText = "/** @param $text foo*/"
        val cdoc = createCDocFromText(cdocText)
        val section = cdoc.getDefaultSection()
        val tag = section.findTagByName("param")
        val link = tag?.getSubjectLink()
            ?: throw IllegalArgumentException("Cannot find subject link in doc comment '$cdocText'")
        return link.getChildOfType()!!
    }
}
