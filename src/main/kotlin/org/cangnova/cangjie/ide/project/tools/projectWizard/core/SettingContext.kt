package org.cangnova.cangjie.ide.project.tools.projectWizard.core/*
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

//package org.cangnova.cangjie.ide.project.tools.projectWizard.core
//
//
//class SettingContext {
//    private val values = mutableMapOf<String, Any>()
//    private val pluginSettings = mutableMapOf<String, PluginSetting<*, *>>()
//    val eventManager = EventManager()
//
//    @Suppress("UNCHECKED_CAST")
//    operator fun <V : Any, T : SettingType<V>> get(
//        reference: SettingReference<V, T>
//    ): V? = values[reference.path] as? V
//
//    operator fun <V : Any, T : SettingType<V>> set(
//        reference: SettingReference<V, T>,
//        newValue: V
//    ) {
//        values[reference.path] = newValue
//        eventManager.fireListeners(reference)
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    fun <V : Any, T : SettingType<V>> getPluginSetting(pluginSettingReference: PluginSettingReference<V, T>) =
//        pluginSettings[pluginSettingReference.path] as PluginSetting<V, T>
//
//    fun <V : Any, T : SettingType<V>> setPluginSetting(
//        pluginSettingReference: PluginSettingReference<V, T>,
//        setting: PluginSetting<V, T>
//    ) {
//        pluginSettings[pluginSettingReference.path] = setting
//    }
//}
