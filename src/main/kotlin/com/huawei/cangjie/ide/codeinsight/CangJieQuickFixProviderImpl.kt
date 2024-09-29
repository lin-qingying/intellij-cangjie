package com.huawei.cangjie.ide.codeinsight

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.diagnostics.DiagnosticFactory
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.diagnostics.Severity
import com.huawei.cangjie.highlighter.CangJieQuickFixProvider
import com.huawei.cangjie.highlighter.RegisterQuickFixesLaterIntentionAction
import com.huawei.cangjie.ide.inspections.suppress.AnnotationHostKind
import com.huawei.cangjie.ide.inspections.suppress.CangJieSuppressIntentionAction
import com.huawei.cangjie.ide.quickfix.CangJieIntentionActionsFactory
import com.huawei.cangjie.ide.quickfix.QuickFixes
import com.huawei.cangjie.ide.quickfix.UnresolvedReferenceQuickFixFactory
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.caches.analyze
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.types.CangJieType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import java.lang.reflect.*
import java.util.*

class CangJieQuickFixProviderImpl : CangJieQuickFixProvider {
    override fun createQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> =
        createQuickFixes(sameTypeDiagnostics, true, false) { factory: DiagnosticFactory<*> ->
            QuickFixes.getInstance().getActionFactories(factory)
        }

    private fun Diagnostic.getRealDiagnosticFactory(): DiagnosticFactory<*> {
        return when (factory) {
            Errors.PLUGIN_ERROR -> Errors.PLUGIN_ERROR.cast(this).a.factory
            Errors.PLUGIN_WARNING -> Errors.PLUGIN_WARNING.cast(this).a.factory
            Errors.PLUGIN_INFO -> Errors.PLUGIN_INFO.cast(this).a.factory
            else -> factory
        }
    }

    private fun createQuickFixes(
        intentionActionsFactories: Collection<CangJieIntentionActionsFactory>,
        diagnostics: Collection<Diagnostic>,
        actions: MultiMap<Diagnostic, IntentionAction>,
        firstDiagnostic: Diagnostic?,
        replaceUnresolvedReferenceQuickFix: Boolean,
        unresolvedReferenceQuickFixOnly: Boolean
    ): MultiMap<Diagnostic, IntentionAction> {
        val first = diagnostics.first()
        for (intentionActionsFactory in intentionActionsFactories) {
            if ((unresolvedReferenceQuickFixOnly || replaceUnresolvedReferenceQuickFix) && intentionActionsFactory is UnresolvedReferenceQuickFixFactory) {
                if (
                // UnresolvedReferenceQuickFixUpdater works only when reference is available
                    first.psiElement.reference != null &&
                    (unresolvedReferenceQuickFixOnly || intentionActionsFactory.areActionsAvailable(first))
                ) {
                    actions.putValue(first, RegisterQuickFixesLaterIntentionAction)
                    if (unresolvedReferenceQuickFixOnly) break
                    continue
                }
            }
            if (unresolvedReferenceQuickFixOnly) {
                continue
            }
            val allProblemsActions = intentionActionsFactory.createActionsForAllProblems(diagnostics)
            if (allProblemsActions.isNotEmpty()) {
                actions.putValues(firstDiagnostic, allProblemsActions)
            } else {
                for (diagnostic in diagnostics) {
                    actions.putValues(diagnostic, intentionActionsFactory.createActions(diagnostic))
                }
            }
        }

        for (diagnostic in diagnostics) {
            val intentionActions = QuickFixes.getInstance().getActions(diagnostic.factory)
            if (intentionActions.isNotEmpty()) {
                actions.putValues(diagnostic, intentionActions)
            }
        }

        actions.values().forEach { NoDeclarationDescriptorsChecker.check(it::class.java) }

        return actions
    }

