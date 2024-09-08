package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.huawei.cangjie.ide.CangJieIndicesHelper
import com.huawei.cangjie.ide.actions.ExpressionWeigher
import com.huawei.cangjie.ide.imports.*
import com.huawei.cangjie.ide.intentions.getCallableDescriptor
import com.huawei.cangjie.ide.quickfix.actions.*
import com.huawei.cangjie.ide.util.substituteExtensionIfCallable
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.CjPsiUtil.isSelectorInQualified
import com.huawei.cangjie.psi.psiUtil.isImportDirectiveExpression
import com.huawei.cangjie.references.mainReference
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.caches.analyze
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.caches.resolveImportReference
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.huawei.cangjie.resolve.calls.util.receiverTypesWithIndex
import com.huawei.cangjie.resolve.calls.util.singleLambdaArgumentExpression
import com.huawei.cangjie.resolve.descriptorUtil.fqNameSafe
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.scopes.getResolveScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.util.isSubtypeOf
import com.huawei.cangjie.utils.CallType
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
import com.huawei.cangjie.utils.safeAs
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.HintAction
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.refactoring.suggested.createSmartPointer
import com.intellij.util.Processors
import org.jetbrains.annotations.TestOnly


/**
 * Check possibility and perform fix for unresolved references.
 */
