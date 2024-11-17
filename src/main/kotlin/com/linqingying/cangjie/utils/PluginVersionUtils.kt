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
    val plugin = PluginManagerCore.getPlugin(PluginId.getId("com.linqingying.cangjie"))
    val version = plugin?.version
    if (version != null) {
        Files.write(Paths.get("${System.getProperty("user.home")}/.cangjie/version"), version.toString().toByteArray())
    }
}
