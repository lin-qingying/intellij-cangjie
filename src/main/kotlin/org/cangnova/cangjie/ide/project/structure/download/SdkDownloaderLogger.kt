/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.ide.project.structure.download//package org.cangnova.cangjie.ide.projectStructure.download
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