@IntellijInternalApi
abstract class ImportFixBase<T : CjExpression> protected constructor(
    expression: T,
    private val expressionToAnalyzePointer: SmartPsiElementPointer<CjExpression>?,
    factory: Factory
) : CangJieImportQuickFixAction<T>(expression), HintAction, HighPriorityAction {

    constructor(expression: T, factory: Factory) :
            this(expression, null, factory)

    constructor(expression: T, expressionToAnalyze: CjExpression, factory: Factory) :
            this(expression, expressionToAnalyze.createSmartPointer(), factory)

    private val project = expression.project

    private val modificationCountOnCreate = PsiModificationTracker.getInstance(project).modificationCount

    protected val expressionToAnalyze: CjExpression?
        get() = expressionToAnalyzePointer?.element ?: element

    protected lateinit var suggestions: Collection<FqName>

    @IntentionName
    private lateinit var text: String

    internal fun computeSuggestions() {
        val suggestionDescriptors = collectSuggestionDescriptors()
        suggestions = collectSuggestions(suggestionDescriptors)
        text = calculateText(suggestionDescriptors)
    }

    protected open val supportedErrors = factory.supportedErrors.toSet()

    protected abstract val importNames: Collection<Name>
    protected abstract fun getCallTypeAndReceiver(): CallTypeAndReceiver<*, *>?

    protected open fun calculateReceiverTypeFromDiagnostic(diagnostic: Collection<Diagnostic>): CangJieType? = null

    protected fun getReceiverTypeFromDiagnostic(): CangJieType? {
        // it is forbidden to keep `diagnostic` as class property as it leads to leakage of IdeaResolverForProject
        // when quick fix is about to apply we have to (re)calculate diagnostics
        val expression = expressionToAnalyze
        val bindingContext = expression?.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS) ?: return null
        val diagnostics = bindingContext.diagnostics.forElement(expression)
        return calculateReceiverTypeFromDiagnostic(diagnostics)
    }

    override fun showHint(editor: Editor): Boolean {
        val element = element?.takeIf(PsiElement::isValid) ?: return false

        if (isOutdated()) return false

        if (ApplicationManager.getApplication().isHeadlessEnvironment ||
            !DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled ||
            HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)
        ) return false

        if (suggestions.isEmpty()) return false

        return createAction(editor, element, suggestions).showHint()
    }

    @IntentionName
    private fun calculateText(suggestionDescriptors: Collection<DeclarationDescriptor>): String {
        val descriptors =
            suggestionDescriptors.mapTo(hashSetOf()) { it.original }.takeIf { it.isNotEmpty() } ?: return ""

        val ktFile = element?.getContainingCjFile() ?: return CangJieBundle.message("fix.import")
        val prioritizer = createPrioritizerForFile(ktFile)
        val expressionWeigher = ExpressionWeigher.createWeigher(element)

        val importInfos = descriptors.mapNotNull { descriptor ->
            val kind = when {
//                descriptor.isExtensionProperty -> ImportFixHelper.ImportKind.EXTENSION_PROPERTY
//                descriptor is PropertyDescriptor -> ImportFixHelper.ImportKind.PROPERTY
                descriptor is ClassConstructorDescriptor -> ImportFixHelper.ImportKind.CLASS
                descriptor is TypeAliasConstructorDescriptor -> ImportFixHelper.ImportKind.TYPE_ALIAS
                descriptor is FunctionDescriptor && descriptor.isOperator -> ImportFixHelper.ImportKind.OPERATOR
                descriptor is FunctionDescriptor && descriptor.isExtension -> ImportFixHelper.ImportKind.EXTENSION_FUNCTION
                descriptor is FunctionDescriptor -> ImportFixHelper.ImportKind.FUNCTION

                descriptor is ClassDescriptor -> ImportFixHelper.ImportKind.CLASS
                descriptor is TypeAliasDescriptor -> ImportFixHelper.ImportKind.TYPE_ALIAS
                else -> null
            } ?: return@mapNotNull null

            val name = buildString {
                descriptor.safeAs<CallableDescriptor>()?.let { callableDescriptor ->
                    val extensionReceiverParameter = callableDescriptor.extensionReceiverParameter
                    if (extensionReceiverParameter != null) {
                        extensionReceiverParameter.type.constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.name?.let {
                            append(it.asString())
                        }
                    } else {
                        callableDescriptor.containingDeclaration.safeAs<ClassifierDescriptor>()?.name?.let {
                            append(it.asString())
                        }
                    }
                }

                descriptor.name.takeUnless { it.isSpecial }?.let {
                    if (this.isNotEmpty()) append('.')
                    append(it.asString())
                }
            }
            val priority = createDescriptorPriority(prioritizer, expressionWeigher, descriptor)

            ImportFixHelper.ImportInfo(kind, name, priority)
        }
        return ImportFixHelper.calculateTextForFix(importInfos, suggestions)
    }

    override fun getText(): String = text

    override fun getFamilyName() = CangJieBundle.message("fix.import")

    override fun isAvailable(project: Project, editor: Editor?, file: CjFile): Boolean {
        return element != null && suggestions.isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor?, file: CjFile) {
        val element = element ?: return
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(editor!!, element, suggestions).execute()
        }
    }

    override fun startInWriteAction() = false

    private fun isOutdated() =
        modificationCountOnCreate != PsiModificationTracker.getInstance(project).modificationCount

    protected open fun createAction(
        editor: Editor,
        element: CjExpression,
        suggestions: Collection<FqName>
    ): CangJieAddImportAction {
        return createSingleImportAction(element.project, editor, element, suggestions)
    }

    override fun createImportAction(editor: Editor, file: CjFile): QuestionAction? =
        element?.let { createAction(editor, it, suggestions) }

    override fun createAutoImportAction(
        editor: Editor,
        file: CjFile,
        filterSuggestions: (Collection<FqName>) -> Collection<FqName>,
    ): QuestionAction? {
        val suggestions = filterSuggestions(suggestions)
        if (suggestions.isEmpty() || !ImportFixHelper.suggestionsAreFromSameParent(suggestions)) return null

        // we do not auto-import nested classes because this will probably add qualification into the text and this will confuse the user
        if (suggestions.any { suggestion ->
                file.resolveImportReference(suggestion).any(::isNestedClassifier)
            }) return null

        return element?.let { createAction(editor, it, suggestions) }
    }

    private fun isNestedClassifier(declaration: DeclarationDescriptor): Boolean =
        declaration is ClassifierDescriptor && declaration.containingDeclaration is ClassifierDescriptor

    private fun collectSuggestionDescriptors(): Collection<DeclarationDescriptor> {
        element?.takeIf(PsiElement::isValid)?.takeIf { it.containingFile is CjFile } ?: return emptyList()

        val callTypeAndReceiver = getCallTypeAndReceiver() ?: return emptyList()
        if (callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN) return emptyList()

        return importNames.flatMap { collectSuggestionsForName(it, callTypeAndReceiver) }
    }

    private fun collectSuggestions(suggestionDescriptors: Collection<DeclarationDescriptor>): Collection<FqName> =
        suggestionDescriptors
            .asSequence()
            .map { it.fqNameSafe }
            .distinct()
            .toList()

    private fun collectSuggestionsForName(
        name: Name,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>
    ): Collection<DeclarationDescriptor>    {
        val element = element ?: return emptyList()
        val expressionToAnalyze = expressionToAnalyze ?: return emptyList()
        val nameStr = name.asString()
        if (nameStr.isEmpty()) return emptyList()

        val file = element.getContainingCjFile()

        val bindingContext = expressionToAnalyze.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        if (!checkErrorStillPresent(bindingContext)) return emptyList()

        val searchScope = getResolveScope(file)

        val resolutionFacade = file.getResolutionFacade()

        fun isVisible(descriptor: DeclarationDescriptor): Boolean =
            descriptor.safeAs<DeclarationDescriptorWithVisibility>()
                ?.isVisible(element, callTypeAndReceiver.receiver as? CjExpression, bindingContext, resolutionFacade)
                ?: true

        val indicesHelper = CangJieIndicesHelper(resolutionFacade, searchScope, ::isVisible, file = file)

        var result = fillCandidates(nameStr, callTypeAndReceiver, bindingContext, indicesHelper)

        // for CallType.DEFAULT do not include functions if there is no parenthesis
        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val isCall = element.parent is CjCallExpression
            if (!isCall) {
                result = result.filter { it !is FunctionDescriptor }
            }
        }

