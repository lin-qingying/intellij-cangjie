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

package cn.cangnova.cangjie.cjpm.project.workspace

import cn.cangnova.cangjie.cjpm.project.model.CjpmProject
import cn.cangnova.cangjie.cjpm.project.model.cjpmProjects
import cn.cangnova.cangjie.cjpm.project.workspace.PackageOrigin.*
import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.ide.project.moduletype.CangJieLibraryModuleType
import cn.cangnova.cangjie.toolchain.impl.CangJieVersion
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleWithNameAlreadyExists
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class CjAdditionalLibraryRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> =
        project.cjpmProjects.allProjects.smartFlatMap { it.ideaLibraries }

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> =
        getAdditionalProjectLibraries(project).flatMap { it.sourceRoots }


    companion object {

        fun findLibrarysByCjFile(project: Project, virtualFile: VirtualFile): List<SyntheticLibrary> {


            val librarys = getCjpmLibrarys(project)

            val tempLibrarys = mutableListOf<SyntheticLibrary>()

            librarys.forEach {
                if (it.contains(virtualFile))
                    tempLibrarys.add(it)
            }


            return tempLibrarys
        }

        fun getCjpmLibrarys(project: Project): MutableCollection<SyntheticLibrary> {
            return EP_NAME.extensionList.filter {
                it is CjAdditionalLibraryRootsProvider
            }.first().getAdditionalProjectLibraries(project)
        }

    }
}

private fun <U, V> Collection<U>.smartFlatMap(transform: (U) -> Collection<V>): Collection<V> =
    when (size) {
        0 -> emptyList()
        1 -> transform(first())
        else -> this.flatMap(transform)
    }


private val CjpmProject.ideaLibraries: Collection<SyntheticLibrary>
    get() {
        val workspace = workspace ?: return emptyList()
        val stdlibPackages = mutableListOf<CjpmWorkspace.Package>()
        val dependencyPackages = mutableListOf<CjpmWorkspace.Package>()
        for (pkg in workspace.packages) {
            when (pkg.origin) {
                STDLIB, STDLIB_DEPENDENCY -> stdlibPackages += pkg
                DEPENDENCY -> dependencyPackages += pkg
                WORKSPACE -> Unit
            }
        }

        return buildList {


            makeStdlibLibrary(stdlibPackages, cjcInfo?.version)
                ?.apply {

                    invokeLater {
                        runWriteAction {
                            try {
                                ModuleManager.getInstance(project)
                                    .newModule(sourceRoots.first().path, CangJieLibraryModuleType.ID)
                            } catch (_: ModuleWithNameAlreadyExists) {

                            }
                        }
                    }
                }
                ?.let(this::add)
            for (pkg in dependencyPackages) {
                pkg.toCjpmLibrary()?.apply {

                    invokeLater {
                        runWriteAction {
                            try {
                                ModuleManager.getInstance(project)
                                    .newModule(sourceRoots.first().path, CangJieLibraryModuleType.ID)
                            } catch (_: ModuleWithNameAlreadyExists) {

                            }
                        }
                    }


                }?.let(this::add)
            }
            GeneratedCodeFakeLibrary.create(this@ideaLibraries)?.let(::add)
        }
    }


private fun makeStdlibLibrary(packages: List<CjpmWorkspace.Package>, cjcVersion: CangJieVersion?): CjpmLibrary? {
    if (packages.isEmpty()) return null
    val sourceRoots = mutableSetOf<VirtualFile>()
    val excludedRoots = mutableSetOf<VirtualFile>()
    for (pkg in packages) {
        val root = pkg.contentRoot ?: continue
        sourceRoots += root
        sourceRoots += pkg.additionalRoots()
    }

    for (root in sourceRoots) {
        excludedRoots += listOfNotNull(
            root.findChild("tests"),
            root.findChild("benches"),
            root.findChild("examples"),
            root.findChild("ci"), // From `backtrace`
            root.findChild(".github"), // From `backtrace`

        )
    }

    val version = cjcVersion?.semver?.parsedVersion
    return CjpmLibrary("stdlib", sourceRoots, excludedRoots, CangJieIcons.CANGJIE_FILE, version)
}


class CjpmLibrary(
    val name: String,
    private val sourceRoots: Set<VirtualFile>,
    private val excludedRoots: Set<VirtualFile>,
    private val icon: Icon,
    private val version: String?
) : SyntheticLibrary(), ItemPresentation {
    override fun equals(other: Any?): Boolean = other is CjpmLibrary && other.sourceRoots == sourceRoots

    override fun hashCode(): Int = sourceRoots.hashCode()

    override fun getPresentableText(): String = if (version != null) "$name $version" else name
    override fun getLocationString(): String? = null

    override fun getIcon(p0: Boolean): Icon = icon
    override fun getExcludedRoots(): Set<VirtualFile> = excludedRoots

    override fun getSourceRoots(): Collection<VirtualFile> = sourceRoots


}

class GeneratedCodeFakeLibrary(private val sourceRoots: Set<VirtualFile>) : SyntheticLibrary() {
    override fun equals(other: Any?): Boolean {
        return other is GeneratedCodeFakeLibrary && other.sourceRoots == sourceRoots
    }

    override fun getSourceRoots(): Collection<VirtualFile> = sourceRoots

    override fun hashCode(): Int = sourceRoots.hashCode()

    override fun isShowInExternalLibrariesNode(): Boolean = false

    companion object {
        fun create(cjpmProject: CjpmProject): GeneratedCodeFakeLibrary? {
            val generatedRoots = cjpmProject.workspace?.packages.orEmpty().mapNotNullTo(HashSet()) { it.outDir }
            return if (generatedRoots.isEmpty()) null else GeneratedCodeFakeLibrary(generatedRoots)
        }
    }

}

private fun CjpmWorkspace.Package.toCjpmLibrary(): CjpmLibrary? {
    val root = contentRoot ?: return null
    val sourceRoots = mutableSetOf<VirtualFile>().apply {
        add(root)
    }
    val excludedRoots = mutableSetOf<VirtualFile>()

    return CjpmLibrary(name, sourceRoots, excludedRoots, CangJieIcons.CANGJIE_FILE, version)
}
