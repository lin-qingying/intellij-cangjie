package com.huawei.cangjie.cjpm.project.workspace

import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin.*
import com.huawei.cangjie.cjpm.toolchain.impl.CjcVersion
import com.huawei.cangjie.idea.icons.CangJieIcons
import com.intellij.navigation.ItemPresentation
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
            makeStdlibLibrary(stdlibPackages, cjcInfo?.version)?.let(this::add)
            for (pkg in dependencyPackages) {
                pkg.toCjpmLibrary()?.let(this::add)
            }
            GeneratedCodeFakeLibrary.create(this@ideaLibraries)?.let(::add)
        }
    }


private fun makeStdlibLibrary(packages: List<CjpmWorkspace.Package>, rustcVersion: CjcVersion?): CjpmLibrary? {
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
            root.findChild("libc-test") // From Rust 1.32.0 `liblibc`
        )
    }

    val version = rustcVersion?.semver?.parsedVersion
    return CjpmLibrary("stdlib", sourceRoots, excludedRoots, CangJieIcons.CANGJIE_FILE, version)
}


class CjpmLibrary(
    private val name: String,
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
