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

package org.cangnova.cangjie.stubindex.resolve

import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.safeNameForLazyResolve
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.CommonProcessors
import com.intellij.util.indexing.FileBasedIndex
import org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider
import org.cangnova.cangjie.descriptors.data.CjClassInfoUtil
import org.cangnova.cangjie.descriptors.data.CjTypeStatementInfo
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.stubindex.CangJieMacroDeclarationFqNameIndex
import org.cangnova.cangjie.stubindex.*
import org.cangnova.cangjie.stubindex.CangJieTopLevelClassByPackageIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelFunctionByPackageIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelFunctionFqNameIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelTypeAliasByPackageIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelVariableByPackageIndex
import org.cangnova.cangjie.utils.isApplicationInternalMode

private val isShortNameFilteringEnabled: Boolean by lazy { Registry.`is`("cangjie.indices.short.names.filtering.enabled") }

class StubBasedPackageMemberDeclarationProvider(
    private val fqName: FqName,
    private val project: Project,
    private val searchScope: GlobalSearchScope
) : PackageMemberDeclarationProvider {
    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<CjDeclaration> {
        val fqNameAsString = fqName.asString()
        val result = ArrayList<CjDeclaration>()

        fun addFromIndex(helper: CangJieStringStubIndexHelper<out CjNamedDeclaration>) {
            helper.processElements(fqNameAsString, project, searchScope) {
                if (nameFilter(it.nameAsSafeName)) {
                    result.add(it)
                }
                true
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            addFromIndex(CangJieTopLevelClassByPackageIndex)
            addFromIndex(CangJieTopLevelTypeAliasByPackageIndex)
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            addFromIndex(CangJieTopLevelFunctionByPackageIndex)
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            addFromIndex(CangJieTopLevelVariableByPackageIndex)
        }

        return result
    }

    override fun getFunctionDeclarations(name: Name): Collection<CjNamedFunction> = runReadAction {
        CangJieTopLevelFunctionFqNameIndex[childName(name), project, searchScope]
    }

    override fun getMacroDeclarations(name: Name): Collection<CjMacroDeclaration> = runReadAction {

        CangJieMacroDeclarationFqNameIndex[childName(name), project, searchScope]
    }

    override fun getMainFunctionDeclarations(): Collection<CjMainFunction> = runReadAction {
        CangJieMainFunctionFqNameIndex[moduleChildName(MAIN), project, searchScope]

    }

    private fun moduleChildName(name: Name): String {
        return FqName(fqName.moduleName.asString()).child(name.safeNameForLazyResolve()).asString()
    }

    private fun childName(name: Name): String {
        return fqName.child(name.safeNameForLazyResolve()).asString()
    }

    override fun getVariableDeclarations(name: Name): Collection<CjVariable<*>> = runReadAction {
        CangJieTopLevelVariableFqNameIndex[childName(name), project, searchScope]

    }

    override fun getPropertyDeclarations(name: Name): Collection<CjProperty> {
        // prop 只能在类成员中，不存在顶层属性
        return emptyList()
    }





    override fun getAliasTypeStatementDeclarations(name: Name): Collection<CjTypeAlias> {

        return runReadAction {

            CangJieTypeAliasByExpansionShortNameIndex[name.asString(), project, searchScope]

        }

    }



    override fun getTypeStatementDeclarations(name: Name): Collection<CjTypeStatementInfo<*>> {
        val childName = childName(name)
        if (isShortNameFilteringEnabled && !name.isSpecial) {
            val shortNames = ShortNamesCacheService.getInstance(project).getShortNameCandidates(name.asString())
            if (childName !in shortNames) {
                return emptyList()
            }
        }
        val cjTypeStatements = runReadAction {
            val results = arrayListOf<CjTypeStatementInfo<*>>()
            CangJieFullClassNameIndex.processElements(childName, project, searchScope) {
                ProgressManager.checkCanceled()
                results += CjClassInfoUtil.createTypeStatementInfo(it)
                true
            }
            results
        }
        return cjTypeStatements
    }

    override fun getTypeAliasDeclarations(name: Name): Collection<CjTypeAlias> {
        return CangJieTopLevelTypeAliasFqNameIndex[childName(name), project, searchScope]

    }

    private val _declarationNames: Set<Name> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val names = hashSetOf<Name>()
        runReadAction {
            FileBasedIndex.getInstance()
                .processValues(
                    CangJiePackageSourcesMemberNamesIndex.NAME,
                    fqName.asString(),
                    null,
                    FileBasedIndex.ValueProcessor { _, values ->
                        ProgressManager.checkCanceled()
                        for (value in values) {
                            names += Name.identifier(value).safeNameForLazyResolve()
                        }
                        true
                    }, searchScope
                )
        }
        names
    }

    override fun getDeclarationNames(): Set<Name> = _declarationNames

    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName> {
        return CangJiePackageIndexUtils.getSubPackageFqNames(fqName, searchScope, nameFilter)

    }

    override fun getPackageFiles(): Collection<CjFile> {
        return CangJiePackageIndexUtils.findFilesWithExactPackage(fqName, searchScope, project)

    }

    override fun containsFile(file: CjFile): Boolean {
        return searchScope.contains(file.virtualFile ?: return false)

    }

    @OptIn(IntellijInternalApi::class)

    fun checkClassOrStructDeclarations(name: Name) {
        val childName = childName(name)
        if (CangJieFullClassNameIndex.get(childName, project, searchScope).isEmpty()) {
            val processor = object : CommonProcessors.FindFirstProcessor<String>() {
                override fun accept(t: String?): Boolean = childName == t
            }
            CangJieFullClassNameIndex.processAllKeys(searchScope, null, processor)
            val everyObjects =
                CangJieFullClassNameIndex.get(childName, project, GlobalSearchScope.everythingScope(project))
            if (processor.isFound || everyObjects.isNotEmpty()) {
                project.messageBus.syncPublisher(CangJieCorruptedIndexListener.TOPIC).corruptionDetected()

                throw IllegalStateException(
                    """
                     | CangJieFullClassNameIndex ${if (processor.isFound) "has" else "has not"} '$childName' key.
                     | No value for it in $searchScope.
                     | Everything scope has ${everyObjects.size} objects${if (everyObjects.isNotEmpty()) " locations: ${everyObjects.map { it.containingFile.virtualFile }}" else ""}.
                     | 
                     | ${if (everyObjects.isEmpty()) "Please try File -> ${if (isApplicationInternalMode()) "Cache recovery -> " else ""}Repair IDE" else ""}
                    """.trimMargin()
                )
            }
        }
    }
}


