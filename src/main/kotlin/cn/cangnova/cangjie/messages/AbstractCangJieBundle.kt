package cn.cangnova.cangjie.messages

import cn.cangnova.cangjie.configurable.LanguageOption
import cn.cangnova.cangjie.configurable.state.PluginLanguageState
import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.util.Locale
import java.util.ResourceBundle

abstract class AbstractCangJieBundle protected constructor(val pathToBundle: String) : DynamicBundle(pathToBundle) {
    @Nls
    protected fun String.withHtml(): String = "<html>$this</html>"

    // 保存当前的 ResourceBundle
    private val currentBundle: ResourceBundle
        get() = ResourceBundle.getBundle(
            pathToBundle,
            PluginLanguageState.Companion.instance.language.locale,
            CustomControl,
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

    // 动态加载资源文件
    @Nls
    override fun getMessage(key: @NonNls String, vararg params: Any?): @Nls String {
        return message(currentBundle, key, *params)
    }
}