package com.huawei.cangjie.ide.imports

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.formatter.CangJieCodeStyleSettings
import com.huawei.cangjie.ide.formatter.cangjieCustomSettings
import com.huawei.cangjie.ide.util.ClassImportFilter
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.descriptorUtil.fqNameSafe
import com.huawei.cangjie.resolve.descriptorUtil.getImportableDescriptor
import com.huawei.cangjie.resolve.descriptorUtil.targetDescriptors
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.utils.addIfNotNull
import com.huawei.cangjie.utils.runAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

abstract class ImportInsertHelper {
    /*TODO: implementation is not quite correct*/
    abstract fun isImportedWithDefault(importPath: ImportPath, contextFile: CjFile): Boolean

    abstract fun isImportedWithLowPriorityDefaultImport(importPath: ImportPath, contextFile: CjFile): Boolean

    abstract fun mayImportOnShortenReferences(descriptor: DeclarationDescriptor, contextFile: CjFile): Boolean

    abstract fun importDescriptor(
        element: CjElement,
        descriptor: DeclarationDescriptor,
        runImmediately: Boolean = true,
        forceAllUnderImport: Boolean = false,
        aliasName: Name? = null,
    ): ImportDescriptorResult

    fun importDescriptor(
        file: CjFile,
        descriptor: DeclarationDescriptor,
        forceAllUnderImport: Boolean = false
    ): ImportDescriptorResult {
        return importDescriptor(file as CjElement, descriptor, runImmediately = true, forceAllUnderImport)
    }

//    abstract fun importPsiClass(element: CjElement, psiClass: PsiClass, runImmediately: Boolean = true): ImportDescriptorResult

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ImportInsertHelper = project.service()
    }
}

enum class ImportDescriptorResult {
    FAIL,
    IMPORT_ADDED,
    ALREADY_IMPORTED
}

class ImportInsertHelperImpl(private val project: Project) : ImportInsertHelper() {
    override fun isImportedWithDefault(importPath: ImportPath, contextFile: CjFile): Boolean =
        isInDefaultImports(importPath, contextFile)

    private fun getCodeStyleSettings(contextFile: CjFile): CangJieCodeStyleSettings = contextFile.cangjieCustomSettings

    override fun isImportedWithLowPriorityDefaultImport(importPath: ImportPath, contextFile: CjFile): Boolean {
        val analyzerServices = contextFile.findAnalyzerServices(contextFile.project)
        return importPath.isImported(analyzerServices.defaultLowPriorityImports, analyzerServices.excludedImports)
    }

    override fun mayImportOnShortenReferences(descriptor: DeclarationDescriptor, contextFile: CjFile): Boolean {
        return when (val importableDescriptor = descriptor.getImportableDescriptor()) {
            is PackageViewDescriptor -> false // now package cannot be imported

            is ClassDescriptor -> allowClassImport(importableDescriptor, contextFile)

            else -> descriptor.getImportableDescriptor().containingDeclaration is PackageFragmentDescriptor // do not import members (e.g. java static members)
        }

    }
    private fun allowClassImport(classDescriptor: ClassDescriptor, contextFile: CjFile): Boolean {
        val nested = classDescriptor.containingDeclaration !is PackageFragmentDescriptor
        // If nested classes are blanket-prohibited from being imported, don't import any.
        if (nested && !getCodeStyleSettings(contextFile).IMPORT_NESTED_CLASSES) return false
        val classInfo = ClassImportFilter.ClassInfo(
            classDescriptor.fqNameSafe,
            classDescriptor.kind,
            classDescriptor.modality,
            classDescriptor.visibility.delegate,
            nested)
        return ClassImportFilter.allowClassImport(classInfo, contextFile)
    }

    override fun importDescriptor(
        element: CjElement,
        descriptor: DeclarationDescriptor,
        runImmediately: Boolean,
        forceAllUnderImport: Boolean,
        aliasName: Name?
    ): ImportDescriptorResult {
        val importer = Importer(element, runImmediately)
        return if (forceAllUnderImport) {
            importer.importDescriptorWithStarImport(descriptor)
        } else {
            importer.importDescriptor(descriptor, aliasName)
        }
    }

