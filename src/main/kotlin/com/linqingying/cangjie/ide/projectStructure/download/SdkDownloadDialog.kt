package com.linqingying.cangjie.ide.projectStructure.download

import com.google.common.hash.Hashing
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslPath
import com.intellij.ide.DataManager

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.progress.util.RelayUiToDelegateIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle


import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask


import com.intellij.openapi.ui.BrowseFolderRunnable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.util.Urls
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.delete
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.CjToolchainPathChoosingComboBox
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.linqingying.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
import com.linqingying.cangjie.utils.AbstractForegroundTask
import org.jetbrains.annotations.Nls

import java.awt.Component
import java.awt.event.ItemEvent
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.function.Predicate

import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import kotlin.concurrent.withLock
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.math.absoluteValue

// 定义一个SdkDownloaderModel类，包含versionGroups、defaultItem、defaultVersion和defaultVersionVendor四个属性
class SdkDownloaderModel(
    // 版本组列表
    val versionGroups: List<SdkVersionItem>,
    // 默认的SdkItem
    val defaultItem: SdkItem,
    // 默认的SdkVersionItem
    val defaultVersion: SdkVersionItem,
    // 默认的SdkVersionVendorItem
    val defaultVersionVendor: SdkVersionVendorItem,
)

internal class SdkDownloaderMergedModel(
    // 主模型
    private val mainModel: SdkDownloaderModel,
    // WSL模型
    private val wslModel: SdkDownloaderModel? = null,
//    // WSL分发列表
    val wslDistributions: List<WSLDistribution> = listOf(),
//    // 项目WSL分发
    val projectWSLDistribution: WSLDistribution? = null
) {
    // 是否有WSL
    val hasWsl: Boolean get() = wslModel != null

    // 选择模型
    fun selectModel(wsl: Boolean): SdkDownloaderModel = when {
        wsl && wslModel != null -> wslModel
        else -> mainModel
    }
}

interface SdkDownloadEp {

    fun showDownloadUI(

        parentComponent: JComponent,

        sdkCreatedCallback: Consumer<in SdkDownloadTask>
    )


    fun supportsDownload(): Boolean
    fun pickSdk(

        parentComponent: JComponent,

        ): SdkDownloadTask? {
        val task = AtomicReference<SdkDownloadTask>()
        showDownloadUI(parentComponent, task::set)
        return task.get()
    }

    companion object {

        val EP_NAME = ExtensionPointName.create<SdkDownloadEp>("com.linqingying.cangjie.sdkDownload");
    }
}

internal interface SdkDownloaderDialogHostExtension {
//    fun allowWsl() : Boolean = true

//    fun shouldIncludeItem(sdkType: SdkTypeId, item: SdkItem) : Boolean = true
}

internal val SDK_DOWNLOADER_EXT: DataKey<SdkDownloaderDialogHostExtension> = DataKey.create("sdk-downloader-extension")
private val LOG = logger<SdkDownloader>()

interface SdkDownloaderBase {
    companion object {
        fun newDownloadTask(item: SdkItem, request: SdkInstallRequest, project: Project?): SdkDownloadTask {
            return CangJieSdkDownloadTask(item, request, project)
        }
    }
}

class SdkDownloader : SdkDownloadEp, SdkDownloaderBase {
    override fun showDownloadUI(
        parentComponent: JComponent, sdkCreatedCallback: Consumer<in SdkDownloadTask>
    ) {
        TODO("Not yet implemented")
    }

    private fun pickSdkItem(
        parentComponent: JComponent?,
        dataContext: DataContext?,
        project: Project?,
        sdkFilter: Predicate<Any>?,
        okActionText: @NlsContexts.Button String = CangJieBundle.message("dialog.button.download.sdk")
    ): Pair<SdkItem, Path>? {
//        val extension = dataContext?.getData(SDK_DOWNLOADER_EXT)
        return selectSdkAndPath(project, parentComponent, /*extension, */sdkFilter, okActionText)
    }

    override fun supportsDownload(): Boolean {
        return true
    }

    override fun pickSdk(parentComponent: JComponent): SdkDownloadTask? {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        if (project?.isDisposed == true) return null
        val (sdkItem, sdkHome) = pickSdkItem(
            parentComponent, dataContext, project, null, CangJieBundle.message("dialog.button.select.sdk")
        ) ?: return null

        return CangJieSdkDownloadTask(sdkItem, SdkInstallRequestInfo(sdkItem, sdkHome), project)

    }

