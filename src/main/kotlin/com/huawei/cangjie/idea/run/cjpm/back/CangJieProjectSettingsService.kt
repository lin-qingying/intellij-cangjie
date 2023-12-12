//package com.huawei.cangjie.idea.run.cjpm.back
//
//import com.huawei.cangjie.idea.run.cjpm.CjToolchainBase
//import com.huawei.cangjie.idea.run.cjpm.back.Cjc
//import com.huawei.cangjie.idea.run.cjpm.back.Cjpm
//import com.huawei.cangjie.lang.sdk.CangJieSdkManager
//import com.intellij.openapi.components.*
//import com.intellij.openapi.extensions.ExtensionPointName
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.projectRoots.Sdk
//import com.intellij.openapi.util.SystemInfo
//import com.intellij.util.io.isDirectory
//import com.intellij.util.io.systemIndependentPath
//import com.intellij.util.xmlb.annotations.Transient
//import java.nio.file.Files
//import java.nio.file.Path
//import java.nio.file.Paths
//
//private const val SERVICE_NAME: String = "CangJieProjectSettings"
//
//@State(
//    name = SERVICE_NAME, storages = [
//        Storage(StoragePathMacros.WORKSPACE_FILE),
//        Storage("misc.xml", deprecated = true)
//    ]
//)
//class CangJieProjectSettingsService(
//    project: Project
//) : CjProjectSettingsServiceBase<CangJieProjectSettingsService.CangJieProjectSettings>(
//    project,
//    CangJieProjectSettings()
//) {
//    val useOffline: Boolean get() = myState.useOffline
//    val toolchain: CjToolchainBase? get() = myState.toolchain
//    val compileAllTargets: Boolean get() = myState.compileAllTargets
//
//    class CangJieProjectSettings : CjProjectSettingsBase<CangJieProjectSettings>() {
//        override fun copy(): CangJieProjectSettings {
//            val myState = CangJieProjectSettings()
//            myState.copyFrom(this)
//            return myState
//        }
//
//        var useOffline by property(false)
//
//        @AffectsHighlighting
//        var compileAllTargets by property(true)
//        var toolchainHomeDirectory by string()
//
//        @get:Transient
//        @set:Transient
//        var toolchain: CjToolchainBase?
//            get() = CjToolchainProvider.getToolchain(CangJieSdkManager.getProjectSdk())
//            //            get() = toolchainHomeDirectory?.let { CjToolchainProvider.getToolchain(Paths.get(it)) }
//            set(value) {
//                toolchainHomeDirectory = value?.location?.systemIndependentPath
//            }
//    }
//}
//
//abstract class CjProjectSettingsServiceBase<T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>>(
//    val project: Project,
//    myState: T
//) : SimplePersistentStateComponent<T>(myState) {
//    @Retention(AnnotationRetention.RUNTIME)
//    @Target(AnnotationTarget.PROPERTY)
//    protected annotation class AffectsHighlighting
//    abstract class CjProjectSettingsBase<T : CjProjectSettingsBase<T>> : BaseState() {
//        abstract fun copy(): T
//    }
//}
//
//interface CjToolchainProvider {
//    fun getToolchain(homePath: Path): CjToolchainBase?
//
//    companion object {
//        private val EP_NAME: ExtensionPointName<CjToolchainProvider> =
//            ExtensionPointName.create("com.huawei.cangjie.toolchainProvider")
//
//        fun getToolchain(homePath: Path): CjToolchainBase? =
//            EP_NAME.extensionList.asSequence()
//                .mapNotNull { it.getToolchain(homePath) }
//                .firstOrNull()
//
//        fun getToolchain(sdk: Sdk?): CjToolchainBase? {
//            if (sdk == null) return null
//            val homePath = Paths.get(sdk.homePath)
//            return getToolchain(homePath)
//
//        }
//    }
//}
//
//abstract class CjToolchainFlavor {
//
//    fun suggestHomePaths(): Sequence<Path> = getHomePathCandidates().filter { isValidToolchainPath(it) }
//
//    protected abstract fun getHomePathCandidates(): Sequence<Path>
//
//    /**
//     * Flavor is added to result in [getApplicableFlavors] if this method returns true.
//     * @return whether this flavor is applicable.
//     */
//    protected open fun isApplicable(): Boolean = true
//
//    /**
//     * Checks if the path is the name of a Rust toolchain of this flavor.
//     *
//     * @param path path to check.
//     * @return true if paths points to a valid home.
//     */
//    protected open fun isValidToolchainPath(path: Path): Boolean {
//        return path.isDirectory() &&
//                hasExecutable(path, Cjc.NAME) &&
//                hasExecutable(path, Cjpm.NAME)
//    }
//
//    protected open fun hasExecutable(path: Path, toolName: String): Boolean = path.hasExecutable(toolName)
//
//    protected open fun pathToExecutable(path: Path, toolName: String): Path = path.pathToExecutable(toolName)
//
//    companion object {
//        private val EP_NAME: ExtensionPointName<CjToolchainFlavor> =
//            ExtensionPointName.create("com.huawei.cangjie.toolchainFlavor")
//
//        fun getApplicableFlavors(): List<CjToolchainFlavor> =
//            EP_NAME.extensionList.filter { it.isApplicable() }
//
//        fun getFlavor(path: Path): CjToolchainFlavor? =
//            getApplicableFlavors().find { flavor -> flavor.isValidToolchainPath(path) }
//    }
//}
//
//fun Path.hasExecutable(toolName: String): Boolean = pathToExecutable(toolName).isExecutable()
//fun Path.pathToExecutable(toolName: String): Path {
//    val exeName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
//    return resolve(exeName).toAbsolutePath()
//}
//
//fun Path.isExecutable(): Boolean = Files.isExecutable(this)
