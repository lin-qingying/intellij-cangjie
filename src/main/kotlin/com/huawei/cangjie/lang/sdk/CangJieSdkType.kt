package com.huawei.cangjie.lang.sdk

import com.intellij.openapi.projectRoots.*
import org.jdom.Element
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.swing.JComponent
import javax.swing.JPanel

class CangJieSdkType : SdkType("CangJie Sdk") {

    private var sdkVersion: String? = null

//    class CangJieSdkAdditionalData : SdkAdditionalData {
//
//        var sdkVersion: String? = null
//
//    }

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
//        if (additionalData is CangJieSdkAdditionalData) {
//            val myAdditionalData: CangJieSdkAdditionalData = additionalData
        val dataElement = Element("data")
        dataElement.setAttribute("version", sdkVersion)
        additional.addContent(dataElement)
//        }
    }

    override fun suggestHomePath(): String? {

        return null
    }

    override fun isValidSdkHome(path: String): Boolean {
        if (!File(path).exists()) return false

//        执行path/bin/cjc -v
        return try {
            val process = ProcessBuilder("$path/bin/cjc", "-v").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            // 取第一个冒号后面的内容
            val version = output.split(":")[1].trim()
            sdkVersion = version
            version.isNotEmpty()
        } catch (e: Exception) {

            false
        }


    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        return "CangJie Sdk"
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator
    ): AdditionalDataConfigurable {
        return object : AdditionalDataConfigurable {
            override fun createComponent(): JComponent {
                return JPanel()
            }

            override fun isModified(): Boolean {
                return false
            }

            override fun apply() {

            }

            override fun setSdk(sdk: Sdk?) {
                println(sdk)
            }

        }
    }

    override fun getVersionString(sdk: Sdk): String? {
        return sdkVersion
    }

    override fun getPresentableName(): String {
        return "CangJie Sdk"
    }
}
