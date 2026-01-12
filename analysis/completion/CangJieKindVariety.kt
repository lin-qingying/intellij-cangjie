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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.codeInsight.completion.CompletionParameters
import org.cangnova.cangjie.completion.turboComplete.KindVariety


/**
 * Represents categories of completion variants in CangJie   code completion.
 *
 * The names are used later to create [org.cangnova.cangjie.completion.turboComplete.KindCollector]s,
 * which are later reordered via [com.intellij.turboComplete.SortingExecutor].
 */
enum class CangJieCompletionKindName {
    DSL_FUNCTION,
    SMART_ADDITIONAL_ITEM,
    REFERENCE_BASIC,
    REFERENCE_EXTENSION,
    PACKAGE_NAME,
    NAMED_ARGUMENT,
    EXTENSION_FUNCTION_TYPE_VALUE,
    CONTEXT_VARIABLE_TYPE_SC,
    CONTEXT_VARIABLE_TYPE_REFERENCE,
    STATIC_MEMBER_FROM_IMPORTS,
    NON_IMPORTED,
    DEBUGGER_VARIANTS,
    STATIC_MEMBER_OBJECT_MEMBER,
    STATIC_MEMBER_EXPLICIT_INHERITED,
    STATIC_MEMBER_INACCESSIBLE,
    KEYWORD_ONLY,
    OPERATOR_NAME,
    DECLARATION_NAME,
    TOP_LEVEL_CLASS_NAME,
    SUPER_QUALIFIER,
    DECLARATION_NAME_FROM_UNRESOLVED_OVERRIDE,
    PARAMETER_OR_VAR_NAME_AND_TYPE,
}
object CangJieKindVariety : KindVariety {
    override fun kindsCorrespondToParameters(parameters: CompletionParameters): Boolean {
        return parameters.position.language == CangJieLanguage
    }

    override val actualCompletionContributorClass: Class<*>
        get() = CangJieCompletionContributor::class.java
}

