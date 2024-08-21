package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.Errors.*
import com.huawei.cangjie.descriptors.impl.LazyPackageViewDescriptorImpl
import com.huawei.cangjie.descriptors.impl.LazyReexportAgent
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getParentOfType
import com.huawei.cangjie.resolve.QualifierPosition.*
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.resolve.scopes.receivers.*
import com.huawei.cangjie.resolve.source.CangJieSourceElement
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.utils.CallOnceFunction
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.openapi.util.Key
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.util.SmartList


class QualifiedExpressionResolver(val languageVersionSettings: LanguageVersionSettings) {
    data class TypeQualifierResolutionResult(
        val qualifierParts: List<ExpressionQualifierPart>,
        val classifierDescriptor: ClassifierDescriptor? = null
    ) {
        val allProjections: List<CjTypeProjection>
            get() = qualifierParts.flatMap { it.typeArguments?.arguments.orEmpty() }
    }

    class ExpressionQualifierPart(
        name: Name,
        override val expression: CjSimpleNameExpression,
        typeArguments: CjTypeArgumentList? = null
    ) : QualifierPart(name, typeArguments, CangJieLookupLocation(expression)) {
        constructor(expression: CjSimpleNameExpression) : this(expression.getReferencedNameAsName(), expression)

        override fun component2() = expression
    }

    private fun LexicalScope.findClassifierAndReportDeprecationIfNeeded(
        name: Name,
        lookupLocation: CangJieLookupLocation,
        reportOn: CjExpression?,
        trace: BindingTrace
    ): ClassifierDescriptor? {
        val (classifier, isDeprecated) = findFirstClassifierWithDeprecationStatus(name, lookupLocation) ?: return null

        if (isDeprecated && reportOn != null) {
            trace.record(BindingContext.DEPRECATED_SHORT_NAME_ACCESS, reportOn) // For IDE

            // slow-path: we know that closest classifier is imported by the deprecated path, but before reporting
            // deprecation, we have to recheck if there's some other import path, which isn't deprecated (e.g. explicit import)
            if (!classifier.canBeResolvedWithoutDeprecation(this, lookupLocation)) {
                trace.report(DEPRECATED_ACCESS_BY_SHORT_NAME.on(reportOn, classifier))
            }
        }

        return classifier
    }

    private fun CjUserType.asQualifierPartList(): Pair<List<ExpressionQualifierPart>, Boolean> {
        var hasError = false
        val result = SmartList<ExpressionQualifierPart>()
        var userType: CjUserType? = this
        while (userType != null) {
            val referenceExpression = userType.referenceExpression
            if (referenceExpression != null) {
                result.add(
                    ExpressionQualifierPart(
                        referenceExpression.getReferencedNameAsName(),
                        referenceExpression,
                        userType.typeArgumentList
                    )
                )
            } else {
                hasError = true
            }
            userType = userType.qualifier
        }
        return result.asReversed() to hasError
    }

    fun resolveDescriptorForType(
        userType: CjUserType,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val ownerDescriptor = if (!isDebuggerContext) scope.ownerDescriptor else null
        if (userType.qualifier == null) {
//      如果没有使用限定名称
            val descriptor = userType.referenceExpression?.let { expression ->
                val classifier = scope.findClassifierAndReportDeprecationIfNeeded(
                    expression.getReferencedNameAsName(),
                    CangJieLookupLocation(expression),
                    expression,
                    trace
                )

//                checkNotEnumEntry(classifier, trace, expression)
                storeResult(
                    trace,
                    expression,
                    classifier,
                    ownerDescriptor,
                    position = TYPE,
                    isQualifier = false
                )
                classifier
            }

            return TypeQualifierResolutionResult(userType.asQualifierPartList().first, descriptor)
        }
//        val a = userType.referenceExpression?.let { expression ->
//            val classifier = scope.findClassifierAndReportDeprecationIfNeeded(
//                expression.getReferencedNameAsName(),
//                CangJieLookupLocation(expression),
//                expression,
//                trace
//            )
//        }


//如果使用了限定名称
//  1 如果使用了包含模块名称的限定名称 报错 快速修复
//  2 如果使用了包名限定名称，则查找包
        val (qualifierPartList, hasError) = userType.asQualifierPartList()
        if (hasError) {
            val descriptor = resolveToPackageOrClass(
                qualifierPartList,
                scope.ownerDescriptor.module,
                trace,
                ownerDescriptor,
                scope,
                position = TYPE
            ) as? ClassifierDescriptor
            return TypeQualifierResolutionResult(qualifierPartList, descriptor)
        }

        return resolveQualifierPartListForType(qualifierPartList, ownerDescriptor, scope, trace, isQualifier = false)
    }

