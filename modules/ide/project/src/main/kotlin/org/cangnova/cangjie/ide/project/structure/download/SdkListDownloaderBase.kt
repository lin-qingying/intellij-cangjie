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

package org.cangnova.cangjie.ide.project.structure.download


import org.cangnova.cangjie.download.sdk.CANGJIE_SDK_INDEX
import org.cangnova.cangjie.utils.isUnitTestMode
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.write
import com.intellij.util.system.CpuArch
import com.intellij.util.text.VersionComparatorUtil
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.annotations.NonNls
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Function
import kotlin.concurrent.read
import kotlin.concurrent.write


abstract class SdkListDownloaderBase {
    protected abstract val feedUrl: String

    private fun downloadSdkList(feedUrl: String, progress: ProgressIndicator?) =
        HttpRequests
            .request(feedUrl)
            .productNameAsUserAgent()
            //timeouts are handled inside
            .readBytes(progress)

    private fun downloadSdksListNoCache(feedUrl: String, progress: ProgressIndicator?): RawSdkList {

        val rawDataJson = try {
            downloadSdkList(feedUrl, progress)
        } catch (t: IOException) {
            thisLogger().warn("Failed to download the list of available SDKs from $feedUrl. ${t.message}")
            return EmptyRawSdkList
        }

        val rawData = try {
            ByteArrayInputStream(rawDataJson).use { input ->
                input.readAllBytes()
            }
        } catch (e: Throwable) {
            throw RuntimeException("Failed to unpack the list of available SDKs from $feedUrl. ${e.message}", e)
        }

        val json = try {
            SdkListParser.readTree(rawData.decodeToString())
        } catch (t: Throwable) {
            throw RuntimeException("Failed to parse the downloaded list of available SDKs. ${t.message}", t)
        }

        return RawSdkListImpl(feedUrl, json)
    }

    private val sdksListCache = CachedValueWithTTL<RawSdkList>(15 to TimeUnit.MINUTES)

    fun downloadForUI(progress: ProgressIndicator?, feedUrl: String? = null): List<SdkItem> {
        val url = feedUrl ?: this.feedUrl
        val raw = downloadSdksListNoCache(url, progress)

        sdksListCache.setValue(url, raw)

        val list = raw.getSdks(SdkPredicate.default())
        if (ApplicationManager.getApplication().isInternal) {
            return list
        }

        return list

    }

}

// 根据传入的SdkItem列表和过滤条件，构建SdkDownloaderModel
fun buildSdkDownloaderModel(allItems: List<SdkItem>, itemFilter: (SdkItem) -> Boolean = { true }): SdkDownloaderModel {
    // 根据版本对SdkItem进行分组
    val groups = allItems
        .filter { itemFilter.invoke(it) }
        .groupBy { it.version }
        .mapValues { (sdkVersion, groupItems) ->
            // 获取版本号
            val version = groupItems.first().version
            // 获取包含的SdkItem
            val includedItems = groupItems
                .map { SdkVersionVendorItem(item = it) }
            // 获取排除的SdkItem
            val excludedItems = allItems
                .asSequence()
                .filter { it !in groupItems }
                .groupBy { it }
                .mapValues { (_, sdkItems) ->
                    val comparator = Comparator.comparing(
                        Function<SdkItem, String> { it.version },
                        VersionComparatorUtil.COMPARATOR
                    )
                    //first try to find closest newer version
                    sdkItems
                        .filter { it.version >= version }
                        .minWithOrNull(comparator)
                    // if not, let's try an older version too
                        ?: sdkItems
                            .filter { it.version < version }
                            .maxWithOrNull(comparator)

                }.mapNotNull { it.value }
                .map { SdkVersionVendorItem(item = it) }

            // 构建SdkVersionItem
            SdkVersionItem(
                sdkVersion,
                includedItems.firstOrNull() ?: error("Empty group of includeItems for $sdkVersion"),
                includedItems,
                excludedItems.toList()
            )
        }
    // 对分组进行排序
    val versionItems = groups.values
        .sortedWith(
            Comparator.comparing(
                Function<SdkVersionItem, String> { it.sdkVersion },
                VersionComparatorUtil.COMPARATOR
            ).reversed()
        )
    // 获取默认的SdkItem
    val defaultItem = allItems.firstOrNull { it.isDefaultItem } /*pick the newest default SDK */
        ?: allItems.firstOrNull() /* pick just the newest SDK is no default was set (aka the JSON is broken) */
        ?: error("There must be at least one SDK to install") /* totally broken JSON */

    // 获取默认的SdkVersionItem
    val defaultSdkVersionItem = versionItems.firstOrNull { group -> group.includedItems.any { it.item == defaultItem } }
        ?: error("Default item is not found in the list")
    // 获取默认的SdkVersionVendorItem
    val defaultVersionVendor = defaultSdkVersionItem.includedItems.find { it.item == defaultItem }
        ?: defaultSdkVersionItem.includedItems.first()

    // 构建SdkDownloaderModel并返回
    return SdkDownloaderModel(
        versionGroups = versionItems,
        defaultItem = defaultItem,
        defaultVersion = defaultSdkVersionItem,
        defaultVersionVendor = defaultVersionVendor,
    )
}

