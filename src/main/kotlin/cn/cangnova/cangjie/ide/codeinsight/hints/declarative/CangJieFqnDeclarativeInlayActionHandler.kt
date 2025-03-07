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

package cn.cangnova.cangjie.ide.codeinsight.hints.declarative

import com.intellij.codeInsight.hints.declarative.InlayActionHandler
import com.intellij.codeInsight.hints.declarative.InlayActionPayload
import com.intellij.codeInsight.hints.declarative.StringInlayActionPayload
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import cn.cangnova.cangjie.references.resolveClass

/**
 * 创建一个处理全限定名（FQN）声明性内嵌动作的处理器类。
 * 这个类继承自 InlayActionHandler，并专注于处理与类的全限定名相关的内嵌动作。
 */
class CangJieFqnDeclarativeInlayActionHandler : InlayActionHandler {
    // 伴生对象中定义处理程序的名称常量
    companion object {
        const val HANDLER_NAME: String = "cangjie.fqn.class"
    }

    /**
     * 处理内嵌动作的点击事件。
     *
     * @param editor 编辑器实例，用于访问编辑器的功能。
     * @param payload 内嵌动作的有效载荷，包含触发动作时的相关信息。
     */
    override fun handleClick(editor: Editor, payload: InlayActionPayload) {
        // 获取项目实例，如果获取失败则直接返回
        val project = editor.project ?: return
        // 尝试将有效载荷转换为字符串类型，如果转换失败则直接返回
        val fqName = (payload as? StringInlayActionPayload)?.text ?: return
        // 尝试解析并导航到指定全限定名的类
        (project.resolveClass(fqName)?.navigationElement as? Navigatable)?.let {
            it.navigate(true)
        }
    }
}
