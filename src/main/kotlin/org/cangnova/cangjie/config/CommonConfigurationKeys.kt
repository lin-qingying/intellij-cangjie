/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.config

import org.cangnova.cangjie.cli.messages.MessageCollector

object CommonConfigurationKeys {

    @JvmField
    val CONTENT_ROOTS: CompilerConfigurationKey<List<ContentRoot>> = CompilerConfigurationKey.create("content roots")

    @JvmField
    val USE_LIGHT_TREE = CompilerConfigurationKey.create<Boolean>("light tree")

    @JvmField
    val MODULE_NAME = CompilerConfigurationKey<String>("module name")

    @JvmField
    val LANGUAGE_VERSION_SETTINGS = CompilerConfigurationKey<LanguageVersionSettings>("language version settings")

    @JvmField
    val MESSAGE_COLLECTOR_KEY = CompilerConfigurationKey.create<MessageCollector>("message collector")

}
