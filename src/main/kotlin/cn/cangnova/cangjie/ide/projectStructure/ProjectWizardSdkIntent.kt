package cn.cangnova.cangjie.ide.projectStructure//package cn.cangnova.cangjie.ide.projectStructure
//
//import com.intellij.icons.AllIcons
//import com.intellij.openapi.Disposable
//import com.intellij.openapi.application.EDT
//import com.intellij.openapi.application.ModalityState
//import com.intellij.openapi.application.asContextElement
//import com.intellij.openapi.components.Service
//import com.intellij.openapi.components.service
//import com.intellij.openapi.observable.util.whenDisposed
//import com.intellij.openapi.progress.ProgressIndicator
//import com.intellij.openapi.progress.ProgressManager
//import com.intellij.openapi.progress.blockingContext
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.projectRoots.JavaSdkType
//
//import com.intellij.openapi.projectRoots.Sdk
//import com.intellij.openapi.projectRoots.SdkType
//import com.intellij.openapi.projectRoots.impl.DependentSdkType
//import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
//import com.intellij.openapi.roots.impl.SdkFinder
//
//import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownload
//import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
//import com.intellij.openapi.ui.ComboBox
//import com.intellij.openapi.ui.popup.ListSeparator
//import com.intellij.openapi.util.NlsSafe
//import com.intellij.openapi.util.io.FileUtil
//import com.intellij.platform.util.coroutines.childScope
//import com.intellij.ui.AnimatedIcon
//import com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED
//import com.intellij.ui.CellRendererPanel
//import com.intellij.ui.ClientProperty
//import com.intellij.ui.GroupedComboBoxRenderer
//import com.intellij.ui.SimpleColoredComponent
//import com.intellij.ui.SimpleTextAttributes
//import com.intellij.ui.components.JBLabel
//import com.intellij.util.application
//import com.intellij.util.system.CpuArch
//import com.intellij.util.ui.EmptyIcon
//import cn.cangnova.cangjie.cjpm.toolchain.CjLocalToolchain
//import cn.cangnova.cangjie.cjpm.toolchain.CjToolchainBase
//import cn.cangnova.cangjie.messages.CangJieUiBundle
//import cn.cangnova.cangjie.ide.projectStructure.ProjectWizardSdkIntent.AddSdkFromPath
//import cn.cangnova.cangjie.ide.projectStructure.ProjectWizardSdkIntent.AddSdkFromSdkListDownloader
//import cn.cangnova.cangjie.ide.projectStructure.ProjectWizardSdkIntent.DetectedSdk
//import cn.cangnova.cangjie.ide.projectStructure.ProjectWizardSdkIntent.DownloadSdk
//import cn.cangnova.cangjie.ide.projectStructure.ProjectWizardSdkIntent.ExistingSdk
//import cn.cangnova.cangjie.ide.projectStructure.ProjectWizardSdkIntent.NoSdk
//import cn.cangnova.cangjie.ide.projectStructure.download.SdkDownloadEp
//import cn.cangnova.cangjie.ide.projectStructure.download.SdkInstaller
//import cn.cangnova.cangjie.ide.projectStructure.download.SdkListDownloader
//import cn.cangnova.cangjie.utils.AbstractForegroundTask
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.awt.BorderLayout
//import java.awt.Component
//import java.util.Vector
//import javax.accessibility.AccessibleContext
//import javax.swing.DefaultComboBoxModel
//import javax.swing.Icon
//import javax.swing.JList
//import kotlin.collections.firstOrNull
//import kotlin.collections.removeAll
//
//sealed class ProjectWizardSdkIntent {
//
//    data object NoSdk : ProjectWizardSdkIntent()
//
//    data class DownloadSdk(val task: SdkDownloadTask) : ProjectWizardSdkIntent()
//
//    data class ExistingSdk(val sdk: Sdk) : ProjectWizardSdkIntent()
//
//    data object AddSdkFromPath : ProjectWizardSdkIntent()
//
//    data class AddSdkFromSdkListDownloader(val extension: SdkDownloadEp) : ProjectWizardSdkIntent()
//
//    data class DetectedSdk(val version: @NlsSafe String, val home: @NlsSafe String) : ProjectWizardSdkIntent()
//
//}
//
//fun addDownloadItem(extension: SdkDownloadEp, combo: ComboBox<ProjectWizardSdkIntent>) {
//    val task = extension.pickSdk(combo) ?: return
//
//
////    val index = (0..combo.itemCount).firstOrNull {
////        val item = combo.getItemAt(it)
////        item !is NoSdk && item !is DownloadSdk
////    } ?: 0
//    TODO()
//}
//
//
//@Service(Service.Level.APP)
//internal class ProjectWizardSdkComboBoxService(
//    private val coroutineScope: CoroutineScope
//) {
//    fun childScope(name: String): CoroutineScope = coroutineScope.childScope(name)
//}
//
//class ProjectWizardSdkComboBox(
//    val projectSdk: Sdk? = null,
//    disposable: Disposable
//) : ComboBox<ProjectWizardSdkIntent>() {
//    val registered: MutableList<ExistingSdk> = mutableListOf()
//    val detectedSDKs: MutableList<DetectedSdk> = mutableListOf()
//    var isLoadingDownloadItem: Boolean = false
//    var isLoadingExistingSdks: Boolean = true
//    val progressIcon: JBLabel = JBLabel(AnimatedIcon.Default.INSTANCE)
//    val coroutineScope = application.service<ProjectWizardSdkComboBoxService>().childScope("ProjectWizardSdkComboBox")
//
//    init {
//        model = DefaultComboBoxModel(Vector(initialItems()))
//
//        disposable.whenDisposed { coroutineScope.cancel() }
//
//        if (registered.isEmpty()) {
//            isLoadingDownloadItem = true
//            coroutineScope.launch {
////                addDownloadOpenSdkIntent()
//            }
//        }
//
//        coroutineScope.launch {
////            addExistingSdks()
//        }
//
//        isSwingPopup = false
//        ClientProperty.put(this, ANIMATION_IN_RENDERER_ALLOWED, true)
//        renderer = object : GroupedComboBoxRenderer<ProjectWizardSdkIntent>(this) {
//            override fun separatorFor(value: ProjectWizardSdkIntent): ListSeparator? {
//                return when (value) {
//                    registered.firstOrNull() -> ListSeparator(CangJieUiBundle.message("sdk.registered.items"))
//                    is AddSdkFromSdkListDownloader -> ListSeparator("")
//                    detectedSDKs.firstOrNull() -> ListSeparator(CangJieUiBundle.message("sdk.detected.items"))
//                    else -> null
//                }
//            }
//
//            override fun customize(
//                item: SimpleColoredComponent,
//                value: ProjectWizardSdkIntent,
//                index: Int,
//                isSelected: Boolean,
//                cellHasFocus: Boolean
//            ) {
//                item.icon = when {
//                    value is NoSdk && index == -1 -> null
//                    else -> getIcon(value)
//                }
//
//                when (value) {
//                    is NoSdk -> item.append(
//                        CangJieUiBundle.message("sdk.missing.item"),
//                        SimpleTextAttributes.ERROR_ATTRIBUTES
//                    )
//
//                    is ExistingSdk -> {
//                        if (value.sdk == projectSdk) {
//                            item.append(CangJieUiBundle.message("sdk.project.item"))
//                            item.append(" ${projectSdk.name}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
//                        } else {
//                            item.append(value.sdk.name)
//                            val version = value.sdk.versionString ?: (value.sdk.sdkType as SdkType).presentableName
//                            item.append(" $version", SimpleTextAttributes.GRAYED_ATTRIBUTES)
//                        }
//                    }
//
//                    is DownloadSdk -> {
//                        when (value.task.productName) {
//                            null -> {
//                                item.append(
//                                    CangJieUiBundle.message(
//                                        "sdk.download.predefined.item",
//                                        value.task.suggestedSdkName
//                                    )
//                                )
//                                item.append(" ${value.task.plannedVersion}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
//                            }
//
//                            else -> {
//                                item.append(
//                                    CangJieUiBundle.message(
//                                        "sdk.download.predefined.item",
//                                        value.task.productName
//                                    )
//                                )
//                            }
//                        }
//                    }
//
//                    is AddSdkFromSdkListDownloader -> {
//                        1
//                        item.append(CangJieUiBundle.message("sdk.download.item"))
//                    }
//
//                    is AddSdkFromPath -> item.append(CangJieUiBundle.message("action.AddSdkAction.text"))
//
//                    is DetectedSdk -> {
//                        item.append(value.version)
//                        item.append(" ${value.home}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
//                    }
//                }
//            }
//
//            override fun getIcon(item: ProjectWizardSdkIntent): Icon? {
//                return when (item) {
//                    is DownloadSdk, is AddSdkFromSdkListDownloader -> AllIcons.Actions.Download
////                    is AddSdkFromPath -> AllIcons.Nodes.PpSdk
////                    is ExistingSdk, is DetectedSdk -> JavaSdk.getInstance().icon
//                    else -> EmptyIcon.ICON_16
//                }
//            }
//
//            override fun getListCellRendererComponent(
//                list: JList<out ProjectWizardSdkIntent>?,
//                value: ProjectWizardSdkIntent,
//                index: Int,
//                isSelected: Boolean,
//                cellHasFocus: Boolean
//            ): Component {
//                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
//                if (index == -1 && (isLoadingExistingSdks || isLoadingDownloadItem) && selectedItem !is DownloadSdk) {
//                    val panel = object : CellRendererPanel(BorderLayout()) {
//                        override fun getAccessibleContext(): AccessibleContext = component.accessibleContext
//                    }
//                    component.background = null
//                    panel.add(component, BorderLayout.CENTER)
//                    panel.add(progressIcon, BorderLayout.EAST)
//                    return panel
//                } else {
//                    return component
//                }
//            }
//        }
//    }
//
//    private fun initialItems(): MutableList<ProjectWizardSdkIntent> {
//        val items = mutableListOf<ProjectWizardSdkIntent>()
//
////        // Add SDKs from the ProjectSdkTable
////        registered.addAll(
////            ProjectSdkTable.getInstance().allSdks
////                .filter { jdk ->
////                    jdk.sdkType is JavaSdkType && jdk.sdkType !is DependentSdkType
////                }
////                .map { ExistingSdk(it) }
////        )
//
//        if (registered.isNotEmpty()) {
//            items.addAll(registered)
//        } else {
//            items.add(NoSdk)
//        }
//
//        // Add options to download or select a SDK
//        SdkDownloadEp.EP_NAME.findFirstSafe { it.supportsDownload() }?.let {
//            items.add(AddSdkFromSdkListDownloader(it))
//        }
//        items.add(AddSdkFromPath)
//
//        return items
//    }
//
////    private suspend fun addDownloadOpenSdkIntent() {
////        val item = SdkListDownloader.getInstance()
////            .downloadModelForSdkInstaller(null)
////            .filter { it.matchesVendor("openjdk") }
////            .filter { CpuArch.fromString(it.arch) == CpuArch.CURRENT }
////            .maxByOrNull { it.jdkMajorVersion }
////
////        if (item == null) {
////            withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
////                isLoadingDownloadItem = false
////            }
////            return
////        }
////
////        val jdkInstaller = SdkInstaller.getInstance()
////        val request = SdkInstallRequestInfo(item, jdkInstaller.defaultInstallDir(item))
////        val task = SdkDownloaderBase.newDownloadTask(item, request, null)
////
////        withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
////            insertItemAt(DownloadSdk(task), lastRegisteredSdkIndex)
////            if (selectedItem is NoSdk) selectedIndex = 1
////            isLoadingDownloadItem = false
////        }
////    }
//
////    private suspend fun addExistingSdks() {
////        val javaSdk = JavaSdk.getInstance()
////
////        val detected = blockingContext {
////            SdkFinder.getInstance().suggestHomePaths().mapNotNull { homePath: String ->
////                val version = javaSdk.getVersionString(homePath)
////                when {
////                    version != null && javaSdk.isValidSdkHome(homePath) -> DetectedSdk(version, homePath)
////                    else -> null
////                }
////            }
////        }
////
////        withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
////            detected
////                .filter { d -> registered.none { r -> FileUtil.pathsEqual(d.home, r.sdk.homePath) } }
////                .forEach {
////                    detectedSDKs.add(it)
////                    addItem(it)
////                }
////            if ((selectedItem is NoSdk || selectedItem is DownloadSdk) && detected.any()) {
////                val regex = "(\\d+)".toRegex()
////                detected
////                    .maxBy { regex.find(it.version)?.value?.toInt() ?: 0 }
////                    .let { selectedItem = it }
////            }
////            isLoadingExistingSdks = false
////        }
////    }
//
//    override fun setSelectedItem(anObject: Any?) {
//        val toSelect = when (anObject) {
//            is String -> {
//                registered.firstOrNull { it.sdk.name == anObject } ?: selectedItem
//            }
//
//            is AddSdkFromSdkListDownloader -> {
//                addDownloadItem(anObject.extension, this)
//                selectedItem
//            }
//
//            is AddSdkFromPath -> {
//                selectAndAddSdk(this)
//                selectedItem
//            }
//
//            else -> anObject
//        }
//        super.setSelectedItem(toSelect)
//    }
//
//    fun filterItems(sdkFilter: (Sdk) -> Boolean) {
//        registered.removeAll { !sdkFilter(it.sdk) }
//        for (i in itemCount - 1 downTo 0) {
//            val item = getItemAt(i)
//            if (item is ExistingSdk && !sdkFilter(item.sdk)) {
//                removeItemAt(i)
//            }
//        }
//    }
//
//    val lastRegisteredSdkIndex
//        get() = (0 until itemCount).firstOrNull { getItemAt(it) is AddSdkFromSdkListDownloader } ?: 0
//
//    val comment: String?
//        get() = when (selectedItem) {
//            is DownloadSdk -> CangJieUiBundle.message("sdk.download.comment")
//            is NoSdk -> when {
//                (0 until itemCount).any { getItemAt(it) is DownloadSdk } -> CangJieUiBundle.message("sdk.missing.item.comment")
//                else -> CangJieUiBundle.message("sdk.missing.item.no.internet.comment")
//            }
//
//            else -> null
//        }
//}
//
//private fun selectAndAddSdk(combo: ProjectWizardSdkComboBox) {
//    combo.popup?.hide()
//
//
////        val version = JavaSdk.getInstance().getVersionString(path)
////        val comboItem = DetectedSdk(version ?: "", path)
////        combo.detectedSDKs.add(comboItem)
////        combo.addItem(comboItem)
////        combo.selectedItem = comboItem
//
//}