    fun prepareDownloadTask(
        project: Project?,
        sdkItem: SdkItem,
        sdkHome: Path,
        sdkCreatedCallback: Consumer<in SdkDownloadTask>
    ) {
        /// prepare the SDK to be installed (e.g. create home dir, write marker file)
        val request = try {
            computeInBackground(project, CangJieUiBundle.message("progress.title.preparing.sdk")) {
                SdkInstaller.getInstance().prepareSdkInstallation(sdkItem, sdkHome)
            }
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
            LOG.warn("Failed to prepare SDK installation to $sdkHome. ${e.message}", e)
            Messages.showErrorDialog(
                project,
                CangJieUiBundle.message("error.message.text.sdk.install.failed", sdkHome),
                CangJieUiBundle.message("error.message.title.download.sdk")
            )
            return
        }

        sdkCreatedCallback.accept(SdkDownloaderBase.newDownloadTask(sdkItem, request, project))
    }


}

class SdkInstallRequestInfo(
    override val item: SdkItem, override val installDir: Path
) : SdkInstallRequest {
    override val cangjieHome: Path
        get() = item.resolveCangJieHome(installDir)
}

interface SdkInstallRequest {
    val item: SdkItem

    /**
     * The path where SDK is installed.
     * On macOS, it is likely (depending on the SDK package)
     * to contain Contents/Home folders
     */
    val installDir: Path

    /**
     * The path on the disk where the installed SDK
     * would have the bin/java and bin/javac files.
     *
     * On macOS this path may differ from the [installDir]
     * if the SDK package follows the macOS Bundle layout
     */
    val cangjieHome: Path
}
//
//interface SdkDownloadTask {
//    val suggestedSdkName: String
//
//    val productName: String?
//        get() = null
//
//    val plannedHomeDir: String
//
//    val plannedVersion: String
//
//    fun doDownload(indicator: ProgressIndicator)
//}

class CangJieSdkDownloadTask(

    val sdkItem: SdkItem,
    val request: SdkInstallRequest,
    val project: Project?
) : SdkDownloadTask {
    //    override val suggestedSdkName: String = request.item.suggestedSdkName
//    override val productName: String = sdkItem.suggestedSdkName
//    override val plannedHomeDir: String = request.cangjieHome.toString()
//    override val plannedVersion: String = request.item.version
    override fun getSuggestedSdkName() = request.item.suggestedSdkName
    override fun getPlannedHomeDir() = request.cangjieHome.toString()
    override fun getPlannedVersion() = request.item.version
    override fun getProductName(): String = request.item.suggestedSdkName


    override fun doDownload(indicator: ProgressIndicator) {
        SdkInstaller.getInstance().installSdk(request, indicator, project)
    }

    override fun toString() = "DownloadTask{${sdkItem.suggestedSdkName}, dir=${request.installDir}}"
}

