import com.linqingying.utils.FileUtils
import kotlin.io.path.Path

fun main() {

    val path = Path("D:\\Code\\intellij\\intellij-cangjie\\generators\\cangjie\\intellij-cangjie-stdlib\\cangjie_libs")
    val map = FileUtils.generateFileList(path)
    println(map)
}
