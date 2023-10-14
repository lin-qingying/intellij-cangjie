package com.huawei.cangjie.lang.core.psi

import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin
import com.huawei.cangjie.ide.injected.isDoctestInjection
import com.huawei.cangjie.lang.CjFileType
import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.completion.getOriginalOrSelf
import com.huawei.cangjie.lang.core.create.Crate
import com.huawei.cangjie.lang.core.create.crateGraph
import com.huawei.cangjie.lang.core.create.impl.FakeDetachedCrate
import com.huawei.cangjie.lang.core.create.impl.FakeInvalidCrate
import com.huawei.cangjie.lang.core.psi.ext.CjMod
import com.huawei.cangjie.lang.core.resolve.DefMapService

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker


class CjFile(
    fileViewProvider: FileViewProvider
)  : CjFileBase(fileViewProvider)  ,CjMod{
    override val `super`: CjMod? = null

    override val modName: String?
        get() {
            return  parent?.name
        }


    override val isCrateRoot: Boolean
        get() {
            val file = originalFile.virtualFile ?: return false
            return file is VirtualFileWithId && project.crateGraph.findCrateByRootMod(file) != null
                    || file.isDoctestInjection(project)
        }
    override val crateRelativePath: String? get() = CjPsiImplUtil.modCrateRelativePath(this)
    override val containingMod: CjMod get() = getOriginalOrSelf()
    override val crateRoot: CjMod? get() = cachedData.crateRoot

    private val CACHED_DATA_KEY: Key<CachedValue<CachedData>> = Key.create("CACHED_DATA_KEY")
    @Volatile
    private var forcedCachedData: (() -> CachedData)? = null
    private var hasForcedStubTree: Boolean = false
    private val cachedData: CachedData
        get() {
            forcedCachedData?.let { return it() }

            val originalFile = originalFile
            if (originalFile != this) {
                return (originalFile as? CjFile)?.cachedData
                    ?: CachedData(crate = FakeInvalidCrate(project))
            }

            val key = CACHED_DATA_KEY
            return CachedValuesManager.getCachedValue(this, key) {
                val value = doGetCachedData()
                val modificationTracker: Any = when {
                   virtualFile is VirtualFileWindow -> PsiModificationTracker.MODIFICATION_COUNT
                    value.crate.origin == PackageOrigin.WORKSPACE -> project.cangjieStructureModificationTracker
                    else -> project.cangjiePsiManager.cangjieStructureModificationTrackerInDependencies
                }
                CachedValueProvider.Result(value, modificationTracker)
            }
        }


    private fun doGetCachedData(): CachedData {
        check(originalFile == this)

        val virtualFile = virtualFile
            ?: return CachedData(crate = FakeDetachedCrate(this, id = -1, dependencies = emptyList()))




        val stdlibCrates = project.crateGraph.topSortedCrates
            .filter { it.origin == PackageOrigin.STDLIB }
            .map { Crate.Dependency(it.normName, it) }

        val crate = FakeDetachedCrate(this, DefMapService.getNextNonCargoCrateId(), dependencies = stdlibCrates)
        val cjpmProject = project.cjpmProjects.findProjectForFile(virtualFile) ?: return CachedData(crate = crate)
        return CachedData(cjpmProject, crate = crate)
    }
}

private data class CachedData(
    val cargoProject: CjpmProject? = null,

    val crateRoot: CjFile? = null,
    val crate: Crate,
    val crates: List<Crate> = emptyList(),
    val isDeeplyEnabledByCfg: Boolean = true,
    val isIncludedByIncludeMacro: Boolean = false,

    val macroExpansionDepth: Int = 0
)
abstract class CjFileBase(fileViewProvider: FileViewProvider) : PsiFileBase(fileViewProvider, CjLanguage)    {


    override fun getOriginalFile(): CjFileBase = super.getOriginalFile() as CjFileBase

    override fun getFileType(): FileType = CjFileType

 }
