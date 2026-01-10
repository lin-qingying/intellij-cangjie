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

package org.cangnova.cangjie.psi
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.name.*

import com.intellij.psi.impl.source.tree.LeafPsiElement

interface ValueArgument {
    @IfNotParsed
    fun getArgumentExpression(): CjExpression?

    fun getArgumentName(): ValueArgumentName?

    fun isNamed(): Boolean

    fun asElement(): CjElement

    /* 例如foo(*arr)中的‘*’，即将数组作为多个var arg参数传递*/
    fun getSpreadElement(): LeafPsiElement?

    /* 参数放在外部以调用元素*/
    fun isExternal(): Boolean

    companion object
}

interface ValueArgumentName {
    val asName: Name
    val referenceExpression: CjSimpleNameExpression?
}

interface LambdaArgument : ValueArgument {
    fun getLambdaExpression(): CjLambdaExpression?
}
