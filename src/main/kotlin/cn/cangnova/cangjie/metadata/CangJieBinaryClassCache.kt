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

package cn.cangnova.cangjie.metadata

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import cn.cangnova.cangjie.metadata.decompiler.CangJieClassFinder
import cn.cangnova.cangjie.serialization.CangJieMetadataVersion
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
                "cn.cangnova.cangjie.metadata.deserialization.CangJieMetadataVersion"
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
