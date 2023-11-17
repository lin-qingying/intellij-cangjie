package com.huawei.cangjie.idea.run.action.modulejson


import com.huawei.cangjie.lang.sdk.CangJieSdkManager


import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo.isWindows


class CjpmUpdateAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {


//        获取sdk的路径 执行 cjpm update
        val sdk = CangJieSdkManager.getProjectSdk()
        if (sdk == null) {
            Messages.showErrorDialog("Please setup CangJie SDK", "Error")
            return
        }

        var sdkHomePath = sdk.homePath ?: return

//        拿到cjpm.exe  tools/bin/cjpm.exe
        val cjpmPath = if (isWindows) {
            sdkHomePath += "\\tools\\bin\\cjpm.exe"
        } else {
            sdkHomePath += "/tools/bin/cjpm"
        }

//        打开终端执行命令
//        executeCommand("$cjpmPath update")
//        runCustomTask( )

    }


    override fun update(e: AnActionEvent) {

        val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)


        val fileName = file?.name

        val fileType = file?.fileType

        val isCjpmModuleJson = fileName?.startsWith("module") == true && fileType?.name == "JSON"


        e.presentation.isEnabledAndVisible = isCjpmModuleJson


    }
}