    private fun resolveQualifierPartListForType(
        qualifierPartList: List<ExpressionQualifierPart>,
        ownerDescriptor: DeclarationDescriptor?,
        scope: LexicalScope,
        trace: BindingTrace,
        isQualifier: Boolean
    ): TypeQualifierResolutionResult {
        assert(qualifierPartList.isNotEmpty()) { "Qualifier list should not be empty" }

//     查找包是否声明
        val qualifier = resolveToPackageOrClass(
            qualifierPartList.subList(0, qualifierPartList.size - 1),
            scope.ownerDescriptor.module, trace, ownerDescriptor, scope,
            position = TYPE
        ) ?: return TypeQualifierResolutionResult(qualifierPartList, null)
// 该包的模块名

        val lastPart = qualifierPartList.last()
        val classifier = when (qualifier) {
            is PackageViewDescriptor -> qualifier.memberScope.getContributedClassifier(lastPart.name, lastPart.location)
            is ClassDescriptor -> {
                val descriptor =
                    qualifier.unsubstitutedInnerClassesScope.getContributedClassifier(lastPart.name, lastPart.location)
//                checkNotEnumEntry(descriptor, trace, lastPart.expression)
                descriptor
            }

            else -> null
        }

        val moduleName = qualifier.fqNameSafe.moduleName
        if (classifier != null && qualifierPartList[0].name == moduleName) {
            trace.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(qualifierPartList[0].expression))
        }

        storeResult(
            trace,
            lastPart.expression,
            classifier,
            ownerDescriptor,
            position = TYPE,
            isQualifier = isQualifier,

            packageView = qualifier
        )
        return TypeQualifierResolutionResult(qualifierPartList, classifier)
    }


    //    private fun checkNotEnumEntry(descriptor: DeclarationDescriptor?, trace: BindingTrace, expression: CjSimpleNameExpression?) {
