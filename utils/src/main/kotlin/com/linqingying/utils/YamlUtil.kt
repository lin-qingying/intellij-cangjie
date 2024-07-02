package com.linqingying.utils

import org.yaml.snakeyaml.Yaml
import java.io.File

object YamlUtil {

    /**
     * 读取yaml文件
     * @param filePath 文件路径
     * @return 返回读取到的yaml文件内容
     */


    fun load(filePath: String): Map<String, Any> {
        return Yaml().load(filePath)
    }


    fun getValue(data: Map<String, Any>, key: String): Any? {
        return data[key]
    }


    /**
     * 获取是否开启 lsp
     */
    val isLsp: Boolean get() {
        return getValue(load(File("./config.yaml").absolutePath), "lsp") as Boolean
    }




}
