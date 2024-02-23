package com.huawei.cangjie.utils


import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 获取保存的插件版本
 */
  fun getSavePluginVersion(): String? {

    return try {
        val version = Files.readAllBytes(Paths.get("${System.getProperty("user.home")}/.cangjie/version"))
        String(version)
    } catch (e: Exception) {
        null
    }

}
/**
 * 将插件版本保存到LSPSERVERPATH
 */
fun savePluginVersion() {
    val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.huawei.cangjie"))
    val version = plugin?.version
    if (version != null) {
        Files.write(Paths.get("${System.getProperty("user.home")}/.cangjie/version"), version.toString().toByteArray())
    }
}
