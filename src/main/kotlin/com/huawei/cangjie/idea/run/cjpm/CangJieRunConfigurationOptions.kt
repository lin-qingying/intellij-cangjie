package com.huawei.cangjie.idea.run.cjpm

import com.intellij.execution.configurations.RunConfigurationOptions

class CangJieRunConfigurationOptions : RunConfigurationOptions() {
    var commandSelectIndex: Int? = null
    var cjcPath: String? = null
    var cjpmPath: String? = null
    var cjpmVersion: String? = null
    var cjcVersion: String? = null
    var moudleJsonPath: String? = null
}
enum class CjpmCommand(val index: Int?, val command: String, val description: String) {

    INIT(null, "init", "初始化"),
    RUN(0, "run", "运行模块"),
    BUILD(1, "build", "编译模块"),
    UPDATE(2, "update", "更新模块"),
    CLEAN(3, "clean", "清理模块"),
    CHECK(4, "check", "检查依赖"),
    TEST(5, "test", "单元测试");


    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun toArray(): Array<CjpmCommand> {

//            去掉INIT

            val arr = CjpmCommand.entries.toMutableList()
            arr.removeAt(0)
            return arr.toTypedArray()


        }

        //        序列化和反序列化
        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun fromInt(index: Int): CjpmCommand {
            val arr = CjpmCommand.entries.toMutableList()
            arr.removeAt(0)

            return arr[index]

//            return when (index) {
//                0 -> INIT
//                1 -> RUN
//                2 -> BUILD
//                3 -> UPDATE
//                4 -> CLEAN
//                5 -> CHECK
//                6 -> TEST
//                else -> throw IllegalArgumentException("Invalid ordinal $index")
//            }
        }


    }

    override fun toString(): String {

        return command
    }
}
