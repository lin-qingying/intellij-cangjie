package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.scopes.ImportingScope
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.findClassifier
import com.huawei.cangjie.resolve.scopes.receivers.ClassQualifier
import com.huawei.cangjie.resolve.scopes.receivers.Qualifier
import com.huawei.cangjie.resolve.scopes.receivers.expression
import com.huawei.cangjie.resolve.source.CangJieSourceElement
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.types.expressions.isWithoutValueArguments
import com.huawei.cangjie.utils.SmartList
import com.intellij.openapi.util.Key
import com.intellij.psi.impl.source.DummyHolder


//private fun resolveQualifierReferenceTarget(
//    qualifier: Qualifier,
//    selector: DeclarationDescriptor?,
//    context: ExpressionTypingContext
//): DeclarationDescriptor {
//    if (qualifier is TypeParameterQualifier) {
//        return qualifier.descriptor
//    }
//
//    val selectorContainer = when (selector) {
//        is ConstructorDescriptor ->
//            selector.containingDeclaration.containingDeclaration
//        else ->
//            selector?.containingDeclaration
//    }
//
//    if (qualifier is PackageQualifier &&
//        (selectorContainer is PackageFragmentDescriptor || selectorContainer is PackageViewDescriptor) &&
//        DescriptorUtils.getFqName(qualifier.descriptor) == DescriptorUtils.getFqName(selectorContainer)
//    ) {
//        return qualifier.descriptor
//    }
//
//    // TODO make decisions about short reference to companion object somewhere else
//    if (qualifier is ClassifierQualifier) {
//        val classifier = qualifier.descriptor
//        val selectorIsCallable = selector is CallableDescriptor &&
//                (selector.dispatchReceiverParameter != null || selector.extensionReceiverParameter != null)
//        // TODO simplify this code.
//        // Given a class qualifier in expression position,
//        // it should provide a proper REFERENCE_TARGET (with type),
//        // and, in case of implicit companion object reference, SHORT_REFERENCE_TO_COMPANION_OBJECT.
//        val receiverClassifierDescriptor = classifier.getCallableReceiverDescriptorRetainingTypeAliasReference()
//        if (selectorIsCallable && receiverClassifierDescriptor != null) {
//            val classValueTypeDescriptor = classifier.classValueTypeDescriptor!!
//            context.trace.record(BindingContext.REFERENCE_TARGET, qualifier.referenceExpression, receiverClassifierDescriptor)
//            context.trace.recordType(qualifier.expression, classValueTypeDescriptor.defaultType)
//            if (classifier.hasCompanionObject) {
//                context.trace.record(BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, qualifier.referenceExpression, classifier)
//            }
//            return classValueTypeDescriptor
//        }
//    }
//
//    return qualifier.descriptor
//}

//fun resolveQualifierAsReceiverInExpression(
//    qualifier: Qualifier, selector: DeclarationDescriptor?, context: ExpressionTypingContext
//): DeclarationDescriptor {
//    val referenceTarget = resolveQualifierReferenceTarget(qualifier, selector, context)
//
//    if (referenceTarget is TypeParameterDescriptor) {
//        context.trace.report(Errors.TYPE_PARAMETER_ON_LHS_OF_DOT.on(qualifier.referenceExpression, referenceTarget))
//    }
//
//    return referenceTarget
//}

class QualifiedExpressionResolver {

    class ExpressionQualifierPart(
        name: Name,
        override val expression: CjSimpleNameExpression,
        typeArguments: CjTypeArgumentList? = null
    ) : QualifierPart(name, typeArguments, CangJieLookupLocation(expression)) {
        constructor(expression: CjSimpleNameExpression) : this(expression.getReferencedNameAsName(), expression)

        override fun component2() = expression
    }


    open class QualifierPart(
        val name: Name,
        val typeArguments: CjTypeArgumentList? = null,
        val location: LookupLocation = NoLookupLocation.FOR_DEFAULT_IMPORTS
    ) {
        open val expression: CjSimpleNameExpression? get() = null

        operator fun component1() = name
        open operator fun component2() = expression
        operator fun component3() = typeArguments
    }

    private fun CjImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
        when (this) {
            is CjImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()
            is CjImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
        }

