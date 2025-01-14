package com.linqingying.cangjie.ide.projectStructure.download

import com.intellij.diagnostic.LoadingState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.SystemInfo

import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.HttpRequests
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.tukaani.xz.XZInputStream

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


const val URL = "https://gitee.com/Lin_Qing_Ying/intellij-cangjie/raw/analyze/data/download_cangjie_index.json"

abstract class SdkDownloaderBase {
    protected abstract val feedUrl: String

    private fun downloadSdkList(feedUrl: String, progress: ProgressIndicator?) =
        HttpRequests
            .request(feedUrl)
            .productNameAsUserAgent()
            //timeouts are handled inside
            .readBytes(progress)

    private fun downloadSdksListNoCache(feedUrl: String, progress: ProgressIndicator?): RawSdkList {
        // download XZ packed version of the data (several KBs packed, several dozen KBs unpacked) and process it in-memory
        val rawDataXZ = try {
            downloadSdkList(feedUrl, progress)
        } catch (t: IOException) {
            thisLogger().warn("Failed to download the list of available SDKs from $feedUrl. ${t.message}")
            return EmptyRawSdkList
        }

        val rawData = try {
            ByteArrayInputStream(rawDataXZ).use { input ->
                XZInputStream(input).use {
                    it.readBytes()
                }
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

    fun downloadForUI(progress: ProgressIndicator?, feedUrl: String? = null) {
        //we intentionally disable cache here for all user UI requests, as of IDEA-252237
        val url = feedUrl ?: this.feedUrl
        val raw = downloadSdksListNoCache(url, progress)

//        //setting value to the cache, just in case
//        sdksListCache.setValue(url, raw)
//
//        val list = raw.getSdks(predicate)
//        if (ApplicationManager.getApplication().isInternal) {
//            return list
//        }
//
//        return list.filter { it.isVisibleOnUI }
        TODO()
    }

}


object SdkListParser {
    fun readTree(rawData: String): JsonObject = Json.decodeFromString<JsonElement>(rawData).jsonObject


}

@Service
class SdkDownloader : SdkDownloaderBase() {
    companion object {
        @JvmStatic
        fun getInstance(): SdkDownloader = service<SdkDownloader>()
    }

    override val feedUrl: String
        get() {
            if (LoadingState.COMPONENTS_LOADED.isOccurred) {
                val registry = runCatching { Registry.get("sdk.downloader.url").asString() }.getOrNull()
                if (!registry.isNullOrBlank()) {
                    return registry
                }
            }
            return URL
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

    fun getSdks()
}

private object EmptyRawSdkList : RawSdkList {
    override fun getSdks() {

    }
}

private class RawSdkListImpl(
    private val feedUrl: String,
    private val json: JsonObject,
) : RawSdkList {

    override fun getSdks() {

    }


}