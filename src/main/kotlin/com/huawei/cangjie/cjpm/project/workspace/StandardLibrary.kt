package com.huawei.cangjie.cjpm.project.workspace

import CjpmWorkspaceData
import PackageId
import com.huawei.cangjie.cjpm.project.model.CjcInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.utils.FileUtils
import com.linqingying.utils.FileUtils.getTopLevelDirectories
import java.nio.file.Path

//
data class StandardLibrary(
    val workspaceData: CjpmWorkspaceData,
    val isHardcoded: Boolean,
    val isPartOfCjpmProject: Boolean = false
) {
    val packages: List<CjpmWorkspaceData.Package> get() = workspaceData.packages

    companion object {
        fun fromFileStdlib(path: Path, version: String): StandardLibrary {
// TODO 标准库验证
            if (!FileUtils.verifyFileList(path)) {
                throw IllegalArgumentException("Invalid standard library file")
            }


            val packages = mutableMapOf<PackageId, CjpmWorkspaceData.Package>()
            val currentFileList = FileUtils.generateFileList(path, false)
            val nameSet = currentFileList.getTopLevelDirectories()
//            val dependencies = mutableMapOf<PackageId, MutableSet<CjpmWorkspaceData.Dependency>>()
//            val version = "0.53.4"

            for (name in nameSet) {
                packages[name] = CjpmWorkspaceData.Package(
                    path.resolve(name).toVirtualFile().toString(),
                    name, version, PackageOrigin.STDLIB
                )


            }
            val data = CjpmWorkspaceData(packages.values.toList(), path.toVirtualFile().toString())
            return StandardLibrary(workspaceData = data, isHardcoded = true)

        }
    }
}

fun Path.toVirtualFile(): VirtualFile? {
    return VfsUtil.findFile(this, true)
}

object AutoInjectedCrates {
    const val STD: String = "std"
    const val CORE: String = "core"
    const val TEST: String = "test"
}