    private fun CjExpression.asQualifierPartList(doubleColonLHS: Boolean = false): List<ExpressionQualifierPart> {
        val result = SmartList<ExpressionQualifierPart>()

        fun addQualifierPart(expression: CjExpression?): Boolean {
            if (expression is CjSimpleNameExpression) {
                result.add(ExpressionQualifierPart(expression))
                return true
            }
            if (doubleColonLHS && expression is CjCallExpression && expression.isWithoutValueArguments) {
                val simpleName = expression.calleeExpression
                if (simpleName is CjSimpleNameExpression) {
                    result.add(
                        ExpressionQualifierPart(
                            simpleName.getReferencedNameAsName(),
                            simpleName,
                            expression.typeArgumentList
                        )
                    )
                    return true
                }
            }
            return false
        }

        var expression: CjExpression? = this
        while (true) {
            if (addQualifierPart(expression)) break
            if (expression !is CjQualifiedExpression) break

            addQualifierPart(expression.selectorExpression)

            expression = expression.receiverExpression
        }

        return result.asReversed()
    }

    private fun computePackageFragmentToCheck(
        containingFile: CjFile,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): PackageFragmentDescriptor? =
        when {
            containingFile.suppressDiagnosticsInDebugMode() -> null

            packageFragmentForVisibilityCheck is DeclarationDescriptorWithSource &&
                    packageFragmentForVisibilityCheck.source == SourceElement.NO_SOURCE -> {

                PackageFragmentWithCustomSource(
                    packageFragmentForVisibilityCheck,
                    CangJieSourceElement(containingFile)
                )
            }

            else -> packageFragmentForVisibilityCheck
        }

    private fun doProcessImportReference(
        importDirective: CjImportInfo,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        excludedImportNames: Collection<FqName>,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? { // null if some error happened
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val importedReference = importDirective.importContent ?: return null
        val path = importedReference.asQualifierPartList()
        val lastPart = path.lastOrNull() ?: return null
        val packageFragmentForCheck =
            if (importDirective is CjImportDirective)
                computePackageFragmentToCheck(importDirective.getContainingCjFile(), packageFragmentForVisibilityCheck)
            else
                null

        if (importDirective.isAllUnder) {
            val packageOrClassDescriptor = resolveToPackageOrClass(
                path, moduleDescriptor, trace, packageFragmentForCheck,
                scopeForFirstPart = null, position = QualifierPosition.IMPORT
            ).classDescriptorFromTypeAlias() ?: return null

            if (packageOrClassDescriptor is ClassDescriptor && packageOrClassDescriptor.kind.isSingleton && lastPart.expression != null) {
                trace.report(
                    Errors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON.on(
                        lastPart.expression!!,
                        packageOrClassDescriptor
                    )
                ) // todo report on star
                return null
            }
            TODO()

//            return AllUnderImportScope.create(packageOrClassDescriptor, excludedImportNames)
        } else {
            return processSingleImport(
                moduleDescriptor,
                trace,
                importDirective,
                path,
                lastPart,
                packageFragmentForCheck
            )
        }
    }

    private fun processSingleImport(
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        importDirective: CjImportInfo,
        path: List<QualifierPart>,
        lastPart: QualifierPart,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        val aliasName = importDirective.importedName
        if (aliasName == null) {
            // import kotlin.
            resolveToPackageOrClass(
                path,
                moduleDescriptor,
                trace,
                packageFragmentForVisibilityCheck,
                scopeForFirstPart = null,
                position = QualifierPosition.IMPORT
            )
            return null
        }

        val resolvedDescriptor = resolveToPackageOrClass(
            path.subList(0, path.size - 1), moduleDescriptor, trace,
            packageFragmentForVisibilityCheck, scopeForFirstPart = null, position = QualifierPosition.IMPORT
        ) ?: return null

        val packageOrClassDescriptor =
            (resolvedDescriptor as? TypeAliasDescriptor)?.let { it.classDescriptor ?: return null }
                ?: resolvedDescriptor

        TODO()
//
//        return LazyExplicitImportScope(
//
//            packageOrClassDescriptor,
//            packageFragmentForVisibilityCheck,
//            lastPart.name,
//            aliasName,
//            CallOnceFunction(Unit) { candidates ->
//                if (candidates.isNotEmpty()) {
//                    storeResult(
//                        trace,
//                        lastPart.expression,
//                        candidates,
//                        packageFragmentForVisibilityCheck,
//                        position = QualifierPosition.IMPORT,
//                        isQualifier = false
//                    )
//                } else {
//                    tryResolveDescriptorsWhichCannotBeImported(
//                        trace,
//                        moduleDescriptor,
//                        packageOrClassDescriptor,
//                        lastPart
//                    )
//                }
//            }
//        )
    }

    private fun DeclarationDescriptor?.classDescriptorFromTypeAlias(): DeclarationDescriptor? {
        return if (this is TypeAliasDescriptor) classDescriptor else this
    }

    private fun resolveToPackageOrClassPrefix(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition,
        isValue: ((CjSimpleNameExpression) -> Boolean)? = null
    ): Pair<DeclarationDescriptor?, Int> {
//        if (resolveInIDEMode(path)) {
//            return resolveToPackageOrClassPrefix(
//                path.subList(1, path.size),
//                moduleDescriptor,
//                trace,
//                shouldBeVisibleFrom,
//                scopeForFirstPart = null,
//                position = position,
//                isValue = null
//            ).let { it.first to it.second + 1 }
//        }

        if (path.isEmpty()) {
            return Pair(moduleDescriptor.getPackage(FqName.ROOT), 0)
        }

        val firstPart = path.first()

        if (position == QualifierPosition.EXPRESSION) {
            // In expression position, value wins against classifier (and package).
            // If we see a function or variable (possibly ambiguous),
            // tell resolver we have no qualifier and let it perform the context-dependent resolution.
            if (scopeForFirstPart != null && isValue != null && firstPart.expression != null && isValue(firstPart.expression!!)) {
                return Pair(null, 0)
            }
        }

        val classifierDescriptor = scopeForFirstPart?.findClassifier(firstPart.name, firstPart.location)

//        if (classifierDescriptor != null) {
//            storeResult(trace, firstPart.expression, classifierDescriptor, shouldBeVisibleFrom, position)
//        }

        val (prefixDescriptor, nextIndexAfterPrefix) =
            if (classifierDescriptor != null)
                Pair(classifierDescriptor, 1)
            else
                moduleDescriptor.quickResolveToPackage(path, trace, position)

        var currentDescriptor: DeclarationDescriptor? = prefixDescriptor
        for (qualifierPartIndex in nextIndexAfterPrefix until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                    is TypeAliasDescriptor -> // TODO type aliases as qualifiers? (would break some assumptions in TypeResolver)
                        null

                    is ClassDescriptor ->
                        currentDescriptor.getContributedClassifier(qualifierPart)

                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(currentDescriptor.fqName.child(qualifierPart.name))
                            } else null
                        if (packageView != null && !packageView.isEmpty()) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.getContributedClassifier(
                                qualifierPart.name,
                                qualifierPart.location
                            )
                        }
                    }

