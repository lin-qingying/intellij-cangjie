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

import com.intellij.psi.PsiElement
import java.util.*


class DiagnosticWithParameters3<E : PsiElement , A:Any, B:Any, C:Any>(
    psiElement: E,
    override val a: A,
    override val b: B,
    override val c: C,
    factory: DiagnosticFactory3<E, A, B, C>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters3Marker<A, B, C> {
    override val factory: DiagnosticFactory3<E, A, B, C>
        get() = super.factory as DiagnosticFactory3<E, A, B, C>

    override fun toString(): String {
        return "$factory(a = $a, b = $b, c = $c)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters3<*, *, *, *>
        return a == that.a &&
                b == that.b &&
                c == that.c
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a, b, c)
    }
}
