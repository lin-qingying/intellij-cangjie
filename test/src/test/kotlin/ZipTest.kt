import com.intellij.util.io.Decompressor
import kotlin.io.path.Path
import kotlin.test.Test


class ZipTest {

    @Test
    fun testZip() {

    }
}

fun main() {
    Decompressor.Zip(Path("C:\\Users\\27439\\IdeaProjects\\intellij-cangjie\\test\\sdk_72292062823100_cangjie_0_53_13_windows_x86_64_zip")).withZipExtensions()
        .let{
            val fullMatchPath= "cangjie-0.53.13"
            if (fullMatchPath.isBlank()) it else it.removePrefixPath(fullMatchPath)
        }
        .extract(Path("C:\\Users\\27439\\IdeaProjects\\intellij-cangjie\\test\\extract"))

}