    private inner class Importer(
        private val element: CjElement,
        private val runImmediately: Boolean
    ) {
        private val file = element.getContainingCjFile()
        private val resolutionFacade = file.getResolutionFacade()
        private val languageVersionSettings = resolutionFacade.languageVersionSettings

        private fun alreadyImported(
            target: DeclarationDescriptor,
            scope: LexicalScope,
            targetFqName: FqName,
            targetName: Name = target.name,
        ): ImportDescriptorResult? {
            return when (target) {
                is ClassifierDescriptorWithTypeParameters -> {
                    val classifiers =
                        scope.findClassifiers(targetName, NoLookupLocation.FROM_IDE).takeIf { it.isNotEmpty() }
                            ?: return null
                    if (classifiers.all { it is TypeAliasDescriptor }) {
                        return when {
                            classifiers.all { it.importableFqName == targetFqName } -> ImportDescriptorResult.ALREADY_IMPORTED
                            // no actual conflict
                            classifiers.size == 1 -> null
                            else -> ImportDescriptorResult.FAIL
                        }
                    }

                    // kotlin.collections.ArrayList is not a conflict, it's an alias to java.util.ArrayList
                    val nonAliasClassifiers =
                        classifiers.filter { it !is TypeAliasDescriptor || it.importableFqName == targetFqName }
                    // top-level classifiers could/should be resolved with imports
                    if (nonAliasClassifiers.size > 1 && nonAliasClassifiers.all { it.containingDeclaration is PackageFragmentDescriptor }) {
                        return null
                    }

                    val classifier: ClassifierDescriptor =
                        nonAliasClassifiers.singleOrNull() ?: return ImportDescriptorResult.FAIL
                    ImportDescriptorResult.ALREADY_IMPORTED.takeIf { classifier.importableFqName == targetFqName }
                }

                is FunctionDescriptor ->
                    ImportDescriptorResult.ALREADY_IMPORTED.takeIf {
                        scope.findFunction(
                            targetName,
                            NoLookupLocation.FROM_IDE
                        ) { it.importableFqName == targetFqName } != null
                    }

                is PropertyDescriptor ->
                    ImportDescriptorResult.ALREADY_IMPORTED.takeIf {
                        scope.findVariable(
                            targetName,
                            NoLookupLocation.FROM_IDE
                        ) { it.importableFqName == targetFqName } != null
                    }

                else -> null
            }
        }

        private fun HierarchicalScope.findClassifiers(name: Name, location: LookupLocation): Set<ClassifierDescriptor> {
            val result = mutableSetOf<ClassifierDescriptor>()
            processForMeAndParent { it.getContributedClassifier(name, location)?.let(result::add) }
            return result
        }

//        fun importPsiClass(psiClass: PsiClass): ImportDescriptorResult {
//            val qualifiedName = psiClass.qualifiedName!!
//
//            val targetFqName = FqName(qualifiedName)
//            val name = Name.identifier(psiClass.name!!)
//
//            val scope = if (element == file) resolutionFacade.getFileResolutionScope(file) else element.getResolutionScope()
//
//            scope.findClassifier(name, NoLookupLocation.FROM_IDE)?.let {
//                return if (it.fqNameSafe == targetFqName) ImportDescriptorResult.ALREADY_IMPORTED else ImportDescriptorResult.FAIL
//            }
//
//            val imports = file.importDirectives
//
//            if (imports.any { !it.isAllUnder && (it.importPath?.alias == name || it.importPath?.fqName == targetFqName) }) {
//                return ImportDescriptorResult.FAIL
//            }
//
//            addImport(targetFqName, false)
//
//            return ImportDescriptorResult.IMPORT_ADDED
//        }

        fun importDescriptor(descriptor: DeclarationDescriptor, aliasName: Name?): ImportDescriptorResult {
            val target = descriptor.getImportableDescriptor()

            val name = aliasName ?: target.name
            val topLevelScope = resolutionFacade.getFileResolutionScope(file)

            // check if import is not needed
            val targetFqName = target.importableFqName ?: return ImportDescriptorResult.FAIL

            val scope = if (element == file) topLevelScope else element.getResolutionScope()

            alreadyImported(target, scope, targetFqName, name)?.let { return it }

            val imports = file.importDirectives
            for (import in imports) {
                val importPath = import.importPath ?: continue
                if (!importPath.isAllUnder && importPath.alias == aliasName && importPath.fqName == targetFqName) {
                    return ImportDescriptorResult.FAIL
                }
            }

            // check there is an explicit import of a class/package with the same name already
//            when (target) {
//                is ClassDescriptor -> scope.findClassifier(name, NoLookupLocation.FROM_IDE)
//                is PackageViewDescriptor -> scope.findPackage(name)
//                else -> null
//            }?.let { conflict ->
//                if (conflict.explicitlyImported(name, imports) || conflict.comesFromLocalScopes()) {
//                    return ImportDescriptorResult.FAIL
//                }
//            }

            val containerFqName = targetFqName.parent()
            val tryStarImport =
                aliasName == null && shouldTryStarImport(containerFqName, target, imports) && when (target) {
                    // this check does not give a guarantee that import with * will import the class - for example,
                    // there can be classes with conflicting name in more than one import with *
                    is ClassifierDescriptorWithTypeParameters -> topLevelScope.findClassifier(
                        name,
                        NoLookupLocation.FROM_IDE
                    ) == null

                    is FunctionDescriptor, is PropertyDescriptor -> true
                    else -> error("Unknown kind of descriptor to import:$target")
                }

            if (tryStarImport) {
                val result = addStarImport(target)
                if (result != ImportDescriptorResult.FAIL) return result
            }

            return addExplicitImport(target, aliasName)
        }

        fun importDescriptorWithStarImport(descriptor: DeclarationDescriptor): ImportDescriptorResult {
            val target = descriptor.getImportableDescriptor()

            val fqName = target.importableFqName ?: return ImportDescriptorResult.FAIL
            val containerFqName = fqName.parent()
            val imports = file.importDirectives

            val starImportPath = ImportPath(containerFqName, true)
            if (imports.any { it.importPath == starImportPath }) {
                return alreadyImported(target, resolutionFacade.getFileResolutionScope(file), fqName)
                    ?: ImportDescriptorResult.FAIL
            }

            if (!canImportWithStar(containerFqName, target)) return ImportDescriptorResult.FAIL

            return addStarImport(target)
        }

        private fun shouldTryStarImport(
            containerFqName: FqName,
            target: DeclarationDescriptor,
            imports: Collection<CjImportDirective>,
        ): Boolean {
            if (!canImportWithStar(containerFqName, target)) return false

            val starImportPath = ImportPath(containerFqName, true)
            if (imports.any { it.importPath == starImportPath }) return false

            val codeStyle = getCodeStyleSettings(file)
            if (containerFqName.asString() in codeStyle.PACKAGES_TO_USE_STAR_IMPORTS) return true

            val importsFromPackage = imports.count {
                val path = it.importPath
                path != null && !path.isAllUnder && !path.hasAlias() && path.fqName.parent() == containerFqName
            }

            val nameCountToUseStar = if (target.containingDeclaration is ClassDescriptor)
                codeStyle.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS
            else
                codeStyle.NAME_COUNT_TO_USE_STAR_IMPORT

            return importsFromPackage + 1 >= nameCountToUseStar
        }

        private fun canImportWithStar(containerFqName: FqName, target: DeclarationDescriptor): Boolean {
            if (containerFqName.isRoot) return false

            val container = target.containingDeclaration
            return !(container is ClassDescriptor /*&& container.kind == ClassKind.OBJECT*/) // cannot import with '*' from object
        }

        private fun addStarImport(targetDescriptor: DeclarationDescriptor): ImportDescriptorResult {
            val targetFqName = targetDescriptor.importableFqName!!
            val parentFqName = targetFqName.parent()

            val moduleDescriptor = resolutionFacade.moduleDescriptor
            val scopeToImport = getMemberScope(parentFqName, moduleDescriptor) ?: return ImportDescriptorResult.FAIL

            val filePackage = moduleDescriptor.getPackage(file.packageFqName)

            fun isVisible(descriptor: DeclarationDescriptor): Boolean {
                if (descriptor !is DeclarationDescriptorWithVisibility) return true
                val visibility = descriptor.visibility
                return !visibility.mustCheckInImports() || DescriptorVisibilityUtils.isVisibleIgnoringReceiver(
                    descriptor,
                    filePackage,
                    languageVersionSettings,
                )
            }

            val kindFilter = DescriptorKindFilter.ALL.withoutKinds(DescriptorKindFilter.PACKAGES_MASK)
            val allNamesToImport =
                scopeToImport.getDescriptorsFiltered(kindFilter).filter(::isVisible).map { it.name }.toSet()

            fun targetFqNameAndType(ref: CjReferenceExpression): Pair<FqName, Class<out Any>>? {
                val descriptors = ref.resolveTargets()
                val fqName: FqName? = descriptors.filter(::isVisible).map { it.importableFqName }.toSet().singleOrNull()
                return if (fqName != null) {
                    Pair(fqName, descriptors.elementAt(0).javaClass)
                } else null
            }

            val futureCheckMap = HashMap<CjSimpleNameExpression, Pair<FqName, Class<out Any>>>()
            file.accept(object : CjVisitorVoid() {
                override fun visitElement(element: PsiElement): Unit = element.acceptChildren(this)
                override fun visitImportList(importList: CjImportList) {}
                override fun visitPackageDirective(directive: CjPackageDirective) {}
                override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
                    val refName = expression.getReferencedNameAsName()
                    if (allNamesToImport.contains(refName)) {
                        val target = targetFqNameAndType(expression)
                        if (target != null) {
                            futureCheckMap += Pair(expression, target)
                        }
                    }
                }
            })

            val addedImport = addImport(parentFqName, true)

            if (alreadyImported(
                    targetDescriptor,
                    resolutionFacade.getFileResolutionScope(file),
                    targetFqName
                ) == null
            ) {
                runAction(runImmediately) { addedImport.delete() }
                return ImportDescriptorResult.FAIL
            }
            dropRedundantExplicitImports(parentFqName)

            val conflicts = futureCheckMap
                .mapNotNull { (expr, fqNameAndType) ->
                    if (targetFqNameAndType(expr) != fqNameAndType) fqNameAndType.first else null
                }
                .toSet()

            fun isNotImported(fqName: FqName): Boolean {
                return file.importDirectives.none { directive ->
                    !directive.isAllUnder && directive.alias == null && directive.importedFqName == fqName
                }
            }

            for (conflict in conflicts.filter(::isNotImported)) {
                addImport(conflict, false)
            }

            return ImportDescriptorResult.IMPORT_ADDED
        }