//        expression ?: return
//        if (descriptor != null && DescriptorUtils.isEnumEntry(descriptor)) {
//            val qualifiedParent = expression.getTopmostParentQualifiedExpressionForSelector()
//            if (qualifiedParent == null || qualifiedParent.parent !is CjDoubleColonExpression) {
//                trace.report(Errors.ENUM_ENTRY_AS_TYPE.on(expression))
//            }
//        }
//    }
    fun resolvePackageHeader(
        packageDirective: CjPackageDirective,
        module: ModuleDescriptor,
        trace: BindingTrace
    ) {
        val packageNames = packageDirective.packageNames
        for ((index, nameExpression) in packageNames.withIndex()) {
            storeResult(
                trace,
                nameExpression,
                module.getPackage(packageDirective.getFqName(nameExpression)),
                shouldBeVisibleFrom = null,
                position = PACKAGE_HEADER,
                isQualifier = index != packageNames.lastIndex
            )
        }
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

    fun CjImportInfo.ImportContent.asQualifierPartList(): List<QualifierPart> =
        when (this) {
            is CjImportInfo.ImportContent.ExpressionBased -> expression.asQualifierPartList()
            is CjImportInfo.ImportContent.FqNameBased -> fqName.pathSegments().map { QualifierPart(it) }
        }


    fun resolveNameExpressionAsQualifierForDiagnostics(
        expression: CjSimpleNameExpression,
        receiver: Receiver?,
        context: ExpressionTypingContext
    ): Qualifier? {
        val name = expression.getReferencedNameAsName()
        if (!expression.isPhysical && !name.isSpecial && name.asString()
                .endsWith(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
        ) {
            return null
        }

        val location = CangJieLookupLocation(expression)
        val qualifierDescriptor = when (receiver) {
//            is PackageQualifier -> {
//                val childPackageFQN = receiver.descriptor.fqName.child(name)
//                receiver.descriptor.module.getPackage(childPackageFQN).takeUnless { it.isEmpty() }
//                    ?: receiver.descriptor.memberScope.getContributedClassifier(name, location)
//            }
            is ClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)
            null -> context.scope.findClassifier(name, location)
                ?: context.scope.ownerDescriptor.module.getPackage(FqName.ROOT.child(name)).takeUnless { it.isEmpty() }

            is ReceiverValue -> receiver.type.memberScope.memberScopeAsImportingScope().findClassifier(name, location)
            else -> null
        }

        if (qualifierDescriptor != null) {
            return storeResult(
                context.trace,
                expression,
                qualifierDescriptor,
                context.scope.ownerDescriptor,
                EXPRESSION
            )
        }

        return null
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


//报告不应该导入自己
        if (packageFragmentForCheck != null) {
            val packageFqname = importDirective.importedFqName

            if (importDirective is CjImportDirective) {
                if (packageFqname == packageFragmentForCheck.fqName) {

                    trace.report(SELF_IMPORT_NOT_ALLOWED.on(importDirective, packageFqname))

                    return null
                }
            }

        }

        if (importDirective.isAllUnder) {
            val packageOrClassDescriptor = resolveToPackageOrClass(
                path, moduleDescriptor, trace, packageFragmentForCheck,
                scopeForFirstPart = null, position = IMPORT
            ).classDescriptorFromTypeAlias() ?: return null

            if (packageOrClassDescriptor is ClassDescriptor && packageOrClassDescriptor.kind.isSingleton && lastPart.expression != null) {
                trace.report(
                    CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON.on(
                        lastPart.expression!!,
                        packageOrClassDescriptor
                    )
                ) // todo report on star
                return null
            }


            return AllUnderImportScope.create(packageOrClassDescriptor, excludedImportNames)
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

//        importDirective.modifierVisibility
        val aliasName = importDirective.importedName
        if (aliasName == null) {

            resolveToPackageOrClass(
                path,
                moduleDescriptor,
                trace,
                packageFragmentForVisibilityCheck,
                scopeForFirstPart = null,
                position = IMPORT
            )
            return null
        }

        val resolvedDescriptor = resolveToPackageOrClass(
            path.subList(0, path.size - 1), moduleDescriptor, trace,
            packageFragmentForVisibilityCheck, scopeForFirstPart = null, position = IMPORT
        ) ?: return null

        val packageOrClassDescriptor =
            (resolvedDescriptor as? TypeAliasDescriptor)?.let { it.classDescriptor ?: return null }
                ?: resolvedDescriptor



        return LazyExplicitImportScope(
            languageVersionSettings,

            packageOrClassDescriptor,
            packageFragmentForVisibilityCheck,
            lastPart.name,
            aliasName,
            CallOnceFunction(Unit) { candidates ->
//                if (candidates.isNotEmpty()) {
//                    storeResult(
//                        trace,
//                        lastPart.expression,
//                        candidates,
//                        packageFragmentForVisibilityCheck,
//                        position = IMPORT,
//                        isQualifier = false
//                    )

                tryResolveDescriptorsWhichCannotBeImported(
                    trace,
                    moduleDescriptor,
                    packageOrClassDescriptor,
                    lastPart, candidates, packageFragmentForVisibilityCheck
                )
//                } else {
//                    tryResolveDescriptorsWhichCannotBeImported(
//                        trace,
//                        moduleDescriptor,
//                        packageOrClassDescriptor,
//                        lastPart
//                    )
//                }
            }
        )
    }

    private fun tryResolveDescriptorsWhichCannotBeImported(
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor,
        packageOrClassDescriptor: DeclarationDescriptor,
        lastPart: QualifierPart,
        candidates: Collection<DeclarationDescriptor> = emptyList(),
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?

    ) {
        val lastPartExpression = lastPart.expression ?: return

        val descriptors = SmartList<DeclarationDescriptor>().apply {
            addAll(candidates)
        }
        val lastName = lastPart.name

        when (packageOrClassDescriptor) {
            is PackageViewDescriptor -> {
                val packageDescriptor = moduleDescriptor.getPackage(packageOrClassDescriptor.fqName.child(lastName))
                if (!packageDescriptor.isEmpty()) {
//                    trace.report(PACKAGE_CANNOT_BE_IMPORTED.on(lastPartExpression))
//                    descriptors.add(packageOrClassDescriptor)


//                    TODO 这里有问题，有时候可能会出现导入的不是包，但是一样报错
//                    不能使用 除private以外的修饰符修饰import语句
                    val importDirective = lastPartExpression.getParentOfType<CjImportDirective>(true)
                    if (importDirective != null) {
                        if (importDirective.modifierVisibility != DescriptorVisibilities.PRIVATE) {
                            importDirective.importedFqName?.let {
                                trace.report(
                                    IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED.on(
                                        importDirective,
                                        it,
                                        importDirective.modifierVisibility
                                    )
                                )
                            }
                        }
                    }


//                    不能导入模块名
                    if (packageDescriptor.fqName.isModuleName) {
                        trace.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(lastPartExpression))
                        descriptors.add(packageOrClassDescriptor)
                    }
//                    return

                }
            }

            is ClassDescriptor -> {
                val memberScope = packageOrClassDescriptor.unsubstitutedMemberScope
                descriptors.addAll(memberScope.getContributedFunctions(lastName, lastPart.location))
                descriptors.addAll(memberScope.getContributedVariables(lastName, lastPart.location))
                if (descriptors.isNotEmpty()) {
                    trace.report(CANNOT_BE_IMPORTED.on(lastPartExpression, lastName))
                }
            }

            else -> throw IllegalStateException("Should be class or package: $packageOrClassDescriptor")
        }
        storeResult(
            trace,
            lastPart.expression,
            descriptors,
            shouldBeVisibleFrom = packageFragmentForVisibilityCheck,
            position = IMPORT,
            isQualifier = false
        )
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

        if (position == EXPRESSION) {
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
        if (classifierDescriptor != null)
            return Pair(classifierDescriptor, 1)
//        第一位匹配最后一位
        val qprts = scopeForFirstPart?.findPackageQualifierParts(path[0].name)


        val (prefixDescriptor, nextIndexAfterPrefix) = if (!qprts.isNullOrEmpty()) {
            var prefixDescriptor: PackageViewDescriptor? = null
            var nextIndexAfterPrefix: Int? = null
            for (qprt in qprts) {
//                合并两个path
//                去掉path的第一位，将path追加到qprt中
                val modifiedPath = qprt + path.drop(1)
                val (_prefixDescriptor, _nextIndexAfterPrefix) = moduleDescriptor.quickResolveToPackage(
                    shouldBeVisibleFrom,
                    modifiedPath,
                    trace,
                    position
                )

                prefixDescriptor = _prefixDescriptor
                nextIndexAfterPrefix = _nextIndexAfterPrefix
                if (!_prefixDescriptor.fqName.isRoot) {

                    break
                }
            }

            Pair(prefixDescriptor!!, nextIndexAfterPrefix!!)
        } else {

            moduleDescriptor.quickResolveToPackage(shouldBeVisibleFrom, path, trace, position)

        }

//        val (prefixDescriptor, nextIndexAfterPrefix) =
//            if (classifierDescriptor != null)
//                Pair(classifierDescriptor, 1)
//            else
//                moduleDescriptor.quickResolveToPackage(path, trace, position)

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
            if (!(position == EXPRESSION && nextPackageOrClassDescriptor == null)) {
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
        shouldBeVisibleFrom: DeclarationDescriptor?,
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
                recordPackageViews(shouldBeVisibleFrom, path.subList(0, prefixSize), packageDescriptor, trace, position)
                return Pair(packageDescriptor, prefixSize)
            }
            fqName = fqName.parent()
            prefixSize--
        }
        return Pair(getPackage(FqName.ROOT), 0)
    }

    private fun recordPackageViews(
        shouldBeVisibleFrom: DeclarationDescriptor?,

        path: List<QualifierPart>,
        packageView: PackageViewDescriptor,
        trace: BindingTrace,
        position: QualifierPosition
    ) {
        path.foldRight(packageView) { qualifierPart, currentView ->
            storeResult(
                trace,
                qualifierPart.expression,
                currentView,
                shouldBeVisibleFrom = shouldBeVisibleFrom,
                position = position
            )
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
        descriptors: Collection<DeclarationDescriptor>,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        isQualifier: Boolean = true
    ) {
        referenceExpression ?: return
        if (descriptors.size > 1) {
            val visibleDescriptors =
                descriptors.filter { isVisible(it, shouldBeVisibleFrom, position, languageVersionSettings) }
            when {
                visibleDescriptors.isEmpty() -> {
                    val descriptor = descriptors.first() as DeclarationDescriptorWithVisibility
                    trace.report(
                        INVISIBLE_REFERENCE.on(
                            referenceExpression,
                            descriptor,
                            descriptor.visibility,
                            descriptor
                        )
                    )
                }

                visibleDescriptors.size > 1 -> {
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression, visibleDescriptors)
                }

                else -> {
                    storeResult(trace, referenceExpression, visibleDescriptors.single(), null, position, isQualifier)
                }
            }
        } else {
            storeResult(
                trace,
                referenceExpression,
                descriptors.singleOrNull(),
                shouldBeVisibleFrom,
                position,
                isQualifier
            )
        }
    }

    private fun storeResult(
        trace: BindingTrace,
        referenceExpression: CjSimpleNameExpression?,
        descriptor: DeclarationDescriptor?,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        position: QualifierPosition,
        isQualifier: Boolean = true,
        packageView: DeclarationDescriptor? = null,
        reportReexportError: Boolean = true
    ): Qualifier? {
        referenceExpression ?: return null
        if (descriptor == null) {
            trace.report(UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
            return null
        }




        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptor)

//        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, referenceExpression, trace)

//        if (descriptor is DeclarationDescriptorWithVisibility) {


        when (position) {
            PACKAGE_HEADER -> {
                if (descriptor is LazyPackageViewDescriptorImpl) {


//                       报告包名修饰符不一致
                    descriptor.packageDirectives.forEach {
                        if (it.modifierVisibility == DescriptorVisibilities.PRIVATE) {
                            trace.report(WRONG_MODIFIER_TARGET.on(it, CjTokens.PRIVATE_KEYWORD, "Package"))
                        }
                        if (descriptor.isReportMacroPackage) {
                            trace.report(
                                INCONSISTENT_PACKAGE_MACOR.on(
                                    it
                                )
                            )
                        }

                        if (descriptor.isReported) {
                            trace.report(
                                INCONSISTENT_PACKAGE_MODIFIERS.on(
                                    it, it.fqName
                                )
                            )
                        }


                    }
                }
            }

            IMPORT -> {

//                //                不能使用 除private以外的修饰符修饰import语句
//                val importDirective = referenceExpression.getParentOfType<CjImportDirective>(true)
//
//                if (importDirective != null) {
//                    if (importDirective.modifierVisibility != DescriptorVisibilities.PRIVATE) {
//                        importDirective.importedFqName?.let {
//                            trace.report(
//                                IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED.on(
//                                    importDirective,
//                                    it,
//                                    importDirective.modifierVisibility
//                                )
//                            )
//                        }
//                    }
//                }


//                    不能导入模块名
//                if (packageDescriptor.fqName.isModuleName) {
//                    trace.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(referenceExpression))
//                    descriptors.add(packageOrClassDescriptor)
//                }

                val fromToCheck =
                    if (shouldBeVisibleFrom is PackageFragmentDescriptor && shouldBeVisibleFrom.source == SourceElement.NO_SOURCE && referenceExpression.containingFile !is DummyHolder) {
                        PackageFragmentWithCustomSource(
                            shouldBeVisibleFrom,
                            CangJieSourceElement(referenceExpression.getContainingCjFile())
                        )
                    } else {
                        shouldBeVisibleFrom
                    }

                if (descriptor is LazyReexportAgent) {

//                    var isReexportError = reportReexportError
//                    if (reportReexportError) {
                    if (!isVisible(descriptor, fromToCheck, position, languageVersionSettings)) {
//                        报告过一次就不报告了
//                            isReexportError = false
                        trace.report(
                            INVISIBLE_REFERENCE_REEXPORT.on(
                                referenceExpression,
                                descriptor,
                                descriptor.visibility,
                                descriptor.packageFragmentDescriptor.fqName
                            )
                        )
                    }
//                    }
                    storeResult(
                        trace,
                        referenceExpression,
                        descriptor.proxied,
                        shouldBeVisibleFrom,
                        position,
                        isQualifier,
                        packageView,

                    )
                } else {
                    if (!isVisible(descriptor, fromToCheck, position, languageVersionSettings)) {
                        trace.report(
                            INVISIBLE_REFERENCE.on(
                                referenceExpression,
                                descriptor,
                                descriptor.visibility,
                                descriptor
                            )
                        )
                    }
                }

            }

//            TYPE -> TODO()
//            EXPRESSION -> TODO()
            else -> {

            }
        }
//        }
        return if (isQualifier) storeQualifier(trace, referenceExpression, descriptor) else null
    }

    private fun storeQualifier(
        trace: BindingTrace,
        referenceExpression: CjSimpleNameExpression,
        descriptor: DeclarationDescriptor
    ): Qualifier? {
        val qualifier =
            when (descriptor) {
                is PackageViewDescriptor -> PackageQualifier(referenceExpression, descriptor)
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

//
        val resolutionAnchor = moduleDescriptor.getResolutionAnchorIfAny() ?: return primaryImportingScope
        val anchorImportingScope = processReferenceInContextOf(resolutionAnchor) ?: return primaryImportingScope
        if (primaryImportingScope == null) return anchorImportingScope
        return CompositePrioritizedImportingScope(anchorImportingScope, primaryImportingScope)
    }
}

internal enum class QualifierPosition {
    PACKAGE_HEADER, IMPORT, TYPE, EXPRESSION
}

val SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE = Key.create<Boolean>("SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE")

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

internal fun isVisible(
    descriptor: DeclarationDescriptor,
    shouldBeVisibleFrom: DeclarationDescriptor?,
    position: QualifierPosition,
    languageVersionSettings: LanguageVersionSettings
): Boolean {
    if (/*descriptor !is DeclarationDescriptorWithVisibility || */shouldBeVisibleFrom == null) return true

    val visibility = descriptor.visibility
    if (position == IMPORT) {
        if (DescriptorVisibilities.isPrivate(visibility)) return DescriptorVisibilities.inSameFile(
            descriptor,
            shouldBeVisibleFrom
        )
        if (!visibility.mustCheckInImports()) return true
    }
    return DescriptorVisibilityUtils.isVisibleIgnoringReceiver(descriptor, shouldBeVisibleFrom, languageVersionSettings)
}
