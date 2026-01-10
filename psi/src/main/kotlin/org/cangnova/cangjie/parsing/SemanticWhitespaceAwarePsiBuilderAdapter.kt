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
package org.cangnova.cangjie.parsing

import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.psi.tree.IElementType


open class SemanticWhitespaceAwarePsiBuilderAdapter(private val builder: SemanticWhitespaceAwarePsiBuilder) :
    PsiBuilderAdapter(
        builder
    ), SemanticWhitespaceAwarePsiBuilder {
    override fun newlineBeforeCurrentToken(): Boolean {
        return builder.newlineBeforeCurrentToken()
    }

    override fun disableNewlines() {
        builder.disableNewlines()
    }

    override fun enableNewlines() {
        builder.enableNewlines()
    }

    override fun restoreNewlinesState() {
        builder.restoreNewlinesState()
    }

    override fun restoreJoiningComplexTokensState() {
        builder.restoreJoiningComplexTokensState()
    }

    override fun enableJoiningComplexTokens() {
        builder.enableJoiningComplexTokens()
    }

    override fun disableJoiningComplexTokens() {
        builder.disableJoiningComplexTokens()
    }

    override fun isWhitespaceOrComment(elementType: IElementType): Boolean {
        return builder.isWhitespaceOrComment(elementType)
    }
}
