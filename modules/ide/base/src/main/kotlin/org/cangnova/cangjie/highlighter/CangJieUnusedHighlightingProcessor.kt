/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.daemon.impl.Divider
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.SuppressionUtil
import com.intellij.codeInspection.ex.InspectionProfileWrapper
import com.intellij.codeInspection.util.IntentionName
import com.intellij.concurrency.JobLauncher
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Predicates
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import com.intellij.util.Processor
import org.jetbrains.annotations.Nls
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.psi.*

/**
 * 基于仓颉 analysis-api 的未使用声明高亮处理器。
 *
 * 设计目标：
 * 1. 复用 IntelliJ `UnusedSymbol` inspection 的展示层与 severity 配置。
 * 2. 使用仓颉 analysis-api 建立本文件内的真实引用图，优先处理局部声明与私有声明。
 * 3. 对非局部声明再退回到 PSI 引用搜索，保证第一阶段能够稳定接入现有高亮管线。
 */
class CangJieUnusedHighlightingProcessor(private val cjFile: CjFile)  {
    private val enabled: Boolean
    private val deadCodeKey: HighlightDisplayKey?
    private val deadCodeInspection: LocalInspectionTool?
    private val deadCodeInfoType: HighlightInfoType.HighlightInfoTypeImpl?
    private val refHolder:CangJieRefsHolder = CangJieRefsHolder(cjFile)

    init {
        val project = cjFile.project
        val profile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile().let {
            InspectionProfileWrapper.getCustomInspectionProfileWrapper(cjFile)?.apply(it)?.inspectionProfile ?: it
        }
        deadCodeKey = HighlightDisplayKey.find("UnusedSymbol")
        deadCodeInspection = profile.getUnwrappedTool("UnusedSymbol", cjFile) as? LocalInspectionTool

        deadCodeInfoType = if (deadCodeKey == null) {
            null
        } else {
            val editorAttributes = profile.getEditorAttributes(deadCodeKey.shortName, cjFile)
            HighlightInfoType.HighlightInfoTypeImpl(
                profile.getErrorLevel(deadCodeKey, cjFile).severity,
                editorAttributes ?: HighlightInfoType.UNUSED_SYMBOL.getAttributesKey()
            )
        }
        enabled = deadCodeInspection != null
                && deadCodeInfoType != null
                && profile.isToolEnabled(deadCodeKey, cjFile)
    }

    private fun registerLocalReferences(session: CaSession, elements: List<PsiElement>){
        val registerDeclarationAccessVisitor = object : CjVisitorUnit() {
            override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
                if (expression.parent is CjValueArgumentName) {
                    return
                }

                val resolvedSymbol = with(session) { expression.resolveToSymbol() }

                when (resolvedSymbol) {
                    is CaLocalVariableSymbol,
                    is CaParameterSymbol,
                    is CaPropertySymbol -> refHolder.registerLocalRef((resolvedSymbol as CaDeclarationSymbol).psi)
                }

                if (expression.parent is CjThisExpression || expression.parent is CjSuperExpression) {
                    return
                }

                when (resolvedSymbol) {
                    is CaConstructorSymbol -> refHolder.registerLocalRef(resolvedSymbol.psi)
                    is CaClassLikeSymbol -> refHolder.registerLocalRef((resolvedSymbol as? CaDeclarationSymbol)?.psi)
                }
            }

            override fun visitBinaryExpression(expression: CjBinaryExpression) {
                val resolvedSymbol = with(session) { expression.operationReference.resolveToSymbol() } ?: return
                refHolder.registerLocalRef((resolvedSymbol as? CaDeclarationSymbol)?.psi)
            }

            override fun visitCallExpression(expression: CjCallExpression) {
                val callee = expression.calleeExpression ?: return
                if (callee is CjLambdaExpression || callee is CjCallExpression) return

                val resolvedSymbol = with(session) { expression.resolveToSymbol() } ?: return
                refHolder.registerLocalRef((resolvedSymbol as? CaDeclarationSymbol)?.psi)
            }

            override fun visitArrayAccessExpression(expression: CjArrayAccessExpression) {
                expression.references.forEach { reference ->
                    refHolder.registerLocalRef(reference.resolve())
                }
            }
        }

