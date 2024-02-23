fun main() {


    val a = "file://C:/Users/27439/AppData/Local/.cjpm/git/zuchongzhi/85e880ab7f08249ea746ea2a3d7038f80a172d28"
 println(a.encodeUrl())
}

fun String.encodeUrl():String

{
    return this.replace(":","%3A")
}