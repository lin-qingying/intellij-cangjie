fun main() {
    // 原始乱码字符串
    val incorrectString = "浣犲ソ涓栫晫"

    // 将其视为 ISO-8859-1 编码的字节数组
    val bytes = incorrectString.toByteArray(charset("ISO-8859-1"))

    // 使用 UTF-8 解码
    val correctString = String(bytes, charset("UTF-8"))

    println(correctString) // 输出：你好世界
}
