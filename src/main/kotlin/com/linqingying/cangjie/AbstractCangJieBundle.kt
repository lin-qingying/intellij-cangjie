package com.linqingying.cangjie

import com.linqingying.cangjie.configurable.LanguageOption
import com.linqingying.cangjie.configurable.state.PluginLanguageState
import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.*


abstract class AbstractCangJieBundle protected constructor(val pathToBundle: String) : DynamicBundle(pathToBundle) {
    @Nls
    protected fun String.withHtml(): String = "<html>$this</html>"

    // 保存当前的 ResourceBundle
    private val currentBundle: ResourceBundle
        get() = ResourceBundle.getBundle(
            pathToBundle,
            PluginLanguageState.instance.language.locale,
            CustomControl
        )

    //     自定义的 ResourceBundle Control，用来控制资源加载
    private object CustomControl : ResourceBundle.Control() {
        override fun getFallbackLocale(baseName: String?, locale: Locale?): Locale? {
            if (baseName == null) {
                throw NullPointerException()
            } else {
                val defaultLocale = LanguageOption.ENGLISH.locale
                return if (locale == defaultLocale) null else defaultLocale
            }

        }
    }
    // 新增的方法来获取原始内容
    @Nls

    fun rawMessage(  key: String): String =
        getMessage(key) // 不传递任何参数
    // 动态加载资源文件
    @Nls
    override fun getMessage(key: String, vararg params: Any): String {
        return message(currentBundle, key, *params)
    }


}
