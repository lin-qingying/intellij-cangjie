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

//package cn.cangnova.cangjie.ide.quickfix.importFix
//
//import cn.cangnova.cangjie.CangJieBundle
//import cn.cangnova.cangjie.ide.quickfix.AutoImportVariant
//import cn.cangnova.cangjie.ide.quickfix.CangJieAddImportActionInfo.executeListener
//import cn.cangnova.cangjie.ide.quickfix.CangJieImportQuickFixAction
//import cn.cangnova.cangjie.ide.quickfix.ImportFixHelper
//import cn.cangnova.cangjie.name.FqName
//import cn.cangnova.cangjie.psi.CjElement
//import cn.cangnova.cangjie.psi.CjFile
//import com.intellij.codeInsight.hint.HintManager
//import com.intellij.codeInsight.hint.QuestionAction
//import com.intellij.codeInsight.intention.HighPriorityAction
//import com.intellij.codeInspection.HintAction
//import com.intellij.codeInspection.util.IntentionName
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.editor.Editor
//import com.intellij.openapi.project.Project
//import com.intellij.psi.PsiElement
//import com.intellij.psi.statistics.StatisticsInfo
//import com.intellij.psi.statistics.StatisticsManager
//import com.intellij.psi.util.PsiModificationTracker
//import com.intellij.refactoring.suggested.startOffset
//import javax.swing.Icon
//
//class ImportQuickFix(
//    element: CjElement,
//    @IntentionName private val text: String,
//    private val importVariants: List<AutoImportVariant>
//) : CangJieImportQuickFixAction<CjElement>(element), HintAction, HighPriorityAction {
//    private data class SymbolBasedAutoImportVariant(
//        override val fqName: FqName,
//        override val declarationToImport: PsiElement?,
//        override val icon: Icon?,
//        override val debugRepresentation: String,
//        val statisticsInfo: StatisticsInfo,
//        val canNotBeImportedOnTheFly: Boolean,
//    ) : AutoImportVariant {
//        override val hint: String = fqName.asString()
//    }
//
//    init {
//        require(importVariants.isNotEmpty())
//    }
//
//    override fun getText(): String = text
//
//    override fun getFamilyName(): String = CangJieBundle.message("fix.import")
//
//    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
//        if (editor == null) return
//
//        createImportAction(editor, file)?.execute()
//    }
//
//    override fun createImportAction(editor: Editor, file: CjFile): QuestionAction? =
//        if (element != null) ImportQuestionAction(file.project, editor, file, importVariants) else null
//
//    override fun createAutoImportAction(
//        editor: Editor,
//        file: CjFile,
//        filterSuggestions: (Collection<FqName>) -> Collection<FqName>
//    ): QuestionAction? {
//        val filteredFqNames = filterSuggestions(importVariants.map { it.fqName }).toSet()
//        if (filteredFqNames.size != 1) return null
//
//        val singleSuggestion = importVariants.filter { it.fqName in filteredFqNames }.first()
//        if ((singleSuggestion as SymbolBasedAutoImportVariant).canNotBeImportedOnTheFly) return null
//
//        return ImportQuestionAction(file.project, editor, file, listOf(singleSuggestion), onTheFly = true)
//    }
//
//    override fun showHint(editor: Editor): Boolean {
//        val element = element ?: return false
//        if (
//            ApplicationManager.getApplication().isHeadlessEnvironment
//            || HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)
//        ) {
//            return false
//        }
//
//        val file = element.getContainingCjFile()
//
//        val elementRange = element.textRange
//        val autoImportHintText = CangJieBundle.message("fix.import.question", importVariants.first().fqName.asString())
//        val importAction = createImportAction(editor, file) ?: return false
//
//        HintManager.getInstance().showQuestionHint(
//            editor,
//            autoImportHintText,
//            elementRange.startOffset,
//            elementRange.endOffset,
//            importAction,
//        )
//
//        return true
//    }
//
//    private val modificationCountOnCreate: Long = PsiModificationTracker.getInstance(element.project).modificationCount
//
//    /**
//     * This is a safe-guard against showing hint after the quickfix have been applied.
//     *
//     * Inspired by the cn.cangnova.cangjie.ide.quickfix.ImportFixBase.isOutdated
//     */
//    private fun isOutdated(project: Project): Boolean {
//        return modificationCountOnCreate != PsiModificationTracker.getInstance(project).modificationCount
//    }
//
//    override fun isAvailable(project: Project, editor: Editor?, file: CjFile): Boolean =
//        !isOutdated(project)
//
//    private class ImportQuestionAction(
//        private val project: Project,
//        private val editor: Editor,
//        private val file: CjFile,
//        private val importVariants: List<AutoImportVariant>,
//        private val onTheFly: Boolean = false,
//    ) : QuestionAction {
//
//        init {
//            require(importVariants.isNotEmpty())
//        }
//
//        override fun execute(): Boolean {
//            file.executeListener?.onExecute(importVariants)
//            when (importVariants.size) {
//                1 -> {
//                    addImport(importVariants.single())
//                    return true
//                }
//
//                0 -> {
//                    return false
//                }
//
//                else -> {
//                    if (onTheFly) return false
//
//                    if (ApplicationManager.getApplication().isUnitTestMode) {
//                        addImport(importVariants.first())
//                        return true
//                    }
//                    ImportFixHelper.createListPopupWithImportVariants(project, importVariants, ::addImport).showInBestPositionFor(editor)
//
//                    return true
//                }
//            }
//        }
//
//        private fun addImport(importVariant: AutoImportVariant) {
//            require(importVariant is SymbolBasedAutoImportVariant)
//
//            StatisticsManager.getInstance().incUseCount(importVariant.statisticsInfo)
//
//            project.executeWriteCommand(QuickFixBundle.message("add.import")) {
//                file.addImport(importVariant.fqName)
//            }
//        }
//    }
//
//    companion object {
//        val invisibleReferenceFactory = diagnosticFixFactory(CjFirDiagnostic.InvisibleReference::class) { getFixes(it.psi) }
//
//        // this factory is used only for importing references on the fly; in all other cases import fixes for unresolved references
//        // are created by [cn.cangnova.cangjie.ide.codeInsight.CangJieFirUnresolvedReferenceQuickFixProvider]
//        val unresolvedReferenceFactory = diagnosticFixFactory(CjFirDiagnostic.UnresolvedReference::class) { getFixes(it.psi) }
//
//        context(CangJieAnalysisSession)
//        fun getFixes(diagnosticPsi: PsiElement): List<ImportQuickFix> {
//            val position = diagnosticPsi.containingFile.findElementAt(diagnosticPsi.startOffset)
//            val positionContext = position?.let { CangJiePositionContextDetector.detect(it) }
//
//            if (positionContext !is CangJieNameReferencePositionContext) return emptyList()
//
//            val indexProvider = CjSymbolFromIndexProvider.createForElement(positionContext.nameExpression)
//            val candidateProviders = buildList {
//                when (positionContext) {
//                    is CangJieSuperTypeCallNameReferencePositionContext,
//                    is CangJieTypeNameReferencePositionContext -> {
//                        add(ClassifierImportCandidatesProvider(positionContext, indexProvider))
//                    }
//
//                    is CangJieAnnotationTypeNameReferencePositionContext -> {
//                        add(AnnotationImportCandidatesProvider(positionContext, indexProvider))
//                    }
//
//                    is CangJieWithSubjectEntryPositionContext,
//                    is CangJieExpressionNameReferencePositionContext -> {
//                        add(CallableImportCandidatesProvider(positionContext, indexProvider))
//                        add(ClassifierImportCandidatesProvider(positionContext, indexProvider))
//                    }
//
//                    is CangJieInfixCallPositionContext -> {
//                        add(InfixCallableImportCandidatesProvider(positionContext, indexProvider))
//                    }
//
//                    is CDocLinkNamePositionContext -> {
//                        // TODO
//                    }
//
//                    is CangJieCallableReferencePositionContext -> {
//                        add(CallableImportCandidatesProvider(positionContext, indexProvider))
//                        add(ConstructorReferenceImportCandidatesProvider(positionContext, indexProvider))
//                    }
//
//                    is CangJieImportDirectivePositionContext,
//                    is CangJiePackageDirectivePositionContext,
//                    is CangJieSuperReceiverNameReferencePositionContext,
//                    is CDocParameterNamePositionContext -> {
//                    }
//                }
//            }
//
//            val candidates = candidateProviders.flatMap { it.collectCandidates() }
//            val quickFix = createImportFix(positionContext.nameExpression, candidates)
//
//            return listOfNotNull(quickFix)
//        }
//
//        private val renderer: CjDeclarationRenderer = CjDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
//            modifiersRenderer = modifiersRenderer.with {
//                visibilityProvider = CjRendererVisibilityModifierProvider.WITH_IMPLICIT_VISIBILITY
//            }
//            annotationRenderer = annotationRenderer.with {
//                annotationFilter = CjRendererAnnotationsFilter.NONE
//            }
//            returnTypeFilter = CjCallableReturnTypeFilter.ALWAYS
//        }
//
//
//        context(CangJieAnalysisSession)
//        private fun renderSymbol(symbol: CjDeclarationSymbol): String = prettyPrint {
//            val fqName = symbol.getFqName()
//            if (symbol is CjNamedClassOrObjectSymbol) {
//                append("class $fqName")
//            } else {
//                renderer.renderDeclaration(symbol, printer = this)
//            }
//
//            when (symbol) {
//                is CjCallableSymbol -> symbol.callableIdIfNonLocal?.packageName
//                is CjClassLikeSymbol -> symbol.classIdIfNonLocal?.packageFqName
//                else -> null
//            }?.let { packageName ->
//                append(" defined in ${packageName.asString()}")
//                symbol.psi?.containingFile?.let { append(" in file ${it.name}") }
//            }
//        }
//
//        context(CangJieAnalysisSession)
//        private fun createImportFix(
//            position: CjElement,
//            importCandidateSymbols: List<CjDeclarationSymbol>,
//        ): ImportQuickFix? {
//            if (importCandidateSymbols.isEmpty()) return null
//
//            val containingCjFile = position.containingCjFile
//
//            val analyzerServices = containingCjFile.platform.findAnalyzerServices(position.project)
//            val defaultImports = analyzerServices.getDefaultImports(position.languageVersionSettings, includeLowPriorityImports = true)
//            val excludedImports = analyzerServices.excludedImports
//
//            val isImported = { fqName: FqName -> ImportPath(fqName, isAllUnder = false).isImported(defaultImports, excludedImports) }
//            val importPrioritizer = ImportPrioritizer(containingCjFile, isImported)
//            val expressionImportWeigher = ExpressionImportWeigher.createWeigher(position)
//
//            val sortedImportCandidateSymbolsWithPriorities = importCandidateSymbols
//                .map { it to createPriorityForImportableSymbol(importPrioritizer, expressionImportWeigher, it) }
//                .sortedBy { (_, priority) -> priority }
//
//            val sortedImportInfos = sortedImportCandidateSymbolsWithPriorities.mapNotNull { (candidateSymbol, priority) ->
//                val kind = candidateSymbol.getImportKind() ?: return@mapNotNull null
//                val name = candidateSymbol.getImportName()
//                ImportFixHelper.ImportInfo(kind, name, priority)
//            }
//
//            val text = ImportFixHelper.calculateTextForFix(
//                sortedImportInfos,
//                suggestions = sortedImportCandidateSymbolsWithPriorities.map { (symbol, _) -> symbol.getFqName() }.distinct()
//            )
//
//            val implicitReceiverTypes = containingCjFile.getScopeContextForPosition(position).implicitReceivers.map { it.type }
//            // don't import callable on the fly as it might be unresolved because of an erroneous implicit receiver
//            val doNotImportCallablesOnFly = implicitReceiverTypes.any { it is CjErrorType }
//
//            val sortedImportVariants = sortedImportCandidateSymbolsWithPriorities
//                .map { (symbol, priority) ->
//                    SymbolBasedAutoImportVariant(
//                        symbol.getFqName(),
//                        symbol.psi,
//                        getIconFor(symbol),
//                        renderSymbol(symbol),
//                        priority.statisticsInfo,
//                        symbol.doNotImportOnTheFly(doNotImportCallablesOnFly),
//                    )
//                }
//
//            return ImportQuickFix(position, text, sortedImportVariants)
//        }
//
//        context(CangJieAnalysisSession)
//        private fun CjDeclarationSymbol.doNotImportOnTheFly(doNotImportCallablesOnFly: Boolean): Boolean = when (this) {
//            // don't import nested class on the fly because it will probably add qualification and confuse the user
//            is CjNamedClassOrObjectSymbol -> isNested()
//            is CjCallableSymbol -> doNotImportCallablesOnFly
//            else -> false
//        }
//
//        context(CangJieAnalysisSession)
//        private fun CjNamedClassOrObjectSymbol.isNested(): Boolean = getContainingSymbol() is CjNamedClassOrObjectSymbol
//
//        context(CangJieAnalysisSession)
//        private fun CjDeclarationSymbol.getImportKind(): ImportFixHelper.ImportKind? = when {
//            this is CjPropertySymbol && isExtension -> ImportFixHelper.ImportKind.EXTENSION_PROPERTY
//            this is CjPropertySymbol -> ImportFixHelper.ImportKind.PROPERTY
//            this is CjJavaFieldSymbol -> ImportFixHelper.ImportKind.PROPERTY
//
//            this is CjFunctionSymbol && isOperator -> ImportFixHelper.ImportKind.OPERATOR
//            this is CjFunctionSymbol && isExtension -> ImportFixHelper.ImportKind.EXTENSION_FUNCTION
//            this is CjFunctionSymbol -> ImportFixHelper.ImportKind.FUNCTION
//
//            this is CjNamedClassOrObjectSymbol && classKind.isObject -> ImportFixHelper.ImportKind.OBJECT
//            this is CjNamedClassOrObjectSymbol -> ImportFixHelper.ImportKind.CLASS
//            this is CjTypeAliasSymbol -> ImportFixHelper.ImportKind.TYPE_ALIAS
//
//            else -> null
//        }
//
//        context(CangJieAnalysisSession)
//        private fun CjDeclarationSymbol.getImportName(): String = buildString {
//            if (this@getImportName !is CjNamedSymbol) error("Unexpected anonymous declaration")
//
//            if (this@getImportName is CjCallableSymbol) {
//                val classSymbol = if (receiverType != null) receiverType?.expandedClassSymbol else originalContainingClassForOverride
//                classSymbol?.name?.let { append(it.asString()) }
//            }
//
//            if (this.isNotEmpty()) append('.')
//            append(name.asString())
//        }
//
//        context(CangJieAnalysisSession)
//        private fun CjDeclarationSymbol.getFqName(): FqName =
//            getFqNameIfPackageOrNonLocal() ?: error("Unexpected null for fully-qualified name of importable symbol")
//
//        context(CangJieAnalysisSession)
//        private fun createPriorityForImportableSymbol(
//            prioritizer: ImportPrioritizer,
//            expressionImportWeigher: ExpressionImportWeigher,
//            symbol: CjDeclarationSymbol
//        ): ImportPrioritizer.Priority =
//            prioritizer.Priority(
//                declaration = symbol.psi,
//                statisticsInfo = K2StatisticsInfoProvider.forDeclarationSymbol(symbol),
//                isDeprecated = symbol.deprecationStatus != null,
//                fqName = symbol.getFqName(),
//                expressionWeight = expressionImportWeigher.weigh(symbol),
//            )
//    }
//}
