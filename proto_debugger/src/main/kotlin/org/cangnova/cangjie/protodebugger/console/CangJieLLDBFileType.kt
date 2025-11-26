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
 */

package org.cangnova.cangjie.protodebugger.console

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * LLDB 命令文件类型
 *
 * 用于 Cjdb 控制台的文件类型定义
 */
object CangJieLLDBFileType : LanguageFileType(CangJieDebugLLDBLanguage) {

    override fun getName(): String = "CangJieLLDB"

    override fun getDescription(): String = "LLDB Command Console"

    override fun getDefaultExtension(): String = "lldb"

    override fun getIcon(): Icon? = null
}