        for (declaration in elements) {
            declaration.accept(registerDeclarationAccessVisitor)
        }
    }

    private fun collectAndHighlightNamedElements(psiElements: List<PsiElement>, holder: HighlightInfoHolder) {
        val namedElements: MutableList<CjElement> = mutableListOf()
        val namedElementVisitor = object : CjVisitorUnit() {
            override fun visitNamedDeclaration(declaration: CjNamedDeclaration) {
                namedElements.add(declaration)
            }

            override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor) {
                namedElements.add(constructor)
            }

            override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
                namedElements.add(constructor)
            }

            override fun visitEnumConstructor(enumEntry: CjEnumConstructor) {
                namedElements.add(enumEntry)
            }
        }

        for (declaration in psiElements) {
            declaration.accept(namedElementVisitor)
        }

        JobLauncher.getInstance().invokeConcurrentlyUnderProgress(namedElements, ProgressManager.getGlobalProgressIndicator()) { declaration ->
            try {
                analyze(declaration) {
                    handleDeclaration(declaration, deadCodeInspection!!, deadCodeInfoType!!, deadCodeKey!!, holder)
                }
            } catch (_: StackOverflowError) {
                return@invokeConcurrentlyUnderProgress true
            }
            true
        }
    }

    internal fun collectHighlights(holder: HighlightInfoHolder){
        if (!enabled) return
        Divider.divideInsideAndOutsideAllRoots(cjFile, cjFile.textRange, holder.annotationSession.priorityRange, Predicates.alwaysTrue()) { dividedElements ->
            try {
                analyze(cjFile) {
                    val session = this
                    registerLocalReferences(session, dividedElements.inside())
                    registerLocalReferences(session, dividedElements.outside())
                }

                // highlight visible symbols first
                collectAndHighlightNamedElements(dividedElements.inside(), holder)
                collectAndHighlightNamedElements(dividedElements.outside(), holder)
            } catch (_: StackOverflowError) {
                // analysis-api 在项目结构尚未稳定时可能递归计算依赖；unused 高亮在此场景下直接跳过，避免污染主编辑链路。
            }
            true
        }
    }

    context(_: CaSession)
    private fun handleDeclaration(
        declaration: CjElement,
        deadCodeInspection: LocalInspectionTool,
        deadCodeInfoType: HighlightInfoType.HighlightInfoTypeImpl,
        deadCodeKey: HighlightDisplayKey,
        holder: HighlightInfoHolder
    ) {
        if (!isApplicableByPsi(declaration)) return
        if (refHolder.isUsedLocally(declaration)) return
        if (SuppressionUtil.inspectionResultSuppressed(declaration, deadCodeInspection)) return
        if (isEntryPoint(declaration)) return
        if (hasNonTrivialUsages(declaration, mustBeLocallyReferenced(declaration))) return

        val problemPsiElement = getProblemPsiElement(declaration) ?: return
        val displayName = declarationDisplayName(declaration) ?: return
        val message = CangJieBundle.message("0.1.is.never.used", declarationKindText(declaration), displayName)
        val builder = HighlightInfo.newHighlightInfo(deadCodeInfoType)
            .range(problemPsiElement)
            .descriptionAndTooltip(message)

        createQuickFixes(declaration).forEach { fix ->
            builder.registerFix(fix, null, null, null, deadCodeKey)
        }

        holder.add(builder.create())
    }

    private fun isApplicableByPsi(declaration: CjElement): Boolean = when (declaration) {
        is CjMainFunction -> false
        is CjParameter -> !declaration.isFunctionTypeParameter() && !declaration.isIgnoredByName()
        is CjNamedDeclaration -> !declaration.isIgnoredByName() && !declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)
        is CjPrimaryConstructor, is CjSecondaryConstructor, is CjEnumConstructor -> true
        else -> false
    }

    private fun mustBeLocallyReferenced(declaration: CjElement): Boolean = when (declaration) {
        is CjParameter -> !declaration.hasLetOrVar()
        is CjDeclaration -> declaration.hasModifier(CjTokens.PRIVATE_KEYWORD) || CjPsiUtil.isLocal(declaration)
        is CjEnumConstructor -> declaration.hasModifier(CjTokens.PRIVATE_KEYWORD)
        else -> false
    }

    private fun hasNonTrivialUsages(declaration: CjElement, localOnly: Boolean): Boolean {
        if (localOnly) return false

        val searchHelper = PsiSearchHelper.getInstance(cjFile.project)
        val useScope = searchHelper.getUseScope(declaration)
        return referenceExists(declaration, useScope) { reference ->
            !isTrivialReference(reference, declaration)
        }
    }

    private fun referenceExists(
        declaration: PsiElement,
        scope: SearchScope,
        predicate: (PsiReference) -> Boolean
    ): Boolean {
        return !ReferencesSearch.search(declaration, scope).forEach(Processor { reference ->
            !predicate(reference)
        })
    }

    private fun isTrivialReference(reference: PsiReference, declaration: PsiElement): Boolean {
        val refElement = reference.element
        if (PsiTreeUtil.isAncestor(declaration, refElement, false)) return true
        if (refElement.parent is CjValueArgumentName) return true
        return false
    }

    private fun getProblemPsiElement(declaration: CjElement): PsiElement? = when (declaration) {
        is CjNamedDeclaration -> declaration.nameIdentifier ?: declaration
        is CjPrimaryConstructor -> declaration.getConstructorKeyword() ?: declaration
        is CjSecondaryConstructor -> declaration.nameIdentifier ?: declaration.getConstructorKeyword() ?: declaration
        is CjEnumConstructor -> declaration.nameIdentifier ?: declaration
        else -> declaration
    }

    private fun isEntryPoint(declaration: CjElement): Boolean = declaration is CjMainFunction

    private fun createQuickFixes(declaration: CjElement): List<LocalQuickFixAndIntentionActionOnPsiElement> {
        val safeDeleteTarget = when (declaration) {
            is CjDeclaration -> declaration
            is CjEnumConstructor -> declaration
            else -> null
        } ?: return emptyList()

        return listOf(CangJieSafeDeleteFix(safeDeleteTarget))
    }

    private fun declarationDisplayName(declaration: CjElement): String? = when (declaration) {
        is CjNamedDeclaration -> declaration.name ?: declaration.nameIdentifier?.text
        is CjPrimaryConstructor -> declaration.getContainingTypeStatement().name ?: declaration.text
        is CjSecondaryConstructor -> declaration.name ?: declaration.getContainingTypeStatement().name ?: declaration.text
        is CjEnumConstructor -> declaration.name
        else -> null
    }

    private fun declarationKindText(declaration: CjElement): String = when (declaration) {
        is CjParameter -> "Parameter"
        is CjTypeParameter -> "Type parameter"
        is CjProperty -> "Property"
        is CjMainFunction, is CjNamedFunction -> "Function"
        is CjTypeAlias -> "Type alias"
        is CjInterface -> "Interface"
        is CjStruct -> "Struct"
        is CjEnum -> "Enum"
        is CjClass -> "Class"
        is CjPrimaryConstructor, is CjSecondaryConstructor -> "Constructor"
        is CjEnumConstructor -> "Enum constructor"
        else -> "Declaration"
    }

    private fun CjNamedDeclaration.isIgnoredByName(): Boolean = nameIdentifier?.text == "_"
}

