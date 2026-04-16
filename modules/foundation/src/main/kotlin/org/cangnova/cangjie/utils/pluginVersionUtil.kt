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

package org.cangnova.cangjie.utils



import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 获取保存的插件版本
 *
 * 从用户主目录下的 `.cangjie/version` 文件中读取保存的插件版本号。
 *
 * 使用场景：
 * - 检查插件是否更新
 * - 版本兼容性检查
 * - 迁移数据或配置
 *
 * @return 保存的插件版本号，如果文件不存在或读取失败则返回 null
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
 * 将插件版本保存到用户配置目录
 *
 * 将当前插件版本号保存到 `~/.cangjie/version` 文件中，
 * 用于后续版本比较和兼容性检查。
 *
 * 该函数会：
 * 1. 获取当前插件实例
 * 2. 读取插件版本号
 * 3. 将版本号写入配置文件
 *
 * 使用场景：
 * - 插件首次安装或更新时保存版本信息
 * - 用于后续的版本检查和数据迁移
 *
 * 注意：如果插件未安装或版本号为 null，则不会写入文件。
 */
fun savePluginVersion() {
    val plugin = PluginManagerCore.getPlugin(PluginId.getId("cn.cangnova.cangjie"))
    val version = plugin?.version
    if (version != null) {
        Files.write(Paths.get("${System.getProperty("user.home")}/.cangjie/version"), version.toString().toByteArray())
    }
}