//        result = result.filter { ImportFilter.shouldImport(file, it.fqNameSafe.asString()) }

        return if (result.size > 1)
            reduceCandidatesBasedOnDependencyRuleViolation(result, file)
        else
            result
    }

    private fun checkErrorStillPresent(bindingContext: BindingContext): Boolean {
        val errors = supportedErrors
        val elementsToCheckDiagnostics = elementsToCheckDiagnostics()
        for (psiElement in elementsToCheckDiagnostics) {
            if (bindingContext.diagnostics.forElement(psiElement).any { it.factory in errors }) return true
        }
        return false
    }

    protected open fun elementsToCheckDiagnostics(): Collection<PsiElement> = listOfNotNull(element)

    abstract fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor>

    private fun reduceCandidatesBasedOnDependencyRuleViolation(
        candidates: Collection<DeclarationDescriptor>, file: PsiFile
    ): Collection<DeclarationDescriptor> {
        val project = file.project
        val validationManager = DependencyValidationManager.getInstance(project)
        return candidates.filter {
            val targetFile =
                DescriptorToSourceUtilsIde.getAnyDeclaration(project, it)?.containingFile ?: return@filter true
            validationManager.getViolatorDependencyRules(file, targetFile).isEmpty()
        }
    }

    abstract class Factory : CangJieSingleIntentionActionFactory() {
        val supportedErrors: Collection<DiagnosticFactory<*>> by lazy { QuickFixes.getInstance().getDiagnostics(this) }

        override fun isApplicableForCodeFragment() = true

        abstract fun createImportAction(diagnostic: Diagnostic): ImportFixBase<*>?

        override fun areActionsAvailable(diagnostic: Diagnostic): Boolean {
            val element = diagnostic.psiElement
            return element is CjExpression && element.references.isNotEmpty()
        }

        open fun createImportActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<ImportFixBase<*>> =
            emptyList()

        final override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return try {
                createImportAction(diagnostic)?.also { it.computeSuggestions() }
            } catch (ex: CangJieExceptionWithAttachments) {
                // Sometimes fails with
                // <production sources for module light_idea_test_case> is a module[ModuleDescriptorImpl@508c55a2] is not contained in resolver...
                // TODO: remove try-catch when the problem is fixed
                if (AbstractImportFixInfo.IGNORE_MODULE_ERROR &&
                    ex.message?.contains("<production sources for module light_idea_test_case>") == true
                ) null
                else throw ex
            }
        }

        override fun doCreateActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> =
            createImportActionsForAllProblems(sameTypeDiagnostics).onEach { it.computeSuggestions() }
    }

    abstract class FactoryWithUnresolvedReferenceQuickFix : Factory(), UnresolvedReferenceQuickFixFactory
}


