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

package com.linqingying.cangjie.diagnostics

import com.linqingying.cangjie.diagnostics.rendering.DiagnosticRenderer

abstract class DiagnosticFactory<D : UnboundDiagnostic> protected constructor(
    private var _name: String?,
    open val severity: Severity
){
    open val name: String
        get() = _name ?: "<unnamed>"
    fun initializeName(name: String) {
        _name = name
    }
    open var defaultRenderer: DiagnosticRenderer<D>? = null

    protected constructor(severity: Severity) : this(null, severity)

    @Suppress("UNCHECKED_CAST")
    fun initDefaultRenderer(defaultRenderer: DiagnosticRenderer<*>?) {
        this.defaultRenderer = defaultRenderer as DiagnosticRenderer<D>?
    }
    fun cast(diagnostic: UnboundDiagnostic): D {
        require(diagnostic.factory === this) { "Factory mismatch: expected " + this + " but was " + diagnostic.factory }
        @Suppress("UNCHECKED_CAST")
        return diagnostic as D
    }
    override fun toString(): String {
        return _name ?: "<Anonymous DiagnosticFactory>"
    }
    companion object {
        @SafeVarargs
        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, vararg factories: DiagnosticFactory<out D>): D {
            return cast(diagnostic, listOf(*factories))
        }

        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, factories: Collection<DiagnosticFactory<out D>>): D {
            for (factory in factories) {
                if (diagnostic.factory === factory) return factory.cast(diagnostic)
            }
            throw IllegalArgumentException("Factory mismatch: expected one of " + factories + " but was " + diagnostic.factory)
        }
    }
}
