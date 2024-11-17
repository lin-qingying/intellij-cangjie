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

package com.linqingying.cangjie.resolve.calls.tasks;

public enum ExplicitReceiverKind {

    /**
     * 表示函数调用使用扩展接收者。
     */
    EXTENSION_RECEIVER,

    /**
     * 表示函数调用使用分发接收者。
     */
    DISPATCH_RECEIVER,

    /**
     * 表示函数调用没有显式接收者。
     */
    NO_EXPLICIT_RECEIVER,

    /**
     * 一个特殊情况。
     * 在调用 `b.foo(1)` 时，如果类 `Foo` 有一个扩展成员 `fun B.invoke(Int)`，则函数 `invoke` 有两个显式接收者：
     * `b`（作为扩展接收者）和 `foo`（作为分发接收者）。
     */
    BOTH_RECEIVERS;

    /**
     * 检查当前枚举值是否表示扩展接收者。
     *
     * @return 如果当前枚举值是 `EXTENSION_RECEIVER` 或 `BOTH_RECEIVERS`，则返回 `true`，否则返回 `false`。
     */
    public boolean isExtensionReceiver() {
        return this == EXTENSION_RECEIVER || this == BOTH_RECEIVERS;
    }

    /**
     * 检查当前枚举值是否表示分发接收者。
     *
     * @return 如果当前枚举值是 `DISPATCH_RECEIVER` 或 `BOTH_RECEIVERS`，则返回 `true`，否则返回 `false`。
     */
    public boolean isDispatchReceiver() {
        return this == DISPATCH_RECEIVER || this == BOTH_RECEIVERS;
    }
}
