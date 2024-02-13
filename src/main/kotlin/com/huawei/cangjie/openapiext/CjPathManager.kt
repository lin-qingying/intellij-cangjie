package com.huawei.cangjie.openapiext

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import java.nio.file.Path
import java.nio.file.Paths

const val PLUGIN_ID: String = "com.huawei.cangjie"

fun plugin(): IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!

object CjPathManager {
    private fun pluginDir(): Path = plugin().pluginPath

    fun prettyPrintersDir(): Path = pluginDir().resolve("prettyPrinters")
    fun pluginDirInSystem(): Path = Paths.get(PathManager.getSystemPath()).resolve("intellij-cangjie")

}