internal class SdkDownloadDialog(
    val project: Project?,
    val parentComponent: Component?,
//    val sdkType: SdkTypeId,
    private val mergedModel: SdkDownloaderMergedModel,
    okActionText: @NlsContexts.Button String = CangJieBundle.message("dialog.button.download.sdk"),
    val text: @Nls String? = null
) : DialogWrapper(project, parentComponent, false, IdeModalityType.IDE) {
    private lateinit var versionComboBox: ComboBox<SdkVersionItem>


    private var installDirTextField: TextFieldWithBrowseButton? = null
    private var installDirCombo: ComboBox<String>? = null
    private lateinit var installDirComponent: JComponent


    private var currentModel: SdkDownloaderModel? = null


    private lateinit var selectedItem: SdkItem
    private lateinit var selectedPath: String


    private val panel: DialogPanel = panel {
        if (text != null) {
            row {
                label(text)
            }
        }

        var archiveSizeCell: Cell<*>? = null

        row(CangJieBundle.message("dialog.row.sdk.version")) {
            versionComboBox =
                comboBox(listOf<SdkVersionItem>().toMutableList(), textListCellRenderer { it!!.sdkVersion }).align(
                    AlignX.FILL
                ).component
        }

        row(CangJieBundle.message("dialog.row.sdk.location")) {
            cell(setupContainer()).align(AlignX.FILL).apply { archiveSizeCell = comment("") }
        }
    }


    init {
        title = CangJieBundle.message("dialog.title.download.sdk")
        isResizable = false

        versionComboBox.onSelectionChange(::onVersionSelectionChange)

        setOKButtonText(okActionText)

        setModel(false/*mergedModel.projectWSLDistribution != null*/)
        init()
    }

    private fun setupContainer(): JComponent {
        if (mergedModel.hasWsl) {
            installDirCombo = ComboBox<String>().apply {
                isEditable = true
                initBrowsableEditor(
                    BrowseFolderRunnable(
                        project,
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withTitle(CangJieBundle.message("dialog.title.select.path.to.install.sdk")),
                        installDirCombo,
                        TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT
                    ), disposable
                )
                addActionListener { onTargetPathChanged(editor.item as String) }
                installDirComponent = this
            }
            installDirTextField = null
        } else {
            installDirTextField = textFieldWithBrowseButton(
                project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle(CangJieBundle.message("dialog.title.select.path.to.install.sdk"))
            ).apply {
                onTextChange { onTargetPathChanged(it) }
                textField.columns = 36
                installDirComponent = this
            }
            installDirCombo = null
        }
        return installDirComponent
    }

    private fun setModel(forWsl: Boolean) {
        val model = mergedModel.selectModel(forWsl)
        if (currentModel === model) return

        val prevSelectedVersion = versionComboBox.selectedItem as? SdkVersionItem

        currentModel = model
        versionComboBox.model = DefaultComboBoxModel(model.versionGroups.toTypedArray())

        val newVersionItem = if (prevSelectedVersion != null) {
            model.versionGroups.singleOrNull { it.sdkVersion == prevSelectedVersion.sdkVersion }
        } else null


        onVersionSelectionChange(newVersionItem ?: model.defaultVersion)

    }


    private fun onTargetPathChanged(path: String) {
        @Suppress("NAME_SHADOWING") val path = FileUtil.expandUserHome(path)
        selectedPath = path


        setModel(WslPath.isWslUncPath(path))
    }

    private fun getSuggestedInstallDirs(newVersion: SdkItem): List<String> {
        return (listOf(null) + mergedModel.wslDistributions).mapTo(LinkedHashSet()) {
            SdkInstaller.getInstance().defaultInstallDir(newVersion, it).toString()
        }.map {
            FileUtil.getLocationRelativeToUserHome(it)
        }
    }

    private fun onVersionSelectionChange(it: SdkVersionItem?) {
        if (it == null) return
        versionComboBox.selectedItem = it
        val path =
            SdkInstaller.getInstance().defaultInstallDir(it.item/* mergedModel.projectWSLDistribution*/).toString()
        val relativePath = FileUtil.getLocationRelativeToUserHome(path)
        if (installDirTextField != null) {
            installDirTextField!!.text = relativePath
        } else {
            installDirCombo!!.model = CollectionComboBoxModel(getSuggestedInstallDirs(it.item), relativePath)
        }
        selectedPath = path
        selectedItem = it.item

    }

    override fun doValidate(): ValidationInfo? {
        super.doValidate()?.let { return it }

        val (_, error) = SdkInstaller.getInstance().validateInstallDir(selectedPath)
        return error?.let { ValidationInfo(error, installDirComponent) }
    }

    override fun createCenterPanel() = panel

    fun selectSdkAndPath(): Pair<SdkItem, Path>? {
        if (!showAndGet()) {
            return null
        }

        val (selectedFile) = SdkInstaller.getInstance().validateInstallDir(selectedPath)
        if (selectedFile == null) {
            return null
        }

        return selectedItem to selectedFile
    }

    private inline fun TextFieldWithBrowseButton.onTextChange(crossinline action: (String) -> Unit) {
        textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                action(text)
            }
        })
    }

    private inline fun <reified T> ComboBox<T>.onSelectionChange(crossinline action: (T) -> Unit) {
        this.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) action(e.item as T)
        }
    }

}

// 定义一个SdkDownloadDialog类，包含versionGroups、defaultItem、defaultVersion和defaultVersionVendor四个属性
class SdkDownloadDialogData(
    // versionGroups：SdkVersionItem类型的列表
    val versionGroups: List<SdkVersionItem>,
    // defaultItem：SdkItem类型的属性
    val defaultItem: SdkItem,
    // defaultVersion：SdkVersionItem类型的属性
    val defaultVersion: SdkVersionItem,
    // defaultVersionVendor：SdkVersionVendorItem类型的属性
    val defaultVersionVendor: SdkVersionVendorItem,
)

