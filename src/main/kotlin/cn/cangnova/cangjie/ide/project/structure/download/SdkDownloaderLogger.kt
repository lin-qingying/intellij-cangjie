package cn.cangnova.cangjie.ide.project.structure.download//package cn.cangnova.cangjie.ide.projectStructure.download
//
//import com.intellij.internal.statistic.eventLog.EventLogGroup
//import com.intellij.internal.statistic.eventLog.events.EventFields
//import com.intellij.internal.statistic.eventLog.events.EventId1
//import com.intellij.internal.statistic.eventLog.events.EventId2
//import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
//
//class SdkDownloaderLogger : CounterUsagesCollector() {
//    override fun getGroup(): EventLogGroup = GROUP
//
//    private val GROUP: EventLogGroup = EventLogGroup("sdk.downloader", 6)
//
//    private val DETECTED_SDK: EventId2<String?, Int> = GROUP.registerEvent(
//        "detected",
//        EventFields.String("product", SdkVersionDetector.VENDORS),
//        EventFields.Int("version")
//    )
//
//    private val DOWNLOADED_SDK: EventId2<String?, Int> = GROUP.registerEvent(
//        "sdk.downloaded",
//        EventFields.String("product", SdkVersionDetector.VENDORS),
//        EventFields.Int("version")
//    )
//
//    private val FAILURE: EventId1<DownloadFailure> = GROUP.registerEvent(
//        "failure",
//        EventFields.Enum("reason", DownloadFailure::class.java)
//    )
//
//    enum class DownloadFailure {
//        WrongProtocol, WSLIssue, FileDoesNotExist, RuntimeException, IncorrectFileSize, ChecksumMismatch, ExtractionFailed, Cancelled,
//    }
//
//    @Deprecated(message = "Use logDownload(SdkItem) instead")
//    fun logDownload(success: Boolean) {
//    }
//
//    fun logDownload(item: SdkItem) {
//        val variant = item.detectVariant()
//        DOWNLOADED_SDK.log(variant.displayName, item.sdkMajorVersion)
//    }
//
//    fun logFailed(failure: DownloadFailure) {
//        FAILURE.log(failure)
//    }
//
//    @JvmStatic
//    fun logDetected(sdkInfo: SdkVersionDetector.SdkVersionInfo?) {
//        val (name, version) = when {
//            sdkInfo == null -> null
//            sdkInfo.variant.displayName in SdkVersionDetector.VENDORS -> sdkInfo.variant.displayName to sdkInfo.version.feature
//            else -> SdkVersionDetector.Variant.Unknown.displayName to sdkInfo.version.feature
//        } ?: return
//        DETECTED_SDK.log(name, version)
//    }
//}