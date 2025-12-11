/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.psi.CjSynchronizedExpression
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import org.cangnova.cangjie.utils.exceptions.CangJieTypeInfo

class SyncExpressionResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,

    val languageVersionSettings: LanguageVersionSettings
) {

    fun resolveSynchronizedExpression(
        expression: CjSynchronizedExpression, context: ExpressionTypingContext
    ): CangJieTypeInfo {

//val blockTypeInfo = callResolver

        return noTypeInfo(context.dataFlowInfo)
    }
}
