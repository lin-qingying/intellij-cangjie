package com.huawei.cangjie.lang.sdk

import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.swing.JComponent
import javax.swing.JPanel


//TODO 该类需要重构  重构思路为将所有sdk相关整合

val Sdk?.cjpmPath: String get() {

    if (this != null) {
        if (sdkType is CangJieSdkType) {
            return "${homePath}/tools/bin/cjpm${if (SystemInfo.isWindows) ".exe" else ""}"
        }
    }
    return ""
}

val Sdk?.cjcPath: String get() {

    if (this != null) {
        if (sdkType is CangJieSdkType) {
            return "${homePath}/bin/cjc${if (SystemInfo.isWindows) ".exe" else ""}"
        }
    }
    return ""
}

class CangJieSdkType : SdkType("CangJie Sdk") {

//    private var sdkVersion: String? = null
//
//    private var cjcVersion: String? = null
//    private var cjpmVersion: String? = null
//
//    private var cjcPath: String? = null
//    private var sdkPath: String? = null

      val sdkAdditionalData = CangJieSdkAdditionalData()

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
////        if (additionalData is CangJieSdkAdditionalData) {
////            val myAdditionalData: CangJieSdkAdditionalData = additionalData
//
//        val additionalDataElement = Element("additionalData")
//        additionalDataElement.setAttribute("customAttribute","12123")
//        additional.addContent(additionalDataElement)
////        if (additionalData is CangJieSdkAdditionalData) {
////            additionalData.sdkVersion = sdkAdditionalData.sdkVersion
////            additionalData.cjcVersion = sdkAdditionalData.cjcVersion
////            additionalData.cjpmVersion = sdkAdditionalData.cjpmVersion
////            additionalData.cjcPath = sdkAdditionalData.cjcPath
////            additionalData.sdkPath = sdkAdditionalData.sdkPath
////            val dataElement = Element("data")
////            dataElement.setAttribute("version", sdkAdditionalData.sdkVersion)
////            dataElement.setAttribute("cjcVersion", sdkAdditionalData.cjcVersion)
////            dataElement.setAttribute("cjpmVersion", sdkAdditionalData.cjpmVersion)
////            dataElement.setAttribute("cjcPath", sdkAdditionalData.cjcPath)
////            dataElement.setAttribute("sdkPath", sdkAdditionalData.sdkPath)
////            additional.addContent(dataElement)
////
////        }
//
//println()
////        }
    }

    override fun loadAdditionalData(currentSdk: Sdk, additional: Element): SdkAdditionalData {

// 相同sdktype时，sdk对象不同，但是sdktype的对象引用地址相同
//        checkSdk(currentSdk.homePath!!)

//        val additionalDataElement = additional.getChild("additionalData")
//        val customAttribute = additionalDataElement.getAttributeValue("customAttribute")

        return  sdkAdditionalData
    //
//
//
//
//
//        return CangJieSdkAdditionalData(
//            additional.getAttributeValue("version"),
//            additional.getAttributeValue("cjcVersion"),
//            additional.getAttributeValue("cjpmVersion"),
//            additional.getAttributeValue("cjcPath"),
//            additional.getAttributeValue("sdkPath")
//        )
    }


    override fun suggestHomePath(): String {

        return ""
    }


    private fun checkSdk(path: String): Boolean {
        if (!File(path).exists()) return false
        //        执行path/bin/cjc -v
        return try {
            val process = ProcessBuilder("$path/bin/cjc", "-v").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            // 取第一个冒号后面的内容
            val version = output.split(":")[1].trim()
            sdkAdditionalData.sdkVersion = version
            sdkAdditionalData.cjcVersion = version
            sdkAdditionalData.cjcPath =
                "$path/bin/cjc" + if (System.getProperty("os.name").contains("Windows")) ".exe" else ""

//            执行path/tools/bin/cjpm -v
//            val process2 = ProcessBuilder("$path/tools/bin/cjpm", "-v").start()
//            val reader2 = BufferedReader(InputStreamReader(process2.inputStream))
//            val output2 = reader2.readLine()
//            // 取第一个冒号后面的内容
//            val version2 = output2.split(":")[1].trim()
//            sdkAdditionalData.cjpmVersion = version2
            sdkAdditionalData.cjpmPath =
                "$path/tools/bin/cjpm" + if (System.getProperty("os.name").contains("Windows")) ".exe" else ""





            version.isNotEmpty()
        } catch (e: Exception) {

            false
        }
    }

    override fun isValidSdkHome(path: String): Boolean = checkSdk(path)


    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        return "CangJie Sdk " + sdkAdditionalData.sdkVersion
    }


    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator
    ): AdditionalDataConfigurable? {
//        val additionalData = CangJieSdkAdditionalData(sdkVersion, cjcVersion, cjpmVersion, cjcPath, sdkPath)
//        sdkModificator.sdkAdditionalData = additionalData
//提交修改
//        sdkModificator.commitChanges()
//        if (sdkAdditionalData.cjcVersion != null) {
//            sdkModificator.sdkAdditionalData = sdkAdditionalData
//
//        }

        return null
//        return object : AdditionalDataConfigurable {
//
//            override fun createComponent(): JComponent? {
//                return null
//            }
//
//            override fun isModified(): Boolean {
//                return false
//            }
//
//            override fun apply() {
//
//
//            }
//
//            override fun setSdk(sdk: Sdk?) {
//                println(sdk)
//
//            }
//
//        }
    }


    override fun getVersionString(sdk: Sdk): String? {
        return sdkAdditionalData.sdkVersion
    }



    override fun getPresentableName(): String {
        return "CangJie Sdk"
    }




}

class CangJieSdkAdditionalData : SdkAdditionalData {
    var sdkVersion: String? = null
    var cjcVersion: String? = null
    var cjpmVersion: String? = null
    var cjcPath: String? = null
    var cjpmPath: String? = null

    constructor()

    constructor(
        sdkVersion: String?,
        cjcVersion: String?,
        cjpmVersion: String?,
        cjcPath: String?,
        cjpmPath: String?
    ) {
        this.sdkVersion = sdkVersion
        this.cjcVersion = cjcVersion
        this.cjpmVersion = cjpmVersion
        this.cjcPath = cjcPath
        this.cjpmPath = cjpmPath
    }

}