                    else ->
                        null
                }

            // If we are in expression, this name can denote a value (not a package or class).
            if (!(position == QualifierPosition.EXPRESSION && nextPackageOrClassDescriptor == null)) {
                storeResult(
                    trace,
                    qualifierPart.expression,
                    nextPackageOrClassDescriptor,
                    shouldBeVisibleFrom,
                    position
                )
            }

            if (nextPackageOrClassDescriptor == null) {
                return Pair(currentDescriptor, qualifierPartIndex)
            }

            currentDescriptor = nextPackageOrClassDescriptor
        }

        return Pair(currentDescriptor, path.size)
    }

    fun ClassDescriptor.getContributedClassifier(qualifierPart: QualifierPart) =
        unsubstitutedInnerClassesScope.getContributedClassifier(qualifierPart.name, qualifierPart.location)

    private fun ModuleDescriptor.quickResolveToPackage(
        path: List<QualifierPart>,
        trace: BindingTrace,
        position: QualifierPosition
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize =
            path.indexOfFirst { it.typeArguments != null }.let { if (it == -1) path.size else it + 1 }
        var fqName = FqName.fromSegments(path.subList(0, possiblePackagePrefixSize).map { it.name.asString() })

        var prefixSize = possiblePackagePrefixSize
        while (!fqName.isRoot) {
            val packageDescriptor = getPackage(fqName)
            if (!packageDescriptor.isEmpty()) {
                recordPackageViews(path.subList(0, prefixSize), packageDescriptor, trace, position)
                return Pair(packageDescriptor, prefixSize)
            }
            fqName = fqName.parent()
            prefixSize--
        }
        return Pair(getPackage(FqName.ROOT), 0)
    }

    private fun recordPackageViews(
        path: List<QualifierPart>,
        packageView: PackageViewDescriptor,
        trace: BindingTrace,
        position: QualifierPosition
    ) {
        path.foldRight(packageView) { qualifierPart, currentView ->
            storeResult(trace, qualifierPart.expression, currentView, shouldBeVisibleFrom = null, position = position)
            currentView.containingDeclaration
                ?: error(
                    "Containing Declaration must be not null for package with fqName: ${currentView.fqName}, " +
                            "path: ${path.joinToString()}, packageView fqName: ${packageView.fqName}"
                )
        }
    }

    private fun storeResult(
        trace: BindingTrace,
        referenceExpression: CjSimpleNameExpression?,
        descriptor: DeclarationDescriptor?,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        isQualifier: Boolean = true
    ): Qualifier? {
        referenceExpression ?: return null
        if (descriptor == null) {
            trace.report(Errors.UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
            return null
        }

        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptor)

//        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, referenceExpression, trace)

        if (descriptor is DeclarationDescriptorWithVisibility) {
            val fromToCheck =
                if (shouldBeVisibleFrom is PackageFragmentDescriptor && shouldBeVisibleFrom.source == SourceElement.NO_SOURCE && referenceExpression.containingFile !is DummyHolder) {
                    PackageFragmentWithCustomSource(
                        shouldBeVisibleFrom,
                        CangJieSourceElement(referenceExpression.getContainingCjFile())
                    )
                } else {
                    shouldBeVisibleFrom
                }
//            if (!isVisible(descriptor, fromToCheck, position, languageVersionSettings)) {
//                trace.report(
//                    Errors.INVISIBLE_REFERENCE.on(
//                        referenceExpression,
//                        descriptor,
//                        descriptor.visibility,
//                        descriptor
//                    )
//                )
//            }
        }

        return if (isQualifier) storeQualifier(trace, referenceExpression, descriptor) else null
    }
    private fun storeQualifier(
        trace: BindingTrace,
        referenceExpression: CjSimpleNameExpression,
        descriptor: DeclarationDescriptor
    ): Qualifier? {
        val qualifier =
            when (descriptor) {
//                is PackageViewDescriptor -> PackageQualifier(referenceExpression, descriptor)
                is ClassDescriptor -> ClassQualifier(referenceExpression, descriptor)
//                is TypeParameterDescriptor -> TypeParameterQualifier(referenceExpression, descriptor)
//                is TypeAliasDescriptor -> {
//                    val classDescriptor = descriptor.classDescriptor ?: return null
//                    TypeAliasQualifier(referenceExpression, descriptor, classDescriptor)
//                }
                else -> return null
            }

        trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)

        return qualifier
    }
    private fun resolveToPackageOrClass(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition
    ): DeclarationDescriptor? {
        val (packageOrClassDescriptor, endIndex) =
            resolveToPackageOrClassPrefix(
                path,
                moduleDescriptor,
                trace,
                shouldBeVisibleFrom,
                scopeForFirstPart,
                position
            )

        if (endIndex != path.size) {
            return null
        }

        return packageOrClassDescriptor
    }


    fun processImportReference(
        importDirective: CjImportInfo,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        excludedImportNames: Collection<FqName>,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        fun processReferenceInContextOf(moduleDescriptor: ModuleDescriptor): ImportingScope? =
            doProcessImportReference(
                importDirective,
                moduleDescriptor,
                trace,
                excludedImportNames,
                packageFragmentForVisibilityCheck
            )

        val primaryImportingScope = processReferenceInContextOf(moduleDescriptor)
TODO()
//
//        val resolutionAnchor = moduleDescriptor.getResolutionAnchorIfAny() ?: return primaryImportingScope
//        val anchorImportingScope = processReferenceInContextOf(resolutionAnchor) ?: return primaryImportingScope
//        if (primaryImportingScope == null) return anchorImportingScope
//        return CompositePrioritizedImportingScope(anchorImportingScope, primaryImportingScope)
    }
}

internal enum class QualifierPosition {
    PACKAGE_HEADER, IMPORT, TYPE, EXPRESSION
}

val SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE: Key<Boolean> = Key.create<Boolean>("SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE")

var CjFile.suppressDiagnosticsInDebugMode: Boolean
    get() = when (this) {
        is CjCodeFragment -> true
        else -> getUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE) ?: false
    }
    set(skip) {
        putUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE, skip)
    }

fun CjElement.suppressDiagnosticsInDebugMode(): Boolean {
    return if (this is CjFile) {
        this.suppressDiagnosticsInDebugMode
    } else {
        val file = this.containingFile
        file is CjFile && file.suppressDiagnosticsInDebugMode
    }
}

/*
    This purpose of this class is to pass information about source file for current package fragment in order for check visibilities between modules
    (see ModuleVisibilityHelperImpl.isInFriendModule).
 */
private class PackageFragmentWithCustomSource(
    override val original: PackageFragmentDescriptor,
    private val source: SourceElement
) :
    PackageFragmentDescriptor by original {
    override fun getSource(): SourceElement = source
}
