package com.huawei.cangjie.idea.projectStructure.moduleInfo

import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
interface NonSourceModuleInfoBase : ModuleInfo


class NotUnderContentRootModuleInfo(
    override val project: Project,
    file: CjFile?
) :   IdeaModuleInfo, NonSourceModuleInfoBase {
    @Deprecated("Backing 'CjFile' expected")
    constructor(project: Project) : this(project, null)

    private val filePointer: SmartPsiElementPointer<CjFile>? = file?.let { SmartPointerManager.createPointer(it.originalFile as CjFile) }

    val file: CjFile?
        get() = filePointer?.element

//    override val moduleOrigin: ModuleOrigin
//        get() = ModuleOrigin.OTHER

    override val name: Name = Name.special("<special module for files not under source root>")

//    override val displayedName: String
//        get() = KotlinBaseProjectStructureBundle.message("special.module.for.files.not.under.source.root")
//
//    override val contentScope: GlobalSearchScope
//        get() = file?.let(GlobalSearchScope::fileScope) ?: GlobalSearchScope.EMPTY_SCOPE

    //TODO: (module refactoring) dependency on runtime can be of use here
//    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)
//    override fun dependenciesWithoutSelf(): Sequence<IdeaModuleInfo> = emptySequence()
//
//    override val platform: TargetPlatform
//        get() = JvmPlatforms.defaultJvmPlatform
//
//    override val analyzerServices: PlatformDependentAnalyzerServices
//        get() = platform.single().findAnalyzerServices()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotUnderContentRootModuleInfo

        if (project != other.project) return false
        return filePointer == other.filePointer
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + (filePointer?.hashCode() ?: 0)
        return result
    }
}