// 定义一个SdkVersionVendorItem类，包含一个SdkItem类型的item属性
class SdkVersionVendorItem(
    val item: SdkItem
) {
    // 定义一个parent属性，类型为SdkVersionItem，默认值为null
    var parent: SdkVersionItem? = null

    // 定义一个selectItem属性，类型为SdkVersionVendorItem，返回parent的includedItems中item属性等于item的元素，如果不存在则返回this
    val selectItem: SdkVersionVendorItem get() = parent?.includedItems?.find { it.item == item } ?: this

    // 定义一个canBeSelected属性，类型为Boolean，返回parent是否为null
    val canBeSelected: Boolean get() = parent == null
}

//定义一个SdkVersionItem类，用于表示SDK版本
class SdkVersionItem(
    //SDK版本
    @NlsSafe val sdkVersion: String,

    //默认选中的项
    private val defaultSelectedItem: SdkVersionVendorItem,
    //包含的项
    val includedItems: List<SdkVersionVendorItem>,
    //排除的项
    val excludedItems: List<SdkVersionVendorItem>
) {
    //we reuse model to keep selected element in-memory!
    //重用模型以保持内存中选定的元素
    val model: ComboBoxModel<SdkVersionVendorItem> by lazy {
        //要求包含的项不为空
        require(this.includedItems.isNotEmpty()) { "No included items for $sdkVersion" }
        //要求默认选中的项在包含的项中
        require(this.defaultSelectedItem in this.includedItems) { "Default selected item must be in the list of items for $sdkVersion" }

        //根据排除的项是否为空，确定所有项
        val allItems = when {
            this.excludedItems.isNotEmpty() -> this.includedItems + this.excludedItems
            else -> this.includedItems
        }

        //将所有项转换为数组，并设置默认选中的项
        DefaultComboBoxModel(allItems.toTypedArray()).also {
            it.selectedItem = defaultSelectedItem
        }
    }

    val item get() = defaultSelectedItem.item
}

interface WSLDistributionForSdkInstaller {
    fun getWslPath(path: Path): String
    fun executeOnWsl(command: List<String>, dir: String, timeout: Int): ProcessOutput
}

private data class PendingSdkRequest(
    override val item: SdkItem,
    override val installDir: Path,
    override val cangjieHome: Path
) : SdkInstallRequest {
    private val isRunning = AtomicBoolean(false)
    private val future = CompletableFuture<Unit>()


    @Volatile
    private var progressIndicator: ProgressIndicator? = null

    fun tryStartInstallOrWait(indicator: ProgressIndicator?, installAction: () -> Unit) {
        if (isRunning.compareAndSet(false, true)) {
            doRealDownload(indicator, installAction)
        } else {
            waitForDownload(indicator)
        }
    }

    private fun doRealDownload(indicator: ProgressIndicator?, installAction: () -> Unit) {
        progressIndicator = indicator
        try {
            installAction()
            future.complete(Unit)
        } catch (t: Throwable) {
            future.completeExceptionally(t)
            throw t
        } finally {
            progressIndicator = null
            isRunning.set(false)
        }
    }

    private fun waitForDownload(indicator: ProgressIndicator?) {
        wrapProgressIfNeeded(indicator) {
            while (true) {
                indicator?.checkCanceled()
                try {
                    future.get(100, TimeUnit.MILLISECONDS)
                    return@wrapProgressIfNeeded
                } catch (t: TimeoutException) {
                    continue
                } catch (e: InterruptedException) {
                    throw ProcessCanceledException()
                } catch (e: CancellationException) {
                    throw ProcessCanceledException()
                } catch (e: ExecutionException) {
                    throw e.cause ?: e
                }
            }
        }
    }

    private fun wrapProgressIfNeeded(indicator: ProgressIndicator?, action: () -> Unit) {
        val parentProgress = progressIndicator as? ProgressIndicatorBase
        if (indicator == null || parentProgress == null) {
            return action()
        }

        val delegate = RelayUiToDelegateIndicator(indicator)
        parentProgress.addStateDelegate(delegate)
        try {
            return action()
        } finally {
            parentProgress.removeStateDelegate(delegate)
        }
    }

    override fun toString(): String {
        return "PendingSdkRequest(item=$item, installDir=$installDir)"
    }
}