        private fun getMemberScope(fqName: FqName, moduleDescriptor: ModuleDescriptor): MemberScope? {
            val packageView = moduleDescriptor.getPackage(fqName)
            if (!packageView.isEmpty()) {
                return packageView.memberScope
            }

            val parentScope = getMemberScope(fqName.parent(), moduleDescriptor) ?: return null
            val classifier = parentScope.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_IDE)
            val classDescriptor = classifier as? ClassDescriptor ?: return null
            return classDescriptor.defaultType.memberScope
        }

        private fun addExplicitImport(target: DeclarationDescriptor, aliasName: Name?): ImportDescriptorResult {
            if (target is ClassDescriptor || target is PackageViewDescriptor) {
                val topLevelScope = resolutionFacade.getFileResolutionScope(file)
                val name = aliasName ?: target.name

                // check if there is a conflicting class imported with * import
                // (not with explicit import - explicit imports are checked before this method invocation)
                val classifier = topLevelScope.findClassifier(name, NoLookupLocation.FROM_IDE)
                if (classifier != null && detectNeededImports(listOf(classifier)).isNotEmpty()) {
                    return ImportDescriptorResult.FAIL
                }
            }

            addImport(target.importableFqName!!, false, aliasName)
            return ImportDescriptorResult.IMPORT_ADDED
        }

        private fun dropRedundantExplicitImports(packageFqName: FqName) {
            val dropCandidates = file.importDirectives.filter {
                !it.isAllUnder && it.aliasName == null && it.importPath?.fqName?.parent() == packageFqName
            }

            val importsToCheck = ArrayList<FqName>()
            for (import in dropCandidates) {
                if (import.importedReference == null) continue
                val targets = import.targetDescriptors()
                if (targets.any { it is PackageViewDescriptor }) continue // do not drop import of package
                val classDescriptor = targets.filterIsInstance<ClassDescriptor>().firstOrNull()
                importsToCheck.addIfNotNull(classDescriptor?.importableFqName)
                runAction(runImmediately) { import.delete() }
            }

            if (importsToCheck.isNotEmpty()) {
                val topLevelScope = resolutionFacade.getFileResolutionScope(file)
                for (classFqName in importsToCheck) {
                    val classifier = topLevelScope.findClassifier(classFqName.shortName(), NoLookupLocation.FROM_IDE)
                    if (classifier?.importableFqName != classFqName) {
                        addImport(classFqName, false) // restore explicit import
                    }
                }
            }
        }

        private fun detectNeededImports(importedClasses: Collection<ClassifierDescriptor>): Set<ClassifierDescriptor> {
            if (importedClasses.isEmpty()) return setOf()

            val classesToCheck = importedClasses.associateByTo(mutableMapOf()) { it.name }
            val result = LinkedHashSet<ClassifierDescriptor>()
            file.accept(object : CjVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    if (classesToCheck.isEmpty()) return
                    element.acceptChildren(this)
                }

                override fun visitImportList(importList: CjImportList) {
                }

                override fun visitPackageDirective(directive: CjPackageDirective) {
                }

                override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
                    if (CjPsiUtil.isSelectorInQualified(expression)) return

                    val refName = expression.getReferencedNameAsName()
                    val descriptor = classesToCheck[refName]
                    if (descriptor != null) {
                        val targetFqName = targetFqName(expression)
                        if (targetFqName != null && targetFqName == DescriptorUtils.getFqNameSafe(descriptor)) {
                            classesToCheck.remove(refName)
                            result.add(descriptor)
                        }
                    }
                }
            })
            return result
        }

        private fun targetFqName(ref: CjReferenceExpression): FqName? =
            ref.resolveTargets().map { it.importableFqName }.toSet().singleOrNull()

        private fun CjReferenceExpression.resolveTargets(): Collection<DeclarationDescriptor> =
            this.getImportableTargets(resolutionFacade.analyze(this, BodyResolveMode.PARTIAL))

        private fun addImport(fqName: FqName, allUnder: Boolean, aliasName: Name? = null): CjImportDirective {
            return runAction(runImmediately) {
                if (file.isPhysical) {
                    runWriteAction { addImport(project, file, fqName, allUnder, aliasName) }
                } else {
                    addImport(project, file, fqName, allUnder, aliasName)
                }
            }
        }
    }

    companion object {
        fun computeDefaultAndExcludedImports(contextFile: CjFile): Pair<List<ImportPath>, List<FqName>> {
            val languageVersionSettings = contextFile.getResolutionFacade().languageVersionSettings
            val analyzerServices = contextFile.findAnalyzerServices(contextFile.project)
            val allDefaultImports =
                analyzerServices.getDefaultImports(languageVersionSettings, includeLowPriorityImports = true)


            return (allDefaultImports) to analyzerServices.excludedImports
        }

        fun addImport(
            project: Project,
            file: CjFile,
            fqName: FqName,
            allUnder: Boolean = false,
            alias: Name? = null
        ): CjImportDirective {
            return file.addImport(fqName, allUnder, alias, project)
        }

        fun isInDefaultImports(importPath: ImportPath, contextFile: CjFile): Boolean {
            val (defaultImports, excludedImports) = computeDefaultAndExcludedImports(contextFile)
            return importPath.isImported(defaultImports, excludedImports)
        }
    }

}

private fun CjFile.findAnalyzerServices(project: Project): PlatformDependentAnalyzerServices {
    return PlatformDependentAnalyzerServicesImpl
}