class CangJieRefsHolder(private val containingFile: CjFile) {
    private val localRefs = mutableSetOf<PsiElement>()

    fun registerLocalRef(declaration: PsiElement?) {
        if (declaration != null && declaration.containingFile == containingFile) {
            localRefs += declaration
        }
    }

    fun isUsedLocally(declaration: CjElement): Boolean {
        if (localRefs.contains(declaration)) {
            return true
        }

        if (declaration is CjClass) {
            return declaration.primaryConstructor?.let(localRefs::contains) == true ||
                declaration.secondaryConstructors.any(localRefs::contains)
        }

        return false
    }
}

internal class CangJieSafeDeleteFix(declaration: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(declaration) {
    @Nls
    private val name: String = when (declaration) {
        is CjPrimaryConstructor -> CangJieBundle.message("safe.delete.primary.ctor.text.0", declaration.getContainingTypeStatement().name ?: declaration.text)
        is CjSecondaryConstructor -> CangJieBundle.message("safe.delete.secondary.ctor.text.0", declaration.getContainingTypeStatement().name ?: declaration.text)
        is CjParameter -> CangJieBundle.message("safe.delete.parameter.text.0", declaration.name ?: declaration.text)
        is CjEnumConstructor -> CangJieBundle.message("safe.delete.enum.entry.text.0", declaration.name ?: declaration.text)
        is CjNamedDeclaration -> CangJieBundle.message("safe.delete.text.0", declaration.name ?: declaration.text)
        else -> CangJieBundle.message("safe.delete.family")
    }

    override fun getText(): @IntentionName String = name

    override fun getFamilyName(): String = CangJieBundle.message("safe.delete.family")

    override fun startInWriteAction(): Boolean = false

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        SafeDeleteHandler.invoke(project, arrayOf(startElement), false)
    }
}