abstract class SdkInstallerBase {
    protected val LOG: Logger = Logger.getInstance(javaClass)
    abstract fun defaultInstallDir(): Path
    private val myLock = ReentrantLock()
    private val myPendingDownloads = HashMap<SdkItem, PendingSdkRequest>()
    open fun defaultInstallDir(wslDistribution: WSLDistributionForSdkInstaller?): Path = defaultInstallDir()

    fun validateInstallDir(selectedPath: String): Pair<Path?, @Nls String?> {
        if (selectedPath.isBlank()) {
            return null to CangJieBundle.message("dialog.message.error.target.path.empty")
        }

        val targetDir = runCatching { Paths.get(FileUtil.expandUserHome(selectedPath)) }.getOrElse { t ->
            LOG.warn("Failed to resolve user path: $selectedPath. ${t.message}", t)
            return null to CangJieBundle.message("dialog.message.error.resolving.path")
        }

        if (Files.isRegularFile(targetDir)) {
            return null to CangJieBundle.message("dialog.message.error.target.path.exists.file")
        }
        if (Files.isDirectory(targetDir) && targetDir.toFile().listFiles()?.isNotEmpty() == true) {
            return null to CangJieBundle.message("dialog.message.error.target.path.exists.nonEmpty.dir")
        }

        return targetDir to null
    }

    fun installSdk(sdkInstallRequest: SdkInstallRequest, indicator: ProgressIndicator?, project: Project?) {
        var request = sdkInstallRequest

        if (request is SdkInstallRequestInfo) {
            // Request was created without side effects
            request = prepareSdkInstallation(request.item, request.installDir)
        }

        if (request is LocallyFoundSdk) {
            return
        }

        if (request is PendingSdkRequest) {
            request.tryStartInstallOrWait(indicator) {
                installSdkImpl(request, indicator, project)
            }
            return
        }

        LOG.error("Unexpected SdkInstallRequest: $request of type ${request.javaClass.name}")
        installSdkImpl(request, indicator, project)
    }

    protected open fun wslDistributionFromPath(targetDir: Path): WSLDistributionForSdkInstaller? = null
    private fun prepareSdkInstallationImpl(sdkItem: SdkItem, targetPath: Path): PendingSdkRequest {
        val (home, error) = validateInstallDir(targetPath.toString())
        if (home == null || error != null) {
            throw RuntimeException(error ?: "Invalid Target Directory")
        }

        val cangjieHome = sdkItem.resolveCangJieHome(targetPath)
        Files.createDirectories(cangjieHome)

        val request = PendingSdkRequest(sdkItem, targetPath, cangjieHome)
        writeMarkerFile(request)
        return request
    }

    fun prepareSdkInstallation(sdkItem: SdkItem, targetPath: Path): SdkInstallRequest {
        if (Registry.`is`("sdk.downloader.reuse.installed")) {
            val distribution = wslDistributionFromPath(targetPath)
            val existingRequest = findAlreadyInstalledSdk(sdkItem, distribution)
            if (existingRequest != null) return existingRequest
        }

        if (Registry.`is`("sdk.downloader.reuse.downloading")) {
            return myLock.withLock {
                myPendingDownloads.computeIfAbsent(sdkItem) { prepareSdkInstallationImpl(sdkItem, targetPath) }
            }
        } else {
            return prepareSdkInstallationDirect(sdkItem, targetPath)
        }
    }

    /**
     * This method does not check any existing or pending SDKs locally,
     * use [prepareSdkInstallation()] to avoid extra work if possible
     *
     * @see prepareSdkInstallation
     */
    fun prepareSdkInstallationDirect(sdkItem: SdkItem, targetPath: Path): SdkInstallRequest =
        prepareSdkInstallationImpl(sdkItem, targetPath)

    private fun findSdkItemForInstalledSdk(sdkPath: Path?): SdkItem? {
        try {
            if (sdkPath == null) return null
            if (!sdkPath.isDirectory()) return null
            val predicate = when {
                WslPath.isWslUncPath(sdkPath.toString()) -> SdkPredicate.forWSL()
                else -> SdkPredicate.default()
            }

            val markerFile = generateSequence(sdkPath) { file -> file.parent }
                .takeWhile {
                    arrayOf<LinkOption>()
                    it.isDirectory()
                }
                .take(5)
                .mapNotNull { markerFile(it) }
                .firstOrNull { it.isRegularFile() } ?: return null

            val json = SdkListParser.readTree(Files.readString(markerFile))
            return SdkListParser.parseSdkItem(json, predicate).firstOrNull()
        } catch (e: Throwable) {
            return null
        }
    }

