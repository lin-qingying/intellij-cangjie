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

import cn.cangnova.cangjie.AbstractCangJieBundle
import cn.cangnova.cangjie.diagnostics.DiagnosticWithParameters1
import cn.cangnova.cangjie.diagnostics.DiagnosticWithParameters4

class DiagnosticWithParametersMultiRenderer<A:Any>(
    message: () -> String ,
    private val renderer: MultiRenderer<A>
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters1<*, A>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters1<*, A>): Array<out Any> {
        return renderer.render(diagnostic.a)
    }
}
class DiagnosticWithParameters4Renderer<A :Any , B :Any , C :Any, D :Any>(
    message: () -> String ,
    private val rendererForA: DiagnosticParameterRenderer<A> ?,
    private val rendererForB: DiagnosticParameterRenderer<B> ?,
    private val rendererForC: DiagnosticParameterRenderer<C> ?,
    private val rendererForD: DiagnosticParameterRenderer<D> ?,
) : AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters4<*, A, B, C, D>>(message) {

    override fun renderParameters(diagnostic: DiagnosticWithParameters4<*, A, B, C, D>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c, diagnostic.d)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
            renderParameter(diagnostic.c, rendererForC, context),
            renderParameter(diagnostic.d, rendererForD, context),
        )
    }
}

interface MultiRenderer<in A> {
    fun render(a: A): Array<String>
}
