package com.linqingying.cangjie.metadata

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.metadata.decompiler.CangJieClassFinder
import com.linqingying.cangjie.serialization.CangJieMetadataVersion
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

@Service
class CangJieBinaryClassCache : Disposable {
    private val requestCaches = CopyOnWriteArrayList<WeakReference<RequestCache>>()

    private class RequestCache {
        var virtualFile: VirtualFile? = null
        var modificationStamp: Long = 0
        var result: CangJieClassFinder.Result? = null

        fun cache(
            file: VirtualFile,
            result: CangJieClassFinder.Result?
        ): CangJieClassFinder.Result? {
            virtualFile = file
            this.result = result
            modificationStamp = file.modificationStamp

            return result
        }
    }

    private val cache = object : ThreadLocal<RequestCache>() {
        override fun initialValue(): RequestCache {
            return RequestCache().also {
                requestCaches.add(WeakReference(it))
            }
        }
    }

    override fun dispose() {
        for (cache in requestCaches) {
            cache.get()?.run {
                result = null
                virtualFile = null
            }
        }
        requestCaches.clear()
        // This is only relevant for tests. We create a new instance of Application for each test, and so a new instance of this service is
        // also created for each test. However all tests share the same event dispatch thread, which would collect all instances of this
        // thread-local if they're not removed properly. Each instance would transitively retain VFS resulting in OutOfMemoryError
        cache.remove()
    }

    companion object {
        @Deprecated(
            "Please pass metadataVersion explicitly",
            ReplaceWith(
                "getCangJieBinaryClassOrClassFileContent(file, CangJieMetadataVersion.INSTANCE, fileContent = fileContent)",
                "com.linqingying.cangjie.metadata.deserialization.CangJieMetadataVersion"
            )
        )
        fun getCangJieBinaryClassOrClassFileContent(
            file: VirtualFile, fileContent: ByteArray?
        ) = getCangJieBinaryClassOrClassFileContent(file, metadataVersion = CangJieMetadataVersion.INSTANCE, fileContent = fileContent)

        fun getCangJieBinaryClassOrClassFileContent(
            file: VirtualFile, metadataVersion: CangJieMetadataVersion, fileContent: ByteArray? = null
        ): CangJieClassFinder.Result? {

            val service = ApplicationManager.getApplication().getService(CangJieBinaryClassCache::class.java)
            val requestCache = service.cache.get()

            if (file.modificationStamp == requestCache.modificationStamp && file == requestCache.virtualFile) {
                return requestCache.result
            }

            val aClass = ApplicationManager.getApplication().runReadAction(Computable {
//                VirtualFileCangJieClass.create(file, metadataVersion, fileContent)
                fileContent?.let { CangJieClassFinder.Result.ClassFileContent(it) }
            })

            return requestCache.cache(file, aClass)
        }
    }
}