@IntellijInternalApi
abstract class OrdinaryImportFixBase<T : CjExpression>(expression: T, factory: Factory) :
    ImportFixBase<T>(expression, factory) {
    override fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> {
        val expression = element ?: return emptyList()

        val result = ArrayList<DeclarationDescriptor>()

        if (expression is CjSimpleNameExpression) {
            if (!expression.isImportDirectiveExpression() && !isSelectorInQualified(expression)) {
                ProgressManager.checkCanceled()
                val filterByCallType = callTypeAndReceiver.toFilter()

                indicesHelper.getClassifiersByName(expression, name).filterTo(result, filterByCallType)

                indicesHelper.getTopLevelCallablesByName(name).filterTo(result, filterByCallType)
            }
            if (callTypeAndReceiver.callType == CallType.OPERATOR) {
                val type = expression.getCallableDescriptor()?.returnType ?: getReceiverTypeFromDiagnostic()
                if (type != null) {
                    result.addAll(
                        indicesHelper.getCallableTopLevelExtensions(
                            callTypeAndReceiver,
                            listOf(type),
                            { it == name })
                    )
                }
            }
        }
        ProgressManager.checkCanceled()

        result.addAll(
            indicesHelper.getCallableTopLevelExtensions(
                callTypeAndReceiver,
                expression,
                bindingContext,
                findReceiverForDelegate(expression, callTypeAndReceiver.callType)
            ) { it == name }
        )

        val ktFile = element?.getContainingCjFile() ?: return emptyList()
        val importedFqNamesAsAlias = getImportedFqNamesAsAlias(ktFile)
        val (defaultImports, excludedImports) = ImportInsertHelperImpl.computeDefaultAndExcludedImports(ktFile)
        return result.filter {
            val descriptor =
                it.takeUnless { expression.parent is CjCallExpression && it.isSealed() } ?: return@filter false
            val importableFqName = descriptor.importableFqName ?: return@filter true
            val importPath = ImportPath(importableFqName, isAllUnder = false)
            !importPath.isImported(defaultImports, excludedImports) || importableFqName in importedFqNamesAsAlias
        }
    }

    private fun findReceiverForDelegate(expression: CjExpression, callType: CallType<*>): CangJieType? {
        if (callType != CallType.DELEGATE) return null

        val receiverTypeFromDiagnostic = getReceiverTypeFromDiagnostic()
        if (receiverTypeFromDiagnostic?.constructor is TypeVariableTypeConstructor) {
            if (receiverTypeFromDiagnostic == expression.getCallableDescriptor()?.returnType) {
                // the issue is that the whole lambda expression cannot be resolved,
                // but it's possible to analyze the last expression independently and try guessing the receiver
                return tryFindReceiverFromLambda(expression)
            }
        }

        return receiverTypeFromDiagnostic
    }

    private fun tryFindReceiverFromLambda(expression: CjExpression): CangJieType? {
        if (expression !is CjCallExpression) return null
        val lambdaExpression = expression.singleLambdaArgumentExpression() ?: return null

        val lastStatement = CjPsiUtil.getLastStatementInABlock(lambdaExpression.bodyExpression) ?: return null
        val bindingContext = lastStatement.analyze(bodyResolveMode = BodyResolveMode.PARTIAL)
        return bindingContext.getType(lastStatement)
    }

    private fun getImportedFqNamesAsAlias(ktFile: CjFile) =
        ktFile.importDirectives
            .filter { it.alias != null }
            .mapNotNull { it.importedFqName }
}

