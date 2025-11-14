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

package org.cangnova.cangjie.protodebugger.notification

/**
 * 通知类型枚举
 *
 * 该枚举定义了调试器中通知消息的类型，用于区分不同严重程度的信息。
 * 这些通知类型通常用于在调试器界面显示状态信息、警告和错误消息。
 *
 * 使用场景：
 * - 调试器状态变化的用户通知
 * - 警告信息的显示（如断点设置警告）
 * - 错误消息的提示（如连接失败）
 * - 调试操作结果的反馈
 *
 * 主要功能：
 * - 标准化通知消息的分类
 * - 支持不同严重程度的消息显示
 * - 提供统一的UI反馈机制
 * - 帮助用户理解调试器状态
 */
enum class NotificationType {
    /**
     * 信息类型通知
     *
     * 用于显示一般性的信息，如操作成功完成、状态变化等。
     * 通常使用蓝色或绿色图标，表示正常的信息提示。
     *
     * 使用场景：
     * - 调试器连接成功
     * - 程序启动完成
     * - 操作成功完成的确认
     */
    INFO,

    /**
     * 警告类型通知
     *
     * 用于显示可能的问题或需要注意的情况，但不影响正常功能。
     * 通常使用黄色或橙色图标，提醒用户注意。
     *
     * 使用场景：
     * - 断点设置在优化代码中
     * - 调试器配置警告
     * - 潜在的性能问题提示
     */
    WARNING,

    /**
     * 错误类型通知
     *
     * 用于显示严重的问题，通常阻止某些功能的正常使用。
     * 通常使用红色图标，表示需要用户注意的错误。
     *
     * 使用场景：
     * - 调试器连接失败
     * - 程序崩溃或异常终止
     * - 配置错误或权限问题
     */
    ERROR
}