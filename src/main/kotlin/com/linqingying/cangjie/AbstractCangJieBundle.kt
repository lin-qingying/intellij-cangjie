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

package com.linqingying.cangjie

import com.linqingying.cangjie.configurable.LanguageOption
import com.linqingying.cangjie.configurable.state.PluginLanguageState
import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.*


abstract class AbstractCangJieBundle protected constructor(val pathToBundle: String) : DynamicBundle(pathToBundle) {
    @Nls
    protected fun String.withHtml(): String = "<html>$this</html>"

    // 保存当前的 ResourceBundle
    private val currentBundle: ResourceBundle
        get() = ResourceBundle.getBundle(
            pathToBundle,
            PluginLanguageState.instance.language.locale,
            CustomControl
        )


    //     自定义的 ResourceBundle Control，用来控制资源加载
    private object CustomControl : ResourceBundle.Control() {
        override fun getFallbackLocale(baseName: String?, locale: Locale?): Locale? {
            if (baseName == null) {
                throw NullPointerException()
            } else {
                val defaultLocale = LanguageOption.ENGLISH.locale
                return if (locale == defaultLocale) null else defaultLocale
            }

        }
    }

    // 动态加载资源文件
    @Nls
    override fun getMessage(key: @NonNls String, vararg params: Any?): @Nls String {
        return message(currentBundle, key, *params)
    }


}