// This is required to be abstract to reduce bunch file size
@IntellijInternalApi
abstract class AbstractImportFix(expression: CjSimpleNameExpression, factory: Factory) :
    OrdinaryImportFixBase<CjSimpleNameExpression>(expression, factory) {

    override fun getCallTypeAndReceiver() = element?.let { CallTypeAndReceiver.detect(it) }

    private fun importNamesForMembers(): Collection<Name> {
        val element = element ?: return emptyList()

        if (element.getIdentifier() != null) {
            val name = element.getReferencedName()
            if (Name.isValidIdentifier(name)) {
                return listOf(Name.identifier(name))
            }
        }

        return emptyList()
    }

    override val importNames: Collection<Name> =
        ((element?.mainReference?.resolvesByNames ?: emptyList()) + importNamesForMembers()).distinct()

    private fun collectMemberCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> {
        val element = element ?: return emptyList()
        if (element.isImportDirectiveExpression()) return emptyList()

        val result = ArrayList<DeclarationDescriptor>()

        val filterByCallType = callTypeAndReceiver.toFilter()

        indicesHelper.getCangJieEnumsByName(name).filterTo(result, filterByCallType)

        ProgressManager.checkCanceled()

        val actualReceivers = getReceiversForExpression(element, callTypeAndReceiver, bindingContext)

        if (isSelectorInQualified(element) && actualReceivers.explicitReceivers.isEmpty()) {
            // If the element is qualified, and at the same time we haven't found any explicit
            // receiver, it means that the qualifier is not a value (for example, it might be a type name).
            // In this case we do not want to propose any importing fix, since it is impossible
            // to import a function which can be syntactically called on a non-value qualifier -
            // such function (for example, a static function) should be successfully resolved
            // without any import
            return emptyList()
        }

        val checkDispatchReceiver = when (callTypeAndReceiver) {
            is CallTypeAndReceiver.OPERATOR -> true
            else -> false
        }

        val processor = { descriptor: CallableDescriptor ->
            ProgressManager.checkCanceled()
            if (descriptor.canBeReferencedViaImport() && filterByCallType(descriptor)) {
                if (descriptor.extensionReceiverParameter != null) {
                    result.addAll(
                        descriptor.substituteExtensionIfCallable(
                            actualReceivers.explicitReceivers.ifEmpty { actualReceivers.allReceivers },
                            callTypeAndReceiver.callType
                        )
                    )
                } else if (descriptor.isValidByReceiversFor(actualReceivers, checkDispatchReceiver)) {
                    result.add(descriptor)
                }
            }
        }



        return result
    }

    /**
     * Currently at most one explicit receiver can be used in expression, but it can change in the future,
     * so we use `Collection` to represent explicit receivers.
     */
    private class Receivers(val explicitReceivers: Collection<CangJieType>, val allReceivers: Collection<CangJieType>)

    private fun getReceiversForExpression(
        element: CjSimpleNameExpression,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext
    ): Receivers {
        val resolutionFacade = element.getResolutionFacade()
        val actualReceiverTypes = callTypeAndReceiver
            .receiverTypesWithIndex(
                bindingContext, element,
                resolutionFacade.moduleDescriptor, resolutionFacade,
                stableSmartCastsOnly = false,
                withImplicitReceiversWhenExplicitPresent = true
            ).orEmpty()

        val explicitReceiverType = actualReceiverTypes.filterNot { it.implicit }

        return Receivers(
            explicitReceiverType.map { it.type },
            actualReceiverTypes.map { it.type }
        )
    }

    /**
     * This method accepts only callables with no extension receiver because it ignores generics
     * and does not perform any substitution.
     *
     * @return true iff [this] descriptor can be called given [actualReceivers] present in scope AND
     * passed [Receivers.explicitReceivers] are satisfied if present.
     */
    private fun CallableDescriptor.isValidByReceiversFor(
        actualReceivers: Receivers,
        checkDispatchReceiver: Boolean
    ): Boolean {
        require(extensionReceiverParameter == null) { "This method works only on non-extension callables, got $this" }

        val dispatcherReceiver = dispatchReceiverParameter.takeIf { checkDispatchReceiver }

        return if (dispatcherReceiver == null) {
            actualReceivers.explicitReceivers.isEmpty()
        } else {
            val typesToCheck = with(actualReceivers) { explicitReceivers.ifEmpty { allReceivers } }
            typesToCheck.any { it.isSubtypeOf(dispatcherReceiver.type) }
        }
    }

    override fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> =
        super.fillCandidates(name, callTypeAndReceiver, bindingContext, indicesHelper) + collectMemberCandidates(
            name,
            callTypeAndReceiver,
            bindingContext,
            indicesHelper
        )
}

