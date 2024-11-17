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

import com.linqingying.cangjie.descriptors.GenericDiagnostics
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement

interface Diagnostics: GenericDiagnostics<Diagnostic> {
    val modificationTracker: ModificationTracker
        get() = throw IllegalStateException("Trying to obtain modification tracker for Diagnostics object of class ${this::class.java}")

    override fun all(): Collection<Diagnostic>

    override fun isEmpty(): Boolean = all().isEmpty()

    override fun iterator(): Iterator<Diagnostic> = all().iterator()

    fun forElement(psiElement: PsiElement): Collection<Diagnostic>

    fun noSuppression(): Diagnostics

    fun setCallback(callback: DiagnosticSink.DiagnosticsCallback) {
        setCallbackIfNotSet(callback)
    }

    fun setCallbackIfNotSet(callback: DiagnosticSink.DiagnosticsCallback): Boolean = false

    fun resetCallback() {}

    companion object {
        val EMPTY: Diagnostics = object : Diagnostics {
            override fun noSuppression(): Diagnostics = this
            override val modificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
            override fun all() = listOf<Diagnostic>()
            override fun forElement(psiElement: PsiElement) = listOf<Diagnostic>()
        }
    }
}