    private fun createQuickFixes(
        diagnostics: Collection<Diagnostic>,
        replaceUnresolvedReferenceQuickFix: Boolean,
        unresolvedReferenceQuickFixOnly: Boolean,
        intentionActionsFactories: (DiagnosticFactory<*>) -> Collection<CangJieIntentionActionsFactory>
    ): MultiMap<Diagnostic, IntentionAction> {
        val firstDiagnostic = diagnostics.minByOrNull { it.toString() }
        val factory = diagnostics.first().getRealDiagnosticFactory()

        val actions = MultiMap<Diagnostic, IntentionAction>()

        val actionsFactories = intentionActionsFactories(factory)
        return createQuickFixes(
            actionsFactories, diagnostics, actions, firstDiagnostic,
            replaceUnresolvedReferenceQuickFix, unresolvedReferenceQuickFixOnly
        )
    }

    override fun createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> {

        return createQuickFixes(sameTypeDiagnostics, true, true) { factory: DiagnosticFactory<*> ->
            QuickFixes.getInstance().getActionFactories(factory)
        }

    }

    override fun createUnresolvedReferenceQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> =
        createQuickFixes(sameTypeDiagnostics, false, false) { factory: DiagnosticFactory<*> ->
            QuickFixes.getInstance().getUnresolvedReferenceActionFactories(factory)
        }

    private fun collectDiagnosticsForElement(element: CjElement, severity: Severity): List<Diagnostic> {
        val file = element.containingFile
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        return bindingContext.diagnostics.filter {
            it.severity == severity && it.psiElement.containingFile == file && it.psiElement.textRange in element.textRange
        }
    }

    override fun createUnresolvedReferenceQuickFixesForElement(element: CjElement): Map<PsiElement, Sequence<IntentionAction>> {
        val diagnostics = collectDiagnosticsForElement(element, Severity.ERROR)

        return diagnostics.groupBy { it.psiElement }.mapValues { (_, sameElementDiagnostics) ->
            sameElementDiagnostics.groupBy { it.factory }.asSequence().flatMap { (_, sameTypeDiagnostics) ->
                createUnresolvedReferenceQuickFixes(sameTypeDiagnostics).values()
            }
        }

    }

    override fun createSuppressFix(
        element: CjElement,
        suppressionKey: String,
        hostKind: AnnotationHostKind
    ): SuppressIntentionAction {
        return CangJieSuppressIntentionAction(element, suppressionKey, hostKind)


    }
}

private object NoDeclarationDescriptorsChecker {
    private val LOG = Logger.getInstance(NoDeclarationDescriptorsChecker::class.java)

    private val checkedQuickFixClasses = Collections.synchronizedSet(HashSet<Class<*>>())

    fun check(quickFixClass: Class<*>) {
        if (!checkedQuickFixClasses.add(quickFixClass)) return

        for (field in quickFixClass.declaredFields) {
            checkType(field.genericType, field)
        }

        quickFixClass.superclass?.let { check(it) }
    }

    private fun checkType(type: Type, field: Field) {
        when (type) {
            is Class<*> -> {
                if (
                    DeclarationDescriptor::class.java.isAssignableFrom(type) ||
                    CangJieType::class.java.isAssignableFrom(type) ||
                    Diagnostic::class.java.isAssignableFrom(type)
                ) {
                    LOG.error(
                        "QuickFix class ${field.declaringClass.name} contains field ${field.name} that holds ${type.simpleName}. "
                                + "This leads to holding too much memory through this quick-fix instance. "
                                + "Possible solution can be wrapping it using CangJieIntentionActionFactoryWithDelegate."
                    )
                }

                if (IntentionAction::class.java.isAssignableFrom(type)) {
                    check(type)
                }
            }

            is GenericArrayType -> checkType(type.genericComponentType, field)

            is ParameterizedType -> {
                if (Collection::class.java.isAssignableFrom(type.rawType as Class<*>)) {
                    type.actualTypeArguments.forEach { checkType(it, field) }
                }
            }

            is WildcardType -> type.upperBounds.forEach { checkType(it, field) }
        }
    }
}
