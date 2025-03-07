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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name


object RequireCangJieConstants {
    val FQ_NAME: FqName = FqName("cangjie.internal.RequireCangJie")

    val VERSION: Name = Name.identifier("version")
    val MESSAGE: Name = Name.identifier("message")
    val LEVEL: Name = Name.identifier("level")
    val VERSION_KIND: Name = Name.identifier("versionKind")
    val ERROR_CODE: Name = Name.identifier("errorCode")

    val VERSION_REGEX: Regex = "(0|[1-9][0-9]*)".let { number -> Regex("$number\\.$number(\\.$number)?") }
}
