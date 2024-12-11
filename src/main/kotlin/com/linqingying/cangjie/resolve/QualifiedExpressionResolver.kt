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

package com.linqingying.cangjie.resolve


import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.openapi.util.Key
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.util.SmartList
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.LazyPackageViewDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.LazyReexportAgent
import com.linqingying.cangjie.diagnostics.Errors.*
import com.linqingying.cangjie.incremental.CangJieLookupLocation
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import com.linqingying.cangjie.resolve.QualifierPosition.*
import com.linqingying.cangjie.resolve.calls.CallExpressionElement
import com.linqingying.cangjie.resolve.calls.unrollToLeftMostQualifiedExpression
import com.linqingying.cangjie.resolve.descriptorUtil.fqNameSafe
import com.linqingying.cangjie.resolve.descriptorUtil.module
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.resolve.scopes.receivers.*
import com.linqingying.cangjie.resolve.source.CangJieSourceElement
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.utils.CallOnceFunction
import jakarta.inject.Inject


class QualifiedExpressionResolver(
    val languageVersionSettings: LanguageVersionSettings

) {
    private lateinit var typeResolver: TypeResolver

    @Inject
    fun setTypeResolver(typeResolver: TypeResolver) {
        this.typeResolver = typeResolver
    }

    companion object {
        /**
         *  Shouldn't be visible to users.
         *  Used as prefix for [FqName] from non-root to avoid conflicts when resolving in IDE.
         *  E.g.:
         *  ---------
         *  package a
         *
         *  class A
         *
         *  fun test(a: Any) {
         *      a.A() // invalid code -> incorrect import/completion/etc.
         *      _root_ide_package_.a.A() // OK
         *  }
         *  ---------
         */
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE = "_root_ide_package_"
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT = "$ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE."
    }

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
        constructor(expression: CjSimpleNameExpression) : this(expression.referencedNameAsName, expression)

        override fun component2() = expression
    }

    data class QualifiedExpressionResolveResult(
        val classOrPackage: DeclarationDescriptor?,
        val memberName: Name?
    ) {
        companion object {
            val UNRESOLVED = QualifiedExpressionResolveResult(null, null)
        }
    }

    fun resolveClassOrPackageInQualifiedExpression(
        expression: CjQualifiedExpression,
        scope: LexicalScope,
        context: BindingContext
    ): QualifiedExpressionResolveResult {
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        val path = mapToQualifierParts(qualifiedExpressions, 0)
        val trace = DelegatingBindingTrace(context, "Temp trace for resolving qualified expression")

        val (result, index) = resolveToPackageOrClassPrefix(
            path = path,
            moduleDescriptor = scope.ownerDescriptor.module,
            trace = trace,
            shouldBeVisibleFrom = scope.ownerDescriptor,
            scopeForFirstPart = scope,
            position = EXPRESSION
        )

        if (result == null) return QualifiedExpressionResolveResult.UNRESOLVED
        return when (index) {
            path.size -> QualifiedExpressionResolveResult(result, null)
            path.size - 1 -> QualifiedExpressionResolveResult(result, path[index].name)
            else -> QualifiedExpressionResolveResult.UNRESOLVED
        }
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
//            if (!classifier.canBeResolvedWithoutDeprecation(this, lookupLocation)) {
//                trace.report(DEPRECATED_ACCESS_BY_SHORT_NAME.on(reportOn, classifier))
//            }
        }

        return classifier
    }


    private fun mapToQualifierParts(
        qualifiedExpressions: List<CjQualifiedExpression>,
        skipLast: Int
    ): List<QualifierPart> {
        if (qualifiedExpressions.isEmpty()) return emptyList()

        val first = qualifiedExpressions.first()
        if (first !is CjDotQualifiedExpression) return emptyList()
        val firstReceiver = first.receiverExpression
        if (firstReceiver !is CjSimpleNameExpression) return emptyList()

        // Qualifier parts are receiver name for the leftmost expression
        //  and selector names for all but the rightmost qualified expressions
        //  (since rightmost selector should denote a value in expression position,
        //  and thus can't be a qualifier part).
        // E.g.:
        //  qualified expression 'a.b': qualifier parts == ['a']
        //  qualified expression 'a.b.c.d': qualifier parts == ['a', 'b', 'c']

        val qualifierParts = arrayListOf<QualifierPart>()
        qualifierParts.add(ExpressionQualifierPart(firstReceiver))

        for (qualifiedExpression in qualifiedExpressions.dropLast(skipLast)) {
            if (qualifiedExpression !is CjDotQualifiedExpression) break
            val selector = qualifiedExpression.selectorExpression
            if (selector !is CjSimpleNameExpression) break
            qualifierParts.add(ExpressionQualifierPart(selector))
        }

        return qualifierParts
    }

    fun resolveQualifierInExpressionAndUnroll(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext,
        isValue: (CjSimpleNameExpression) -> Boolean
    ): List<CallExpressionElement> {
        val qualifiedExpressions = unrollToLeftMostQualifiedExpression(expression)
        val maxPossibleQualifierPrefix = mapToQualifierParts(qualifiedExpressions, 1)

        var (declarationDescriptor, nextIndexAfterPrefix) = resolveToPackageOrClassPrefix(
            path = maxPossibleQualifierPrefix,
            moduleDescriptor = context.scope.ownerDescriptor.module,
            trace = context.trace,
            shouldBeVisibleFrom = context.scope.ownerDescriptor,
            scopeForFirstPart = context.scope,
            position = EXPRESSION,
            isValue = isValue,

            )
        if (nextIndexAfterPrefix == 0) {
            var (declarationDescriptor1, nextIndexAfterPrefix1) = resolveToPackageOrClassPrefixByImport(
                path = maxPossibleQualifierPrefix,
                moduleDescriptor = context.scope.ownerDescriptor.module,
                trace = context.trace,
                shouldBeVisibleFrom = context.scope.ownerDescriptor,
                scopeForFirstPart = context.scope,
                position = EXPRESSION,
                isValue = isValue,

                )
            if (nextIndexAfterPrefix1 != 0) {
                declarationDescriptor = declarationDescriptor1
                nextIndexAfterPrefix = nextIndexAfterPrefix1
            }
        }

        val nextExpressionIndexAfterQualifier =
            if (nextIndexAfterPrefix == 0) 0 else nextIndexAfterPrefix - 1

        return qualifiedExpressions
            .subList(nextExpressionIndexAfterQualifier, qualifiedExpressions.size)
            .map(::CallExpressionElement)
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
                        referenceExpression.referencedNameAsName,
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
                    expression.referencedNameAsName,
                    CangJieLookupLocation(expression),
                    expression,
                    trace
                )

                checkNotEnumEntry(classifier, trace, expression)
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
//                expression.referencedNameAsName,
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

    private fun checkNotEnumEntry(
        descriptor: DeclarationDescriptor?,
        trace: BindingTrace,
        expression: CjSimpleNameExpression?
    ) {
        expression ?: return
        if (descriptor != null && DescriptorUtils.isEnumEntry(descriptor)) {
            val qualifiedParent = expression.getTopmostParentQualifiedExpressionForSelector()
            if (qualifiedParent == null) {
                trace.report(ENUM_ENTRY_AS_TYPE.on(expression))
            }
        }
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
                checkNotEnumEntry(descriptor, trace, lastPart.expression)
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
    ): QualifierReceiver? {

        val name = expression.referencedNameAsName
        if (!expression.isPhysical && !name.isSpecial && name.asString()
                .endsWith(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
        ) {
            return null
        }

        val location = CangJieLookupLocation(expression)
        val qualifierDescriptor = when (receiver) {
            is PackageQualifier -> {
                val childPackageFQN = receiver.descriptor.fqName.child(name)
                receiver.descriptor.module.getPackage(childPackageFQN).takeUnless { it.isEmpty() }
                    ?: receiver.descriptor.memberScope.getContributedClassifier(name, location)
            }

            is EnumClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)
            is ClassQualifier -> receiver.staticScope.getContributedClassifier(name, location)
//            无接收器时，直接获取
            null -> context.scope.findClassifier(name, location)
                ?: context.scope.getPackageView(name, location)

            is ReceiverValue -> receiver.type.memberScope.memberScopeAsImportingScope().findClassifier(name, location)
            else -> null
        }

        if (qualifierDescriptor != null) {
            typeResolver.resolveTypeForClass(expression, context.scope, context.trace, qualifierDescriptor)
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
            if (importDirective is CjImportDirectiveItem)
                computePackageFragmentToCheck(importDirective.getContainingCjFile(), packageFragmentForVisibilityCheck)
            else
                null


//报告不应该导入自己
//        if (packageFragmentForCheck != null) {
//            val packageFqname = importDirective.importedFqName
//
//            if (importDirective is CjImportDirectiveItem) {
//                if (packageFqname == packageFragmentForCheck.fqName) {
//
//                    trace.report(SELF_IMPORT_NOT_ALLOWED.on(importDirective, packageFqname))
//
//                    return null
//                }
//            }
//
//        }

        if (importDirective.isAllUnder) {
            val packageOrClassDescriptor = resolveToPackageOrClass(
                path, moduleDescriptor, trace, packageFragmentForCheck,
                scopeForFirstPart = null, position = IMPORT
            ).classDescriptorFromTypeAlias() ?: return null

            if (packageOrClassDescriptor is ClassDescriptor  /* && packageOrClassDescriptor.kind.isObject */ && lastPart.expression != null) {
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


//                    TODO 这里有问题，有时候可能会出现导入的不是包，但是一样报错
//                  如歌导入的是一个包，那么它不能为导出  不能使用 除private以外的修饰符修饰import语句
//                    val importDirective = lastPartExpression.getParentOfType<CjImportDirectiveItem>(true)
//                    if (importDirective != null) {
//                        if (importDirective.modifierVisibility != DescriptorVisibilities.PRIVATE) {
//                            importDirective.importedFqName?.let {
//                                trace.report(
//                                    IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED.on(
//                                        importDirective,
//                                        it,
//                                        importDirective.modifierVisibility
//                                    )
//                                )
//                            }
//                        }
//                    }


//                    不能导入模块名
//                    if (packageDescriptor.fqName.isModuleName) {
//                        trace.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(lastPartExpression))
//                        descriptors.add(packageOrClassDescriptor)
//                    }

                    descriptors.add(packageDescriptor)
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

    private fun resolveInIDEMode(path: List<QualifierPart>): Boolean =
        path.size > 1 && path.first().name.asString() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

    fun resolveDescriptorForDoubleColonLHS(
        expression: CjExpression,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val ownerDescriptor = if (!isDebuggerContext) scope.ownerDescriptor else null

        val qualifierPartList = expression.asQualifierPartList(doubleColonLHS = true)
        if (qualifierPartList.isEmpty()) {
            return TypeQualifierResolutionResult(qualifierPartList, null)
        }

        if (qualifierPartList.size == 1) {
            val (name, simpleNameExpression) = qualifierPartList.single()
            val descriptor = scope.findClassifierAndReportDeprecationIfNeeded(
                name,
                CangJieLookupLocation(simpleNameExpression),
                simpleNameExpression,
                trace
            )
            storeResult(trace, simpleNameExpression, descriptor, ownerDescriptor, position = TYPE, isQualifier = true)
            return TypeQualifierResolutionResult(qualifierPartList, descriptor)
        }

        return resolveQualifierPartListForType(qualifierPartList, ownerDescriptor, scope, trace, isQualifier = true)
    }

    private fun resolveToPackageOrClassPrefixByImport(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition,
        isValue: ((CjSimpleNameExpression) -> Boolean)? = null
    ): Pair<DeclarationDescriptor?, Int> {
        if (resolveInIDEMode(path)) {
            return resolveToPackageOrClassPrefixByImport(
                path.subList(1, path.size),
                moduleDescriptor,
                trace,
                shouldBeVisibleFrom,
                scopeForFirstPart = null,
                position = position,
                isValue = null
            ).let { it.first to it.second + 1 }
        }

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

        if (classifierDescriptor != null) {
//            typeResolver.resolveTypeForClass()
            storeResult(
                trace, firstPart.expression, classifierDescriptor, shouldBeVisibleFrom, position,
                scope = scopeForFirstPart
            )
            return Pair(classifierDescriptor, 1)
        }


        val (prefixDescriptor, nextIndexAfterPrefix) = moduleDescriptor.quickResolveToPackageByImport(
            scopeForFirstPart,
            shouldBeVisibleFrom,
            path,
            trace,
            position
        )

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

    private fun resolveToPackageOrClassPrefix(
        path: List<QualifierPart>,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        scopeForFirstPart: LexicalScope?,
        position: QualifierPosition,
        isValue: ((CjSimpleNameExpression) -> Boolean)? = null
    ): Pair<DeclarationDescriptor?, Int> {
        if (resolveInIDEMode(path)) {
            return resolveToPackageOrClassPrefix(
                path.subList(1, path.size),
                moduleDescriptor,
                trace,
                shouldBeVisibleFrom,
                scopeForFirstPart = null,
                position = position,
                isValue = null
            ).let { it.first to it.second + 1 }
        }

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

        if (classifierDescriptor != null) {
//            typeResolver.resolveTypeForClass()
            storeResult(
                trace, firstPart.expression, classifierDescriptor, shouldBeVisibleFrom, position,
                scope = scopeForFirstPart
            )
            return Pair(classifierDescriptor, 1)
        }


        val (prefixDescriptor, nextIndexAfterPrefix) = moduleDescriptor.quickResolveToPackage(
            shouldBeVisibleFrom,
            path,
            trace,
            position
        )

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

    private fun ModuleDescriptor.quickResolveToPackageByImport(
        scopeForFirstPart: LexicalScope?,
        shouldBeVisibleFrom: DeclarationDescriptor?,
        path: List<QualifierPart>,
        trace: BindingTrace,
        position: QualifierPosition
    ): Pair<PackageViewDescriptor, Int> {
        val possiblePackagePrefixSize =
            path.indexOfFirst { it.typeArguments != null }.let { if (it == -1) path.size else it + 1 }
        val firstName = path.first().name

        val packageView = scopeForFirstPart?.getPackageView(firstName, NoLookupLocation.FROM_PACKAGE)


        var fqName = if (packageView?.isEmpty() == false) {
            packageView.fqName.child(
                FqName.fromSegments(
                    path.subList(1, possiblePackagePrefixSize).map { it.name.asString() })
            )

        } else {
            FqName.fromSegments(path.subList(0, possiblePackagePrefixSize).map { it.name.asString() })

        }
        var prefixSize = possiblePackagePrefixSize
//        if(packageView?.isEmpty() == false){
//            recordPackageViews(shouldBeVisibleFrom, path.subList(0, prefixSize), packageDescriptor, trace, position)
//            return Pair(packageView, prefixSize)
//        }
//
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

                    .distinctBy { descriptor ->
                        descriptor.fqNameSafe
                    }


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
        reportReexportError: Boolean = true,
        scope: LexicalScope? = null,
    ): QualifierReceiver? {
        referenceExpression ?: return null
        if (descriptor == null) {
            trace.report(UNRESOLVED_REFERENCE.on(referenceExpression, referenceExpression))
            return null
        }




        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, descriptor)

//        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, referenceExpression, trace)

//        if (descriptor is DeclarationDescriptorWithVisibility) {

        val fromToCheck =
            if (shouldBeVisibleFrom is PackageFragmentDescriptor && shouldBeVisibleFrom.source == SourceElement.NO_SOURCE && referenceExpression.containingFile !is DummyHolder) {
                PackageFragmentWithCustomSource(
                    shouldBeVisibleFrom,
                    CangJieSourceElement(referenceExpression.getContainingCjFile())
                )
            } else {
                shouldBeVisibleFrom
            }

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

            IMPORT, TYPE -> {

//                //                不能使用 除private以外的修饰符修饰import语句
//                val importDirective = referenceExpression.getParentOfType<CjImportDirectiveItem>(true)
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


            else -> {

            }
        }
//        }
        return if (isQualifier) storeQualifier(trace, referenceExpression, descriptor, scope) else null
    }

    private fun storeQualifier(
        trace: BindingTrace,
        referenceExpression: CjSimpleNameExpression,
        descriptor: DeclarationDescriptor,
        scope: LexicalScope? = null,

        ): QualifierReceiver? {
        val qualifier =
            when (descriptor) {
                is PackageViewDescriptor -> PackageQualifier(referenceExpression, descriptor)
                is ClassDescriptor -> {
//                    if (descriptor.kind == ClassKind.ENUM) {
//                        EnumClassQualifier(referenceExpression, descriptor)
//                    } else {


                    ClassQualifier(referenceExpression, descriptor, scope?.let {

                        typeResolver.resolveTypeForClass(referenceExpression, it, trace, descriptor)
                    })

//                    }


                }

                is TypeParameterDescriptor -> TypeParameterQualifier(referenceExpression, descriptor)
                is TypeAliasDescriptor -> {
                    val classDescriptor = descriptor.classDescriptor ?: return null
                    TypeAliasQualifier(referenceExpression, descriptor, classDescriptor)
                }

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
        else -> getUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE) == true
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
    override val source: SourceElement
) :
    PackageFragmentDescriptor by original

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
