package com.huawei.cangjie.utils

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import java.lang.ref.WeakReference

open class MappedExtensionProvider<T : Any, out R>
protected constructor(
    private val epName: ExtensionPointName<T>,
    private val map: (List<T>) -> R
){
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
            Pair(app, map(app.extensionArea.getExtensionPoint(epName).extensionList))
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
