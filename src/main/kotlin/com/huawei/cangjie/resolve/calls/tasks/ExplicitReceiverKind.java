package com.huawei.cangjie.resolve.calls.tasks;

public enum ExplicitReceiverKind {
    EXTENSION_RECEIVER,
    DISPATCH_RECEIVER,
    NO_EXPLICIT_RECEIVER,

    // A very special case.
    // In a call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' function 'invoke' has two explicit receivers:
    // 'b' (as extension receiver) and 'foo' (as dispatch receiver).
    BOTH_RECEIVERS;

    public boolean isExtensionReceiver() {
        return this == EXTENSION_RECEIVER || this == BOTH_RECEIVERS;
    }

    public boolean isDispatchReceiver() {
        return this == DISPATCH_RECEIVER || this == BOTH_RECEIVERS;
    }
}
