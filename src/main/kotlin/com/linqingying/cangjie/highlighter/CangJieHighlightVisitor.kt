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

package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.psi.CjFunction
import com.linqingying.cangjie.psi.CjMainFunction
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjParameterList
import com.linqingying.cangjie.psi.psiUtil.parents
import com.linqingying.cangjie.utils.match
import com.intellij.codeInsight.daemon.impl.HighlightVisitor

class CangJieHighlightVisitor : AbstractCangJieHighlightVisitor() {

    override fun shouldSuppressUnusedParameter(parameter: CjParameter): Boolean {
        val grandParent = parameter.parents.match(CjParameterList::class, last = CjFunction::class) ?: return false
//        if (!UnusedSymbolInspection.isEntryPoint(grandParent)) return false
        return  grandParent !is CjMainFunction
    }

    override fun clone(): HighlightVisitor = CangJieHighlightVisitor()


}
