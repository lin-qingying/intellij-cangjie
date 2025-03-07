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

package cn.cangnova.cangjie.diagnostics.rendering

import cn.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.diagnostics.*
import cn.cangnova.cangjie.renderer.DescriptorRenderer

fun RenderingContext.Companion.fromDiagnostic(d: Diagnostic): RenderingContext = RenderingContext.Impl(parameters(d))

fun RenderingContext.Companion.parameters(d: Diagnostic): List<Any> = when (d) {
    is SimpleDiagnostic<*> -> listOf()
    is DiagnosticWithParameters1<*, *> -> listOf(d.a)
    is DiagnosticWithParameters2<*, *, *> -> listOf(d.a, d.b)
    is DiagnosticWithParameters3<*, *, *, *> -> listOf(d.a, d.b, d.c)
    is DiagnosticWithParameters4<*, *, *, *, *> -> listOf(d.a, d.b, d.c, d.d)
    is ParametrizedDiagnostic<*> -> error("Unexpected diagnostic: ${d::class.java}")
    else -> listOf()
}
fun ClassifierDescriptorWithTypeParameters.renderKindWithName(): String =
    DescriptorRenderer.getClassifierKindPrefix(this) + " '" + name + "'"
