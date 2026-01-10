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

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType

interface SemanticWhitespaceAwarePsiBuilder : PsiBuilder {

    /**
     * 检查当前标记之前是否有换行符
     */
    fun newlineBeforeCurrentToken(): Boolean

    /**
     * 禁用换行符
     */
    fun disableNewlines()

    /**
     * 启用换行符
     */
    fun enableNewlines()

    /**
     * 恢复换行符状态
     */
    fun restoreNewlinesState()

    /**
     * 恢复复杂标记的连接
     */
    fun restoreJoiningComplexTokensState()

    /**
     * 启用复杂标记的连接
     */
    fun enableJoiningComplexTokens()

    /**
     * 禁用复杂标记的连接
     */
    fun disableJoiningComplexTokens()

    /**
     * 检查当前标记是否是空白符或注释
     */
    override fun isWhitespaceOrComment(elementType: IElementType): Boolean
}
