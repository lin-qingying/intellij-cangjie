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

package org.cangnova.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVariable

/**
 * IDE 侧结构高亮兼容函数。
 *
 * PSI -> HighlightInfoType 规则已经迁入 code-insight/highlighting；这些函数只保留旧 API 名称。
 */
fun textAttributesKeyForCjElement(element: PsiElement): HighlightInfoType? =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesKeyForCjElement(element)

fun textAttributesForCjVariableDeclaration(variable: CjVariable<*>): HighlightInfoType =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesForCjVariableDeclaration(variable)

fun textAttributesForCjPropertyDeclaration(property: CjProperty): HighlightInfoType =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesForCjPropertyDeclaration(property)

fun textAttributesForCjParameterDeclaration(parameter: CjParameter): HighlightInfoType =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesForCjParameterDeclaration(parameter)

fun textAttributesKeyForPropertyDeclaration(declaration: PsiElement): HighlightInfoType? =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesKeyForPropertyDeclaration(declaration)

fun textAttributesKeyForCjFunction(function: PsiElement): HighlightInfoType? =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesKeyForCjFunction(function)

fun textAttributesKeyForTypeDeclaration(declaration: PsiElement): HighlightInfoType? =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesKeyForTypeDeclaration(declaration)

fun textAttributesForClass(cclass: CjTypeStatement): HighlightInfoType =
    org.cangnova.cangjie.codeinsight.highlighting.textAttributesForClass(cclass)
