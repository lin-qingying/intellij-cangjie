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

package com.linqingying.lsp.api.lsWidget

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.linqingying.lsp.api.LspServer


/**
 * LspWidgetInternalService 是一个抽象类，提供了与LSP（Language Server Protocol）服务器交互的接口。
 * 它定义了创建显示错误输出的操作、重启和停止LSP服务器的方法。
 */
abstract class LspWidgetInternalService {

    /**
     * 创建一个显示错误输出的操作。
     *
     * @param lspServer LSP服务器实例，用于执行显示错误输出的操作。
     * @return 返回一个AnAction实例，如果没有相应的操作则返回null。
     */
    abstract fun createShowErrorOutputAction(lspServer: LspServer): AnAction?

    /**
     * 重启指定的LSP服务器。
     *
     * @param lspServer 要重启的LSP服务器实例。
     */
    abstract fun restartLspServer(lspServer: LspServer)

    /**
     * 停止指定的LSP服务器。
     *
     * @param lspServer 要停止的LSP服务器实例。
     */
    abstract fun stopLspServer(lspServer: LspServer)

    /**
     * Companion object 用于提供LspWidgetInternalService的单例访问。
     */
    internal companion object {
        /**
         * 获取LspWidgetInternalService的实例。
         *
         * @return 返回LspWidgetInternalService的实例。
         */
        fun getInstance(): LspWidgetInternalService = ApplicationManager.getApplication().service()
    }
}