    protected open fun findHistoryRoots(feedItem: SdkItem): List<Path> = listOf()
    fun findSdkItemForInstalledSdk(jdkHome: String?): SdkItem? {
        try {
            if (jdkHome == null) return null
            val jdkPath = Paths.get(jdkHome)
            return findSdkItemForInstalledSdk(jdkPath)
        } catch (t: Throwable) {
            return null
        }
    }

    private fun findAlreadyInstalledSdk(
        feedItem: SdkItem,
        distribution: WSLDistributionForSdkInstaller?
    ): SdkInstallRequest? {
        try {
            val localRoots = run {
                val defaultInstallDir = defaultInstallDir(distribution)
                arrayOf<LinkOption>()
                if (!defaultInstallDir.isDirectory()) return@run listOf()
                Files.list(defaultInstallDir).use { it.toList() }
            }

            val historyRoots = findHistoryRoots(feedItem)
            for (installDir in localRoots + historyRoots) {
                arrayOf<LinkOption>()
                if (!installDir.isDirectory()) continue

                val item = findSdkItemForInstalledSdk(installDir) ?: continue
                if (item != feedItem) continue

                val sdkHome = item.resolveCangJieHome(installDir)
                if (run {
                        arrayOf<LinkOption>()
                        sdkHome.isDirectory()
                    } && CjToolchainBase.checkForSdk(sdkHome) && wslDistributionFromPath(sdkHome) == distribution) {
                    return LocallyFoundSdk(feedItem, installDir, sdkHome)
                }
            }
        } catch (t: Throwable) {
            return null
        }

        return null
    }

    /**
     * @see [SdkInstallRequest.javaHome] for the actual java home, it may not match the [SdkInstallRequest.installDir]
     */
    protected open fun installSdkImpl(request: SdkInstallRequest, indicator: ProgressIndicator?, project: Project?) {
        val item = request.item
        indicator?.text = CangJieUiBundle.message("progress.text.installing.sdk.1", item.suggestedSdkName)
        val urlString =
            item.url ?: "https://gitee.com/Lin_Qing_Ying/intellij-cangjie-stdlib/raw/master/cangjie-0.53.13.zip"
        val targetDir = request.installDir
        val url = Urls.parse(urlString, false) ?: error("Cannot parse download URL: ${urlString}")

        var logFailed = false
        if (!url.scheme.equals("https", ignoreCase = true)) {
            logFailed = true
            error("URL must use https:// protocol, but was: $url")
        }
        val wslDistribution = wslDistributionFromPath(targetDir)
        if (wslDistribution != null && item.os != "linux") {
            logFailed = true
            error("Cannot install non-linux SDK into WSL environment to $targetDir from $item")
        }
        indicator?.text2 = CangJieUiBundle.message("progress.text2.downloading.sdk")
        val downloadFile = Paths.get(
            PathManager.getTempPath(),
            FileUtil.sanitizeFileName("sdk-${System.nanoTime()}-${item.archiveFileName}")
        )

        try {
            try {
                HttpRequests.request(urlString)
                    .productNameAsUserAgent()
                    .saveToFile(downloadFile.toFile(), indicator)

                if (!downloadFile.isRegularFile()) {
                    logFailed = true
                    throw RuntimeException("Downloaded file does not exist: $downloadFile")
                }
            } catch (t: Throwable) {
                if (t is ControlFlowException) throw t
                logFailed = true
                throw RuntimeException("Failed to download ${item.suggestedSdkName} from $url. ${t.message}", t)
            }

            val sizeDiff = runCatching { Files.size(downloadFile) }.getOrNull()
            if (sizeDiff == null || sizeDiff <= 0L) {

                logFailed = true
                throw RuntimeException(
                    "The downloaded ${item.suggestedSdkName} has incorrect file size,\n" +
                            "the difference is ${sizeDiff?.absoluteValue ?: "unknown"} bytes.\n" +
                            "Check your internet connection and try again later"
                )
            }

            val actualHashCode = runCatching {
                com.google.common.io.Files.asByteSource(downloadFile.toFile()).hash(Hashing.sha256()).toString()
            }.getOrNull()
            if (!actualHashCode.equals(item.sha256, ignoreCase = true)) {
                throw RuntimeException(
                    "Failed to verify SHA-256 checksum for ${item.suggestedSdkName}\n\n" +
                            "The actual value is ${actualHashCode ?: "unknown"},\n" +
                            "but expected ${item.sha256} was expected\n" +
                            "Check your internet connection and try again later"
                )
            }

            indicator?.isIndeterminate = true
            indicator?.text2 = CangJieUiBundle.message("progress.text2.unpacking.sdk")

            try {
//                  if (wslDistribution != null) {
//                      unpackSdkOnWsl(wslDistribution, item.packageType, downloadFile, targetDir, item.packageRootPrefix)
//                  } else {
                item.packageType.openDecompressor(downloadFile)
                    .entryFilter { indicator?.checkCanceled(); true }
//                    .let {
//                        val fullMatchPath = item.suggestedSdkName.trim('/')
//                        if (fullMatchPath.isBlank()) it else it.removePrefixPath(fullMatchPath)
//                    }
                    .extract(targetDir)
//                }
//                如果存在cangjie， 将cangjie中的文件剪切到targetDir中
                moveCangjieContents(targetDir)

                runCatching { writeMarkerFile(request) }
            } catch (t: Throwable) {
                if (t is ControlFlowException) throw t
                logFailed = true
                throw RuntimeException("Failed to extract ${item.suggestedSdkName}. ${t.message}", t)
            }
        } catch (t: Throwable) {
            targetDir.delete()
            markerFile(targetDir)?.delete()
            throw t
        } finally {
            runCatching { FileUtil.delete(downloadFile) }
        }

    }

