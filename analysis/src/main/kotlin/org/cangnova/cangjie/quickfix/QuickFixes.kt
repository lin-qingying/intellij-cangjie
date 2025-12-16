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

package org.cangnova.cangjie.quickfix

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.cangnova.cangjie.utils.ifNotEmpty


@Service
class QuickFixes {
    private val factories: Multimap<DiagnosticFactory<*>, CangJieIntentionActionsFactory> =
        HashMultimap.create<DiagnosticFactory<*>, CangJieIntentionActionsFactory>()
    private val unresolvedReferenceActionFactories: Multimap<DiagnosticFactory<*>, CangJieIntentionActionsFactory> =
        HashMultimap.create<DiagnosticFactory<*>, CangJieIntentionActionsFactory>()
    private val actions: Multimap<DiagnosticFactory<*>, IntentionAction> =
        HashMultimap.create<DiagnosticFactory<*>, IntentionAction>()

    init {

        QuickFixContributor.EP_NAME.extensionList.forEach { it.registerQuickFixes(this) }
    }

    fun register(diagnosticFactory: DiagnosticFactory<*>, vararg factory: QuickFixFactory) {
        factories.putAll(diagnosticFactory, factory.map { it.asCangJieIntentionActionsFactory() })
        factory.filterIsInstance<UnresolvedReferenceQuickFixFactory>().ifNotEmpty {
            unresolvedReferenceActionFactories.putAll(
                diagnosticFactory,
                this.map { it.asCangJieIntentionActionsFactory() })
        }
    }

    fun register(diagnosticFactory: DiagnosticFactory<*>, vararg factory: CangJieIntentionActionsFactory) {
        factories.putAll(diagnosticFactory, factory.toList())
        factory.filterIsInstance<UnresolvedReferenceQuickFixFactory>().ifNotEmpty {
            unresolvedReferenceActionFactories.putAll(
                diagnosticFactory,
                this.map { it.asCangJieIntentionActionsFactory() })
        }
    }

    fun register(diagnosticFactory: DiagnosticFactory<*>, vararg action: IntentionAction) {
        actions.putAll(diagnosticFactory, action.toList())
    }

    fun getActionFactories(diagnosticFactory: DiagnosticFactory<*>): Collection<CangJieIntentionActionsFactory> {
        return factories.get(diagnosticFactory)
    }

    fun getUnresolvedReferenceActionFactories(diagnosticFactory: DiagnosticFactory<*>): Collection<CangJieIntentionActionsFactory> {
        return unresolvedReferenceActionFactories.get(diagnosticFactory)
    }

    fun getActions(diagnosticFactory: DiagnosticFactory<*>): Collection<IntentionAction> {
        return actions.get(diagnosticFactory)
    }

    fun getDiagnostics(factory: CangJieIntentionActionsFactory): Collection<DiagnosticFactory<*>> {
        return factories.keySet().filter { factory in factories.get(it) }
    }

    companion object {
        fun getInstance(): QuickFixes = service()
    }
}

fun QuickFixFactory.asCangJieIntentionActionsFactory(): CangJieIntentionActionsFactory = when (this) {
    is CangJieIntentionActionsFactory -> this
    is QuickFixesPsiBasedFactory<*> -> object : CangJieIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val psiElement = diagnostic.psiElement
            return createQuickFix(psiElement)
        }
    }

    else -> error("Unexpected QuickFixFactory ${this::class}")
}

