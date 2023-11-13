package com.huawei.cangjie.lang.sdk

import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk

//class CangJieProjectSdkTableListener : ProjectJdkTable.Listener {
//
//    override fun jdkAdded(sdk: Sdk) {
//        checkSdkType(sdk)
//    }
//
//    override fun jdkRemoved(sdk: Sdk) {
//        checkSdkType(sdk)
//    }
//
//    override fun jdkNameChanged(sdk: Sdk, previousName: String) {
//        checkSdkType(sdk)
//    }
//
//
//    private fun checkSdkType(sdk: Sdk) {
//        if (sdk.sdkType !is CangJieSdkType) {
//            // 清空SDK
//            val sdkModificator = sdk.sdkModificator
//            sdkModificator.homePath = ""
//            sdkModificator.commitChanges()
//        }
//    }
//}
