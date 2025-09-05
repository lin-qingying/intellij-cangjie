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

package org.cangnova.cangjie.cli.messages

enum class CompilerSystemProperties(val property: String, val alwaysDirectAccess: Boolean = false) {
    OS_NAME("os.name", alwaysDirectAccess = true),
    CANGJIE_COLORS_ENABLED_PROPERTY("cangjie.colors.enabled"),

    CANGJIE_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY("cangjie.environment.keepalive");
    private fun <T> getProperFunction(custom: T?, default: T): T {
        if (alwaysDirectAccess) return default
        return custom ?: default
    }

    companion object {
        var systemPropertyGetter: ((String) -> String?)? = null

        var systemPropertySetter: ((String, String) -> String?)? = null

        var systemPropertyCleaner: ((String) -> String?)? = null
    }
    var value: String?
        get() {
            return getProperFunction(systemPropertyGetter, System::getProperty)(property)
        }
        set(value) {
            getProperFunction(systemPropertySetter, System::setProperty)(property, value!!)
        }
}
fun String?.toBooleanLenient(): Boolean? = when (this?.lowercase()) {
    null -> false
    in listOf("", "yes", "true", "on", "y") -> true
    in listOf("no", "false", "off", "n") -> false
    else -> null
}
val isWindows: Boolean
    get() = CompilerSystemProperties.OS_NAME.value!!.lowercase().startsWith("windows")
