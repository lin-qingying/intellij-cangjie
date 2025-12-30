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

package org.cangnova.cangjie.highlighter

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.cangnova.cangjie.diagnostics.Severity
import org.cangnova.cangjie.highlighter.CangJieQuickFixProvider
import org.cangnova.cangjie.highlighter.RegisterQuickFixesLaterIntentionAction
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.caches.analyze
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.types.CangJieType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.cangnova.cangjie.diagnostics.infos.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.inspections.suppress.AnnotationHostKind
import org.cangnova.cangjie.inspections.suppress.CangJieSuppressIntentionAction
import org.cangnova.cangjie.quickfix.CangJieIntentionActionsFactory
import org.cangnova.cangjie.quickfix.QuickFixes
import java.lang.reflect.*
import java.util.*

class CangJieQuickFixProviderImpl : CangJieQuickFixProvider {
    override fun createQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> =
        createQuickFixes(sameTypeDiagnostics, true, false) { factory: DiagnosticFactory<*> ->
            QuickFixes.getInstance().getActionFactories(factory)
        }

    private fun Diagnostic.getRealDiagnosticFactory(): DiagnosticFactory<*> {
        return when (factory) {
            PLUGIN_ERROR -> PLUGIN_ERROR.cast(this).a.factory
            PLUGIN_WARNING -> PLUGIN_WARNING.cast(this).a.factory
            PLUGIN_INFO -> PLUGIN_INFO.cast(this).a.factory
            else -> factory
        }
    }

    /**
     * 创建快速修复项。
     *
     * 该函数根据诊断信息和意图操作工厂生成相应的快速修复项，并将它们添加到 [actions] 中。
     *
     * @param intentionActionsFactories 意图操作工厂的集合，用于生成快速修复项。
     * @param diagnostics 诊断信息的集合，表示需要处理的问题。
     * @param actions 存储诊断信息与快速修复项映射关系的多值映射表。
     * @param firstDiagnostic 第一个诊断信息，用于某些全局修复操作。
     * @param replaceUnresolvedReferenceQuickFix 是否替换未解析引用的快速修复项。
     * @param unresolvedReferenceQuickFixOnly 是否仅处理未解析引用的快速修复项。
     * @return 返回包含所有生成的快速修复项的多值映射表。
     */
    private fun createQuickFixes(
        intentionActionsFactories: Collection<CangJieIntentionActionsFactory>,
        diagnostics: Collection<Diagnostic>,
        actions: MultiMap<Diagnostic, IntentionAction>,
        firstDiagnostic: Diagnostic?,
        replaceUnresolvedReferenceQuickFix: Boolean,
        unresolvedReferenceQuickFixOnly: Boolean
    ): MultiMap<Diagnostic, IntentionAction> {

        // 获取第一个诊断信息，用于全局修复操作
        val first = diagnostics.first()

        // 遍历每个意图操作工厂，生成对应的快速修复项
        for (intentionActionsFactory in intentionActionsFactories) {
            // 如果仅处理未解析引用的快速修复项或替换未解析引用的快速修复项，并且当前工厂是 UnresolvedReferenceQuickFixFactory 类型
//            if ((unresolvedReferenceQuickFixOnly || replaceUnresolvedReferenceQuickFix) && intentionActionsFactory is UnresolvedReferenceQuickFixFactory) {
//                // 只有当引用存在并且满足条件时才注册快速修复项
//                if (first.psiElement.reference != null && (unresolvedReferenceQuickFixOnly || intentionActionsFactory.areActionsAvailable(
//                        first
//                    ))
//                ) {
//                    actions.putValue(first, RegisterQuickFixesLaterIntentionAction)
//                    if (unresolvedReferenceQuickFixOnly) break
//                    continue
//                }
//            }

            // 如果仅处理未解析引用的快速修复项，则跳过其他类型的修复项
//            if (unresolvedReferenceQuickFixOnly) {
//                continue
//            }

            // 尝试为所有问题创建修复项
            val allProblemsActions = intentionActionsFactory.createActionsForAllProblems(diagnostics)
            if (allProblemsActions.isNotEmpty()) {
                actions.putValues(firstDiagnostic, allProblemsActions)
            } else {
                // 如果没有全局修复项，则逐个诊断信息创建修复项
                for (diagnostic in diagnostics) {
                    actions.putValues(diagnostic, intentionActionsFactory.createActions(diagnostic))
                }
            }
        }

        // 从 QuickFixes 实例中获取额外的修复项并添加到 actions 中
        for (diagnostic in diagnostics) {
            val intentionActions = QuickFixes.getInstance().getActions(diagnostic.factory)
            if (intentionActions.isNotEmpty()) {
                actions.putValues(diagnostic, intentionActions)
            }
        }

        // 对所有修复项进行检查
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
