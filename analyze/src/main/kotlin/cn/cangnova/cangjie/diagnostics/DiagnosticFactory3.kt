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

package cn.cangnova.cangjie.diagnostics

import cn.cangnova.cangjie.descriptors.PositioningStrategies
import com.intellij.psi.PsiElement


class DiagnosticFactory3<E : PsiElement, A:Any, B:Any, C:Any> protected constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters3<E, A, B, C> >(severity, positioningStrategy) {
    fun on(element: E, a: A, b: B, c: C): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters3(element, a, b, c, this, severity)
    }

    companion object {
        @JvmStatic
        fun <T : PsiElement, A:Any, B:Any, C:Any> create(severity: Severity): DiagnosticFactory3<T, A, B, C> {
            return create(severity, PositioningStrategies.DEFAULT)
        }
        @JvmStatic
        fun <T : PsiElement, A:Any, B:Any, C:Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory3<T, A, B, C> {
            return DiagnosticFactory3(severity, positioningStrategy)
        }
    }
}
