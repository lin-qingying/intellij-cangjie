package cn.cangnova.cangjie.messages


import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier
@NonNls
const val BUNDLE = "messages.CangJieBundle"

object CangJieUiBundle : AbstractCangJieBundle(BUNDLE) {


    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any?): @Nls String {
        return getMessage(key, *params)
    }

    fun messagePointer(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: String): Supplier<String> {
        return getLazyMessage(key, *params)
    }
}