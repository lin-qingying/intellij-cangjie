package com.linqingying.cangjie.ide.refactoring.move

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.imports.importableFqName
import com.linqingying.cangjie.ide.imports.isImported
import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.forEachDescendantOfType
import com.linqingying.cangjie.psi.psiUtil.getParentOfTypeAndBranch
import com.linqingying.cangjie.references.CjSimpleNameReference
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.caches.analyzeWithAllCompilerChecks
import com.linqingying.cangjie.resolve.calls.util.getResolvedCall
import com.linqingying.cangjie.resolve.descriptorUtil.getImportableDescriptor
import com.linqingying.cangjie.resolve.descriptorUtil.parents
import com.linqingying.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.utils.addIfNotNull
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Comparing
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

internal fun CjElement.getInternalReferencesToUpdateOnPackageNameChange(containerChangeInfo: MoveContainerChangeInfo): List<UsageInfo> {
    val usages = ArrayList<UsageInfo>()
    processInternalReferencesToUpdateOnPackageNameChange(this, containerChangeInfo) { expr, factory ->
        usages.addIfNotNull(factory(expr))
    }
    return usages
}

internal fun processInternalReferencesToUpdateOnPackageNameChange(
    element: CjElement,
    containerChangeInfo: MoveContainerChangeInfo,
    body: (originalRefExpr: CjSimpleNameExpression, usageFactory: CangJieUsageInfoFactory) -> Unit
) {
    val file = element.containingFile as? CjFile ?: return

    val importPaths = file.importDirectives.mapNotNull { it.importPath }

    tailrec fun isImported(descriptor: DeclarationDescriptor): Boolean {
        val fqName =
            DescriptorUtils.getFqName(descriptor).let { if (it.isSafe) it.toSafe() else return@isImported false }
        if (importPaths.any { fqName.isImported(it, false) }) return true

        return when (val containingDescriptor = descriptor.containingDeclaration) {
            is ClassDescriptor, is PackageViewDescriptor -> isImported(containingDescriptor)
            else -> false
        }
    }

    fun MoveContainerInfo.matches(decl: DeclarationDescriptor) = when (this) {
        is MoveContainerInfo.UnknownPackage -> decl is PackageViewDescriptor && decl.fqName == fqName
        is MoveContainerInfo.Package -> decl is PackageFragmentDescriptor && decl.fqName == fqName
        is MoveContainerInfo.Class -> decl is ClassDescriptor && decl.importableFqName == fqName
    }

    fun isCallableReference(reference: PsiReference): Boolean {
        return reference is CjSimpleNameReference && reference.element.getParentOfTypeAndBranch<CjCallableReferenceExpression> {
            callableReference
        } != null
    }

    fun processReference(refExpr: CjSimpleNameExpression, bindingContext: BindingContext): CangJieUsageInfoFactory? {
        val descriptor =
            bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.getImportableDescriptor() ?: return null
        val containingDescriptor = descriptor.containingDeclaration ?: return null

        val callableKind = (descriptor as? CallableMemberDescriptor)?.kind
        if (callableKind != null && callableKind != CallableMemberDescriptor.Kind.DECLARATION) return null

        // Special case for enum entry superclass references (they have empty text and don't need to be processed by the refactoring)
        if (refExpr.textRange.isEmpty) return null

//        if (descriptor is ClassDescriptor && descriptor.isInner && refExpr.parent is CjCallExpression) return null

        val isCallable = descriptor is CallableDescriptor
        val isExtension = isCallable && isExtensionRef(refExpr)
        val isCallableReference = isCallableReference(refExpr.mainReference)

        val declaration by lazy {
            val result = DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, descriptor) ?: return@lazy null



            result
        }

        if (isCallable) {
            if (!isCallableReference) {
                if (isExtension && containingDescriptor is ClassDescriptor) {
                    val dispatchReceiver = refExpr.getResolvedCall(bindingContext)?.dispatchReceiver
                    val implicitClass = (dispatchReceiver as? ImplicitClassReceiver)?.classDescriptor
                    val psiClass = implicitClass?.source?.getPsi()

                    if (dispatchReceiver != null) return null
                }
            }

            if (!isExtension) {
                val isCompatibleDescriptor = containingDescriptor is PackageFragmentDescriptor


                if (!isCompatibleDescriptor) return null
            }
        }

        if (!DescriptorUtils.getFqName(descriptor).isSafe) return null

        val (oldContainer, newContainer) = containerChangeInfo

        val containerFqName = descriptor.parents.mapNotNull {
            when {
                oldContainer.matches(it) -> oldContainer.fqName
                newContainer.matches(it) -> newContainer.fqName
                else -> null
            }
        }.firstOrNull()

        val isImported = isImported(descriptor)
        if (isImported && element is CjFile) return null

        val declarationNotNull = declaration ?: return null

        if (isExtension || containerFqName != null || isImported) return {
            CangJieMoveRenameUsage.createIfPossible(
                it.mainReference,
                declarationNotNull,
                addImportToOriginalFile = false,
                isInternal = true
            )
        }

        return null
    }

    @Suppress("DEPRECATION")
    val bindingContext = element.analyzeWithAllCompilerChecks().bindingContext
    element.forEachDescendantOfType<CjReferenceExpression> { refExpr ->
        if (refExpr !is CjSimpleNameExpression || refExpr.parent is CjThisExpression) return@forEachDescendantOfType

        processReference(refExpr, bindingContext)?.let { body(refExpr, it) }
    }
}