@Service
class SdkListDownloader : SdkListDownloaderBase() {
    companion object {
        @JvmStatic
        fun getInstance(): SdkListDownloader = service<SdkListDownloader>()
    }

    override val feedUrl: String
        get() {
            if (isUnitTestMode) {
                val registry = runCatching { Registry.get("sdk.downloader.url").asString() }.getOrNull()
                if (!registry.isNullOrBlank()) {
                    return registry
                }
            }
            return CANGJIE_SDK_INDEX
        }
}

private class CachedValueWithTTL<T : Any>(
    private val ttl: Pair<Int, TimeUnit>
) {
    private val lock = ReentrantReadWriteLock()
    private var cachedUrl: String? = null
    private var value: T? = null
    private var computed = 0L

    private fun now() = System.currentTimeMillis()
    private operator fun Long.plus(ttl: Pair<Int, TimeUnit>): Long = this + ttl.second.toMillis(ttl.first.toLong())

    private inline fun readValueOrNull(expectedUrl: String, onValue: (T) -> Unit) {
        if (cachedUrl != expectedUrl) {
            return
        }

        val value = this.value
        if (value != null && computed + ttl > now()) {
            onValue(value)
        }
    }

    fun getOrCompute(url: String, defaultOrFailure: T, compute: () -> T): T {
        lock.read {
            readValueOrNull(url) { return it }
        }

        lock.write {
            // double-checked
            readValueOrNull(url) { return it }

            val value = runCatching(compute).getOrElse {
                if (it is ProcessCanceledException) {
                    throw it
                }
                Logger.getInstance(javaClass).warn("Failed to compute value. ${it.message}", it)
                defaultOrFailure
            }

            ProgressManager.checkCanceled()
            return setValue(url, value)
        }
    }

    fun setValue(url: String, value: T): T = lock.write {
        this.value = value
        computed = now()
        cachedUrl = url
        return value
    }
}

private interface RawSdkList {
    /**
     * 获取sdk
     *
     */
//    fun getSdk(
//        version: String,
//
//        platform: String = SystemInfo.getOsName(),
//        arch: String = SystemInfo.OS_ARCH
//
//
//    )

    fun getSdks(platform: SdkPredicate): List<SdkItem>
}

private object EmptyRawSdkList : RawSdkList {
    override fun getSdks(platform: SdkPredicate): List<SdkItem> = emptyList()
}

private class RawSdkListImpl(
    private val feedUrl: String,
    private val json: JsonObject,
) : RawSdkList {
    private val cache = ConcurrentHashMap<SdkPredicate, () -> List<SdkItem>>()


    override fun getSdks(platform: SdkPredicate): List<SdkItem> = cache.computeIfAbsent(platform) { parseJson(it) }()

    private fun parseJson(predicate: SdkPredicate): () -> List<SdkItem> {
        val result = runCatching {
            try {
                SdkListParser.parseSdkList(json, predicate)
            } catch (t: Throwable) {
                throw RuntimeException(
                    "Failed to process the downloaded list of available SDKs from $feedUrl. ${t.message}",
                    t
                )
            }
        }

        return { result.getOrThrow() }
    }
}

@ExperimentalSerializationApi
private val jsonFormat = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