    fun moveCangjieContents(targetDir: Path) {
        val cangjieDir = targetDir.resolve("cangjie")
        if (cangjieDir.isDirectory()) {
            Files.walk(cangjieDir).use { paths ->
                paths.filter { it.isRegularFile() }.forEach { file ->
                    val relativePath = cangjieDir.relativize(file)
                    val targetPath = targetDir.resolve(relativePath)
                    targetPath.parent.createDirectories()
                    file.toFile().copyTo(targetPath.toFile(), overwrite = true) // 修复点
                }
            }
            cangjieDir.toFile().deleteRecursively()
        }
    }

    private fun markerFile(installDir: Path): Path? =
        installDir.parent?.resolve(".${installDir.fileName}.intellij_cangjie")

    // 根据传入的SdkInstallRequest对象，写入标记文件
    private fun writeMarkerFile(request: SdkInstallRequest) {
        // 获取安装目录
        val installDir = request.installDir
        // 获取标记文件
        val markerFile = markerFile(installDir) ?: return
        try {
            // 写入标记文件
            request.item.writeMarkerFile(markerFile)
        } catch (t: Throwable) {
            // 如果是ControlFlowException，则抛出
            if (t is ControlFlowException) throw t
            // 否则，记录警告日志
            LOG.warn("Failed to write marker file to $markerFile. ${t.message}", t)
        }
    }
}

@Service

class SdkInstaller : SdkInstallerBase() {
    companion object {
        @JvmStatic
        fun getInstance(): SdkInstaller = service<SdkInstaller>()
    }

    override fun defaultInstallDir(): Path {
        val explicitHome = System.getProperty("cangjie.sdk.downloader.home")
        if (explicitHome != null) {
            return Paths.get(explicitHome)
        }

        val home = Paths.get(FileUtil.toCanonicalPath(System.getProperty("user.home") ?: "."))
        return when {
            SystemInfo.isLinux -> home.resolve(".cangjie").resolve("sdks")
            //see https://youtrack.jetbrains.com/issue/IDEA-206163#focus=streamItem-27-3270022.0-0
            SystemInfo.isMac -> home.resolve("Library/CangJie/Sdk")
            SystemInfo.isWindows -> home.resolve(".cangjie").resolve("sdks")

            else -> error("Unsupported OS: ${SystemInfo.getOsNameAndVersion()}")
        }
    }

    protected fun defaultInstallDir(installDir: Path, newVersion: SdkItem): Path {
        val targetDir = installDir.resolve(newVersion.suggestedSdkName)
        var count = 1
        var uniqueDir = targetDir
        while (uniqueDir.exists()) {
            uniqueDir = targetDir.parent.resolve("${targetDir.fileName}-${count++}")
        }
        return uniqueDir.toAbsolutePath()
    }

