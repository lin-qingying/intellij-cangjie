package com.huawei.cangjie.debugger.impl

import com.huawei.bitfun.DapFromServerService
import com.huawei.bitfun.connect.DapConnectionLauncher
import com.huawei.bitfun.intellij.ex.DapXDebugProcess
import com.huawei.bitfun.intellij.start.DebugStartType
import com.intellij.xdebugger.XDebugSession

class CangjieXDebugProcess(
    xDebugSession: XDebugSession,
    launchOrAttachArgs: Any,
    startType: DebugStartType,
    connectionLauncher: DapConnectionLauncher<DapFromServerService, *, CangjieDapToServerService>
) : DapXDebugProcess<DapFromServerService, CangjieDapToServerService>(
    xDebugSession, launchOrAttachArgs, startType, connectionLauncher
) {

    init {
        this.addUpdateNotificationListeners(xDebugSession)

    }
}