data class SdkPredicate(
    private val ideBuildNumber: BuildNumber?,
    private val supportedPlatforms: Set<SdkPlatform>,
) {

    /**
     * 测试给定的 JSON 对象是否匹配支持的平台集合。
     *
     * @param pkg 一个包含 os 和 arch 字段的 JSON 对象
     * @return 如果支持的平台集合中有匹配的平台，则返回 true，否则返回 false
     */
    fun testSdkPackage(pkg: JsonObject): Boolean {
        val os = (pkg["os"] as? JsonPrimitive)?.contentOrNull ?: return false
        val arch = (pkg["arch"] as? JsonPrimitive)?.contentOrNull ?: return false
        // 遍历支持的平台集合，测试是否有匹配的平台
        return supportedPlatforms.any { platform ->
            platform.os.any { it.equals(os, ignoreCase = true) } &&
                    platform.arch.any { it.equals(arch, ignoreCase = true) }
        }
    }

    companion object {
        fun none(): SdkPredicate = SdkPredicate(null, emptySet())

        fun default(): SdkPredicate = createInstance(forWsl = false)
        fun forWSL(buildNumber: BuildNumber? = ApplicationInfo.getInstance().build): SdkPredicate =
            createInstance(forWsl = true, buildNumber)

        private fun createInstance(
            forWsl: Boolean = false,
            buildNumber: BuildNumber? = ApplicationInfo.getInstance().build
        ): SdkPredicate {
            val x86_64 = "x86_64"
            val defaultPlatform = getCurrentSdkPlatform()
            val platforms = when {
                SystemInfo.isWindows && forWsl && CpuArch.isArm64() -> listOf(
                    SdkPlatform.findPlatform(
                        "linux",
                        "aarch64"
                    )
                )

                SystemInfo.isWindows && forWsl && !CpuArch.isArm64() -> listOf(
                    SdkPlatform.findPlatform(
                        defaultPlatform,
                        newOs = "linux"
                    )
                )

                SystemInfo.isLinux && CpuArch.isArm64() -> listOf(
                    SdkPlatform.findPlatform(
                        defaultPlatform,
                        arch = "aarch64"
                    )
                )

                (SystemInfo.isMac || SystemInfo.isWindows) && CpuArch.isArm64() -> listOf(
                    defaultPlatform,
                    SdkPlatform.findPlatform(defaultPlatform, arch = "aarch64")
                )

                !SystemInfo.isWindows && forWsl -> listOf()
                else -> listOf(defaultPlatform)
            }.mapNotNull { it }

            return SdkPredicate(buildNumber, platforms.toSet())
        }
    }
}

object SdkListParser {
    fun readTree(rawData: String): JsonObject = Json.decodeFromString<JsonElement>(rawData).jsonObject

    fun parseSdkList(tree: JsonObject, filters: SdkPredicate): List<SdkItem> {
        val items = tree.values.flatMap { it.jsonArray }
        return items
            .asSequence()
            .filterIsInstance<JsonObject>()
            .flatMap { parseSdkItem(item = it, filters = filters) }
            .toList()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun parseSdkItem(item: JsonObject, filters: SdkPredicate): List<SdkItem> {


        if (!filters.testSdkPackage(item)) return emptyList()
        val contents = jsonFormat.encodeToString(item)

        return listOf(
            SdkItem(

                os = (item["os"] as? JsonPrimitive)?.contentOrNull ?: return emptyList(),
                arch = (item["arch"] as? JsonPrimitive)?.contentOrNull ?: return emptyList(),
                version = (item["version"] as? JsonPrimitive)?.contentOrNull ?: return emptyList(),
                url = (item["url"] as? JsonPrimitive)?.contentOrNull ?: return emptyList(),
                channel = (item["channel"] as? JsonPrimitive)?.contentOrNull ?: return emptyList(),
                sha256 = (item["sha256sum"] as? JsonPrimitive)?.contentOrNull ?: return emptyList(),
                ext = (item["ext"] as? JsonPrimitive)?.contentOrNull ?: return emptyList(),
                packageType = SdkPackageType.findType(
                    (item["ext"] as? JsonPrimitive)?.contentOrNull ?: return emptyList()
                ),
                saveToFile = { file -> file.write(contents) }
            )
        )


    }
}

enum class SdkPackageType(@NonNls vararg val types: String) {
    ZIP("zip") {
        override fun openDecompressor(archiveFile: Path): Decompressor =
            Decompressor.Zip(archiveFile).withZipExtensions()
    },

    @Suppress("SpellCheckingInspection")
    TAR_GZ("targz", "tar.gz") {
        override fun openDecompressor(archiveFile: Path): Decompressor =
            Decompressor.Tar(archiveFile)
    };

    abstract fun openDecompressor(archiveFile: Path): Decompressor

