package com.huawei.cangjie.ide.quickfix

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.descriptors.DiagnosticFactory
import com.huawei.cangjie.utils.ifNotEmpty
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service


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