object AbstractImportFixInfo {
    @Volatile
    internal var IGNORE_MODULE_ERROR = false

    @TestOnly
    fun ignoreModuleError(disposable: Disposable) {
        IGNORE_MODULE_ERROR = true
        Disposer.register(disposable) { IGNORE_MODULE_ERROR = false }
    }

}

private fun CallTypeAndReceiver<*, *>.toFilter() = { descriptor: DeclarationDescriptor ->
    callType.descriptorKindFilter.accepts(descriptor)
}

private fun CangJieIndicesHelper.getClassifiersByName(
    useSiteExpression: CjExpression,
    name: String,
): Collection<ClassifierDescriptor> = buildList {
    addAll(getClassesByName(useSiteExpression, name))

    processTopLevelTypeAliases({ it == name }, { add(it) })
}

private fun CangJieIndicesHelper.getClassesByName(
    expressionForPlatform: CjExpression,
    name: String
): Collection<ClassDescriptor> {

    val result = mutableListOf<ClassDescriptor>()
    val processor = Processors.cancelableCollectProcessor(result)
    // Enum entries should be contributed with members import fix
    processCangJieClasses(
        nameFilter = { it == name },
        // Enum entries should be contributed with members import fix
        psiFilter = { cjDeclaration -> cjDeclaration !is CjEnumEntry },
        kindFilter = { kind -> kind != ClassKind.ENUM_ENTRY },
        processor = processor::process
    )
    return result

}


@OptIn(IntellijInternalApi::class)
internal class ImportConstructorReferenceFix(expression: CjSimpleNameExpression) :
    ImportFixBase<CjSimpleNameExpression>(expression, MyFactory) {
    override fun getCallTypeAndReceiver() = null

    override fun fillCandidates(
        name: String,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        bindingContext: BindingContext,
        indicesHelper: CangJieIndicesHelper
    ): List<DeclarationDescriptor> {
        val expression = element ?: return emptyList()

        val filterByCallType = callTypeAndReceiver.toFilter()
        return indicesHelper.getClassifiersByName(expression, name)
            .asSequence()
            .flatMap { it.getConstructors() }
            .filter { it.importableFqName != null }
            .filter(filterByCallType)
            .toList()
    }

    override fun createAction(
        editor: Editor,
        element: CjExpression,
        suggestions: Collection<FqName>
    ): CangJieAddImportAction {
        return createSingleImportActionForConstructor(element.project, editor, element, suggestions)
    }

    override val importNames = element?.mainReference?.resolvesByNames ?: emptyList()

    companion object MyFactory : FactoryWithUnresolvedReferenceQuickFix() {
        override fun createImportAction(diagnostic: Diagnostic) =
            diagnostic.psiElement.safeAs<CjSimpleNameExpression>()?.let(::ImportConstructorReferenceFix)
    }
}