    companion object {
        fun findType(jsonText: String): SdkPackageType =
            entries.firstOrNull { type -> type.types.any { it.equals(jsonText, ignoreCase = true) } } ?: ZIP
    }
}

data class SdkItem(
    val isDefaultItem: Boolean = false,

    val os: String,
    val arch: String,
    val version: String,
    val url: String,
    val sha256: String,
    val packageType: SdkPackageType,
    val ext: String,
    val channel: String,
    val suggestedSdkName: String = "cangjie-$version",
    private val saveToFile: (Path) -> Unit,

    ) {
    /**
     * the CangJie Home folder (which contains the `bin` folder and `bin/java` path
     * may be deep inside a JDK package, e.g. on macOS
     * This method helps to find a traditional CangJie Home
     * from a JDK install directory
     */
    fun resolveCangJieHome(installDir: Path): Path {
        return installDir
//        val packageToBinCangJiePrefix = packageToBinCangJiePrefix
//        if (packageToBinCangJiePrefix.isBlank()) return installDir
//        return installDir.resolveName(packageToBinCangJiePrefix)
    }

    fun writeMarkerFile(file: Path) {
        saveToFile(file)
    }

    val archiveFileName get() = "cangjie-$version-$os-$arch.$ext"
}

enum class SdkPlatform(
    val os: List<String>, // 支持多个 OS 名称
    val arch: List<String> // 支持多个架构名称
) {
    WINDOWS_X86(listOf("Windows"), listOf("x86")),
    WINDOWS_X86_64(listOf("Windows"), listOf("x86_64", "amd64")),
    WINDOWS_ARM(listOf("Windows"), listOf("arm")),
    WINDOWS_ARM64(listOf("Windows"), listOf("arm64", "aarch64")),

    LINUX_X86(listOf("Linux"), listOf("x86")),
    LINUX_X86_64(listOf("Linux"), listOf("x86_64", "amd64")),
    LINUX_ARM(listOf("Linux"), listOf("arm")),
    LINUX_ARM64(listOf("Linux"), listOf("arm64", "aarch64")),

    MAC_OS_X_X86(listOf("Mac OS X", "Darwin"), listOf("x86")),
    MAC_OS_X_X86_64(listOf("Mac OS X", "Darwin"), listOf("x86_64", "amd64")),
    MAC_OS_X_ARM(listOf("Mac OS X", "Darwin"), listOf("arm")),
    MAC_OS_X_ARM64(listOf("Mac OS X", "Darwin"), listOf("arm64", "aarch64"));


    companion object {

        /**
         * 根据新的 OS 和架构查找匹配的 SdkPlatform。
         *
         * @param newOs 新的操作系统名称，默认为 null。如果为 null，则使用 basePlatform.os 的第一个值。
         * @param basePlatform 基础平台，用于提供默认的操作系统和架构。
         * @param arch 指定的架构名称，默认为 null。如果为 null，则使用 basePlatform.arch 的第一个值。
         * @return 如果找到匹配的 SdkPlatform，则返回；否则返回 null。
         */
        fun findPlatform(basePlatform: SdkPlatform, newOs: String? = null, arch: String? = null): SdkPlatform? {
            val osToMatch = newOs ?: basePlatform.os.first() // 如果 newOs 为 null，使用 basePlatform.os 的第一个值
            val archToMatch = arch ?: basePlatform.arch.first() // 如果 arch 为 null，使用 basePlatform.arch 的第一个值
            return entries.firstOrNull {
                it.os.any { it.equals(osToMatch, ignoreCase = true) } &&
                        it.arch.any { it.equals(archToMatch, ignoreCase = true) }
            }
        }

        /**
         * 根据 OS 和架构查找匹配的 SdkPlatform。
         *
         * @param os 操作系统名称
         * @param arch 架构名称
         * @return 如果找到匹配的 SdkPlatform，则返回该实例；否则返回 null
         */
        fun findPlatform(os: String, arch: String): SdkPlatform? {
            return entries.firstOrNull {
                it.os.any { it.equals(os, ignoreCase = true) } &&
                        it.arch.any { it.equals(arch, ignoreCase = true) }
            }
        }
    }
}


fun getCurrentSdkPlatform(): SdkPlatform =
    SdkPlatform.findPlatform(SystemInfo.getOsName(), SystemInfo.OS_ARCH) ?: SdkPlatform.LINUX_X86_64