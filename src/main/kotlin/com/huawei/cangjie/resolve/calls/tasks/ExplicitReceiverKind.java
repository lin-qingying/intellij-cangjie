package com.huawei.cangjie.resolve.calls.tasks;

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
