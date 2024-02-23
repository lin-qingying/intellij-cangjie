package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.CjpmProjectsService
import com.huawei.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.huawei.cangjie.utils.checkReadAccessAllowed
import com.huawei.cangjie.utils.checkWriteAccessAllowed
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.LightDirectoryIndex
import java.util.*

class CjpmPackageIndex(
    private val project: Project,
    private val service: CjpmProjectsService
) : CjpmProjectsService.CjpmProjectsListener {
    private val indices: MutableMap<CjpmProject, LightDirectoryIndex<Optional<CjpmWorkspace.Package>>> = hashMapOf()
    private var indexDisposable: Disposable? = null

    init {
        project.messageBus.connect(project).subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, this)
    }


    override fun cjpmProjectsUpdated(service: CjpmProjectsService, projects: Collection<CjpmProject>) {
        checkWriteAccessAllowed()
        resetIndex()
        val disposable = Disposer.newDisposable("CjpmPackageIndexDisposable")
        Disposer.register(project, disposable)

        for (cjpmProject in projects) {
            val packages = cjpmProject.workspace?.packages.orEmpty()
            indices[cjpmProject] = LightDirectoryIndex(disposable, Optional.empty()) { index ->
                for (pkg in packages) {
                    val info = Optional.of(pkg)
                    index.putInfo(pkg.contentRoot, info)
//                    index.putInfo(pkg.outDir, info)
//                    for (additionalRoot in pkg.additionalRoots()) {
//                        index.putInfo(additionalRoot, info)
//                    }

                }
            }
        }
        indexDisposable = disposable

    }


    fun findPackageForFile(file: VirtualFile): CjpmWorkspace.Package? {
        checkReadAccessAllowed()
        val cjpmProject = service.findProjectForFile(file) ?: return null
        return indices[cjpmProject]?.getInfoForFile(file)?.orElse(null)
    }

    private fun resetIndex() {
        val disposable = indexDisposable
        if (disposable != null) {
            Disposer.dispose(disposable)
        }
        indexDisposable = null
        indices.clear()
    }
}
