package  inspections


import com.huawei.cangjie.AbstractCangJieBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey


const val BUNDLE = "messages.CangJieInspectionsBundle"

object CangJieInspectionsBundle : AbstractCangJieBundle( BUNDLE) {
    @Nls
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle =  BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)

    @Nls
    @JvmStatic
    fun htmlMessage(@NonNls @PropertyKey(resourceBundle =  BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params).withHtml()

    @Nls
    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle =  BUNDLE) key: String, vararg params: Any): () -> String = { getMessage(key, *params) }
}