internal fun isExtensionRef(expr:CjSimpleNameExpression): Boolean {
//    val resolvedCall = expr.getResolvedCall(expr.analyze(BodyResolveMode.PARTIAL)) ?: return false
//    if (resolvedCall is VariableAsFunctionResolvedCall) {
//        return resolvedCall.variableCall.candidateDescriptor.isExtension || resolvedCall.functionCall.candidateDescriptor.isExtension
//    }
//    return resolvedCall.candidateDescriptor.isExtension
    return false
}

typealias CangJieUsageInfoFactory = (CjSimpleNameExpression) -> UsageInfo?
internal fun isQualifiable(callableReferenceExpression: CjCallableReferenceExpression): Boolean {
    return true
//    val receiverExpression = callableReferenceExpression.receiverExpression
//    val lhs = callableReferenceExpression.analyze(BodyResolveMode.PARTIAL)[BindingContext.DOUBLE_COLON_LHS, receiverExpression]
//    return lhs is DoubleColonLHS.Type
}

/**
 * Perform usage postprocessing and return non-code usages
 */
internal fun postProcessMoveUsages(
    usages: Collection<UsageInfo>,
    oldToNewElementsMapping: Map<PsiElement, PsiElement> = Collections.emptyMap(),
    shorteningMode: CjSimpleNameReference.ShorteningMode = CjSimpleNameReference.ShorteningMode.DELAYED_SHORTENING
): List<NonCodeUsageInfo> {
    val sortedUsages = usages.sortedWith(
        Comparator { o1, o2 ->
            val file1 = o1.virtualFile
            val file2 = o2.virtualFile
            if (Comparing.equal(file1, file2)) {
                val rangeInElement1 = o1.rangeInElement
                val rangeInElement2 = o2.rangeInElement
                if (rangeInElement1 != null && rangeInElement2 != null) {
                    return@Comparator rangeInElement2.startOffset - rangeInElement1.startOffset
                }
                return@Comparator 0
            }
            if (file1 == null) return@Comparator -1
            if (file2 == null) return@Comparator 1
            Comparing.compare(file1.path, file2.path)
        }
    )

    val nonCodeUsages = java.util.ArrayList<NonCodeUsageInfo>()

    val progressStep = 1.0 / sortedUsages.size
    val progressIndicator = ProgressManager.getInstance().progressIndicator
    progressIndicator?.isIndeterminate = false
    progressIndicator?.text = CangJieBundle.message("text.updating.usages.progress")
    usageLoop@ for ((i, usage) in sortedUsages.withIndex()) {
        progressIndicator?.fraction = (i + 1) * progressStep
        postProcessMoveUsage(usage, oldToNewElementsMapping, nonCodeUsages, shorteningMode)
    }
    progressIndicator?.text = ""

    return nonCodeUsages
}
private val LOG = Logger.getInstance("#com.linqingying.cangjie.ide.refactoring.move.MoveUtil")

internal fun mapToNewOrThis(e: PsiElement, oldToNewElementsMapping: Map<PsiElement, PsiElement>) = oldToNewElementsMapping[e] ?: e
private fun processReference(
    reference: PsiReference?,
    newElement: PsiElement,
    shorteningMode: CjSimpleNameReference.ShorteningMode,
    oldElement: PsiElement
) {
    try {
        when {
            reference is CjSimpleNameReference -> reference.bindToElement(newElement, shorteningMode)

            else -> reference?.bindToElement(newElement)
        }
    } catch (e: IncorrectOperationException) {
        LOG.warn("bindToElement not implemented for ${reference!!::class.qualifiedName}")
    }
}
private fun postProcessMoveUsage(
    usage: UsageInfo,
    oldToNewElementsMapping: Map<PsiElement, PsiElement>,
    nonCodeUsages: java.util.ArrayList<NonCodeUsageInfo>,
    shorteningMode: CjSimpleNameReference.ShorteningMode
) {
    if (usage is NonCodeUsageInfo) {
        nonCodeUsages.add(usage)
        return
    }

    if (usage !is MoveRenameUsageInfo) return

    val oldElement = usage.referencedElement!!
    val newElement = mapToNewOrThis(oldElement, oldToNewElementsMapping)

    when (usage) {
        is CangJieMoveRenameUsage.Deferred -> {
            val newUsage = usage.resolve(newElement) ?: return
            postProcessMoveUsage(newUsage, oldToNewElementsMapping, nonCodeUsages, shorteningMode)
        }

        is CangJieMoveRenameUsage.Unqualifiable -> {
            val file = with(usage) {
                if (addImportToOriginalFile) originalFile else mapToNewOrThis(
                    originalFile,
                    oldToNewElementsMapping
                )
            } as CjFile
            addDelayedImportRequest(newElement, file)
        }

        else -> {
            val reference = (usage.element as? CjSimpleNameExpression)?.mainReference ?: usage.reference
            processReference(reference, newElement, shorteningMode, oldElement)
        }
    }
}
internal fun addDelayedImportRequest(elementToImport: PsiElement, file: CjFile) {
    com.linqingying.cangjie.ide.codeinsight.shorten.addDelayedImportRequest(elementToImport, file)
}
