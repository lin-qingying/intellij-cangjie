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

package cn.cangnova.cangjie.ide.imports

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.ide.formatter.CangJieCodeStyleSettings
import cn.cangnova.cangjie.ide.formatter.cangjieCustomSettings
import cn.cangnova.cangjie.ide.util.ClassImportFilter
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.resolve.*
import cn.cangnova.cangjie.resolve.caches.getResolutionFacade
import cn.cangnova.cangjie.resolve.descriptorUtil.classId
import cn.cangnova.cangjie.resolve.descriptorUtil.fqNameSafe
import cn.cangnova.cangjie.resolve.descriptorUtil.getImportableDescriptor
import cn.cangnova.cangjie.resolve.descriptorUtil.targetDescriptors
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.resolve.scopes.*
import cn.cangnova.cangjie.utils.addIfNotNull
import cn.cangnova.cangjie.utils.runAction
import cn.cangnova.cangjie.utils.safeAs
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

    abstract fun importPsiClass(element: CjElement, psiClass: CjTypeStatement, runImmediately: Boolean = true): ImportDescriptorResult

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ImportInsertHelper = project.service()
    }
}

enum class ImportDescriptorResult {
    //                                  导入失败
    FAIL,

    //                            导入成功
    IMPORT_ADDED,

    //       导入成功，但需要重新格式化
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
            nested
        )
        return ClassImportFilter.allowClassImport(classInfo, contextFile)
    }

    override fun importPsiClass(element: CjElement, psiClass: CjTypeStatement, runImmediately: Boolean): ImportDescriptorResult {
        return Importer(element, runImmediately).importPsiClass(psiClass)
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
        fun importPsiClass(psiClass: CjTypeStatement): ImportDescriptorResult {


            val targetFqName = psiClass.fqName
            val name = Name.identifier(psiClass.name!!)

            val scope = if (element == file) resolutionFacade.getFileResolutionScope(file) else element.getResolutionScope()

            scope.findClassifier(name, NoLookupLocation.FROM_IDE)?.let {
                return if (it.fqNameSafe == targetFqName) ImportDescriptorResult.ALREADY_IMPORTED else ImportDescriptorResult.FAIL
            }

            val imports = file.importDirectivesItem

            if (imports.any { !it.isAllUnder && (it.importPath?.alias == name || it.importPath?.fqName == targetFqName) }) {
                return ImportDescriptorResult.FAIL
            }

            targetFqName?.let { addImport(it, false) }

            return ImportDescriptorResult.IMPORT_ADDED
        }
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
//            val imports = file.importDirectivesItem
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

            // 检查是否不需要导入
            val targetFqName = target.importableFqName ?: return ImportDescriptorResult.FAIL

            val scope = if (element == file) topLevelScope else element.getResolutionScope()

            alreadyImported(target, scope, targetFqName, name)?.let { return it }

            val imports = file.importDirectivesItem
            for (import in imports) {
                val importPath = import.importPath ?: continue
                if (!importPath.isAllUnder && importPath.alias == aliasName && importPath.fqName == targetFqName) {
                    return ImportDescriptorResult.FAIL
                }
            }

            // 检查是否已显式导入具有相同名称的类/包
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


        private fun DeclarationDescriptor.comesFromLocalScopes(): Boolean =
            // local class
            DescriptorUtils.isLocal(this) ||
                    // nested class
                    this.safeAs<ClassifierDescriptor>()?.classId?.isNestedClass == true

        private fun DeclarationDescriptor.explicitlyImported(
            name: Name,
            imports: List<CjImportDirectiveItem>
        ) = imports.any {
            !it.isAllUnder && it.importPath?.fqName == importableFqName && it.importPath?.importedName == name
        }

        fun importDescriptorWithStarImport(descriptor: DeclarationDescriptor): ImportDescriptorResult {
            val target = descriptor.getImportableDescriptor()

            val fqName = target.importableFqName ?: return ImportDescriptorResult.FAIL
            val containerFqName = fqName.parent()
            val imports = file.importDirectivesItem

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
            imports: Collection<CjImportDirectiveItem>,
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
                    val refName = expression.referencedNameAsName
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
                return file.importDirectivesItem.none { directive ->
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
//           导入项是否于包拆分
            var isPackageSplit = false

            if (target is ClassDescriptor || target is PackageViewDescriptor) {
                val topLevelScope = resolutionFacade.getFileResolutionScope(file)
                val name = aliasName ?: target.name

                // check if there is a conflicting class imported with * import
                // (not with explicit import - explicit imports are checked before this method invocation)
                val classifier = topLevelScope.findClassifier(name, NoLookupLocation.FROM_IDE)
                if (classifier?.name == target.name) {
                    isPackageSplit = true
                }
                if (classifier != null && detectNeededImports(listOf(classifier)).isNotEmpty()) {
                    return ImportDescriptorResult.FAIL
                }
            }

            addImport(target.importableFqName!!, false, aliasName, isPackageSplit = isPackageSplit)
//            return if (isPackageSplit)
//                ImportDescriptorResult.ALREADY_IMPORTED
//            else
            return  ImportDescriptorResult.IMPORT_ADDED
        }

        private fun dropRedundantExplicitImports(packageFqName: FqName) {
            val dropCandidates = file.importDirectivesItem.filter {
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

                    val refName = expression.referencedNameAsName
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

        private fun addImport(
            fqName: FqName,
            allUnder: Boolean,
            aliasName: Name? = null,
            isPackageSplit: Boolean = false
        ): CjImportDirective {
            return runAction(runImmediately) {
                if (file.isPhysical) {
                    runWriteAction { addImport(project, file, fqName, allUnder, aliasName, isPackageSplit) }
                } else {
                    addImport(project, file, fqName, allUnder, aliasName, isPackageSplit)
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
            alias: Name? = null,
            isPackageSplit: Boolean = false
        ): CjImportDirective {
            return file.addImport(fqName, allUnder, alias, project, isPackageSplit)
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
