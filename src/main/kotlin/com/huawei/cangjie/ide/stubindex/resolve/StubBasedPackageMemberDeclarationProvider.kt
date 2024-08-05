package com.huawei.cangjie.ide.stubindex.resolve

import com.huawei.cangjie.ide.indices.CangJiePackageIndexUtils
import com.huawei.cangjie.ide.stubindex.*
import com.huawei.cangjie.ide.vfilefinder.CangJiePackageSourcesMemberNamesIndex
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.safeNameForLazyResolve
import com.huawei.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.CommonProcessors
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.annotations.ApiStatus

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
        CangJieTopLevelFunctionFqnNameIndex[childName(name), project, searchScope]
    }

    private fun childName(name: Name): String {
        return fqName.child(name.safeNameForLazyResolve()).asString()
    }

    override fun getVariableDeclarations(name: Name): Collection<CjProperty> {
        TODO("Not yet implemented")
    }

    override fun getDestructuringDeclarationsEntries(name: Name): Collection<CjDestructuringDeclarationEntry> {
        TODO("Not yet implemented")
    }

    override fun getTypeAliasDeclarations(name: Name): Collection<CjTypeAlias> {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
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
@Suppress("NOTHING_TO_INLINE")
inline fun isUnitTestMode(): Boolean = ApplicationManager.getApplication().isUnitTestMode

@Suppress("NOTHING_TO_INLINE")
inline fun isApplicationInternalMode(): Boolean = ApplicationManager.getApplication().isInternal
