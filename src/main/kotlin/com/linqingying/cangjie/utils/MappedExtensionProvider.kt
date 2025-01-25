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

package com.linqingying.cangjie.utils

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import java.lang.ref.WeakReference

open class MappedExtensionProvider<T : Any, out R>
protected constructor(
    private val epName: ExtensionPointName<T>,
    private val map: (List<T>) -> R
) {
    private var cached = WeakReference<Pair<Application, R>>(null)

    fun get(): R {
        val cached = cached.get() ?: return update()
        val (app, extensions) = cached
        return if (app == ApplicationManager.getApplication()) {
            extensions
        } else {
            update()
        }
    }

    private fun update(): R {
        val newVal = ApplicationManager.getApplication().let { app ->
            Pair(app, map(epName.extensionList))
        }
        cached = WeakReference(newVal)
        return newVal.second
    }
}

class ExtensionProvider<T : Any>(epName: ExtensionPointName<T>) : MappedExtensionProvider<T, List<T>>(epName, { it }) {
    companion object {
        @JvmStatic
        fun <T : Any> create(epName: ExtensionPointName<T>): ExtensionProvider<T> = ExtensionProvider(epName)
    }
}