    private fun defaultInstallDir(wslDistribution: WSLDistribution?): Path {
        wslDistribution?.let { dist ->
            dist.userHome?.let { home ->
                return Paths.get(dist.getWindowsPath("$home/.cangjie/sdks"))
            }
        }

        return defaultInstallDir()
    }

    fun defaultInstallDir(newVersion: SdkItem, wslDistribution: WSLDistribution? = null): Path {
        return defaultInstallDir(defaultInstallDir(wslDistribution), newVersion)
    }
}

private inline fun <T : Any?> computeInBackground(
    project: Project?, @NlsContexts.DialogTitle title: String, crossinline action: (ProgressIndicator) -> T
): T = ProgressManager.getInstance().run(object : Task.WithResult<T, Exception>(project, title, true) {
    override fun compute(indicator: ProgressIndicator) = action(indicator)
})

private fun selectSdkAndPath(
    project: Project?,

    parentComponent: JComponent?,
//    extension: SdkDownloaderDialogHostExtension?,
    sdkFilter: Predicate<Any>?,
    okActionText: @NlsContexts.Button String,
): Pair<SdkItem, Path>? {
    val items = try {
        val sdkFilter: Predicate<Any>? = null
        computeInBackground(null, CangJieBundle.message("progress.title.downloading.sdk.list")) {
            val buildModel = {
                SdkListDownloader.getInstance().downloadForUI(progress = it)
//                        .filter { extension.shouldIncludeItem(sdkTypeId, it) }
                    .takeIf { it.isNotEmpty() }?.let { buildSdkDownloaderModel(it, { sdkFilter?.test(it) != false }) }
            }
            val mainModel = buildModel() ?: return@computeInBackground null

            SdkDownloaderMergedModel(mainModel, null)
        }
    } catch (e: Throwable) {
        if (e is ControlFlowException) throw e

        null
    }
    if (items == null) {
        Messages.showErrorDialog(
            project,
            CangJieBundle.message("error.message.no.sdk.for.download"),
            CangJieBundle.message("error.message.title.download.sdk")
        )
        return null
    }
    return SdkDownloadDialog(project, parentComponent, items, okActionText).selectSdkAndPath()

}

fun addDownloadItem(
    extension: SdkDownloadEp,
    pathToToolchainComboBox: CjToolchainPathChoosingComboBox,
    update: (Any?) -> Unit = {}
) {
    val task = extension.pickSdk(pathToToolchainComboBox) as? CangJieSdkDownloadTask ?: return

    val dataContext = DataManager.getInstance().getDataContext(pathToToolchainComboBox)
    val project = CommonDataKeys.PROJECT.getData(dataContext)

    val sdkDownloader = (SdkDownloadEp.EP_NAME.findFirstSafe { it is SdkDownloader } as? SdkDownloader)
        ?: return


    ProgressManager.getInstance().run(
        object : AbstractForegroundTask<Unit>(
            project,
            CangJieUiBundle.message("progress.text2.downloading.sdk", task.suggestedSdkName)
        ) {
            override fun execute(indicator: ProgressIndicator) {
                val (selectedFile, error) = SdkInstaller.getInstance()
                    .validateInstallDir(task.request.installDir.toString())
                if (selectedFile != null) {
                    sdkDownloader.prepareDownloadTask(project, task.sdkItem, selectedFile) { downloadTask ->
//                        scheduleSetupInstallableSdk(project, downloadTask, sdkDownloadedFuture, setSdk)

                        downloadTask.doDownload(indicator)
//                        pathToToolchainComboBox.addSingleToolchainAndSelect( "C:\\Users\\27439\\.cangjie\\sdks\\cangjie-0.53.13-1")

                        pathToToolchainComboBox.addSingleToolchainAndSelect(downloadTask.getPlannedHomeDir())
                    }
                } else {
                    Messages.showErrorDialog(
                        project,
                        CangJieUiBundle.message("sdk.download.error.message", error),
                        CangJieUiBundle.message("sdk.download.error.title")
                    )
                }
            }

            override fun handleSuccess(result: Unit) {

            }

        }


    )


}


private data class LocallyFoundSdk(
    override val item: SdkItem,
    override val installDir: Path,
    override val cangjieHome: Path
) : SdkInstallRequest {

    override fun toString(): String {
        return "LocallyFoundSdk(item=$item, installDir=$installDir)"
    }
}
