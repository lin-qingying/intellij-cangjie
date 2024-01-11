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

enum class CjpmCommand(
    val command: String,
    val description: String,
    var executeCommand: String = "",
    val index: Int? = null,
) {


    INIT("init", "初始化", executeCommand = "init"),
    RUN("run", "运行模块", executeCommand = "run", index = 0),
    BUILD("build", "编译模块", executeCommand = "build", index = 1),
    UPDATE("update", "更新模块", executeCommand = "update", index = 2),
    CLEAN("clean", "清理模块", executeCommand = "clean", index = 3),
    CHECK("check", "检查依赖", executeCommand = "check", index = 4),
    TEST("test", "单元测试", executeCommand = "test", index = 5),


    OTHER("", "其他"),
    UNUSED("", "未使用");


    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun toArray(): Array<CjpmCommand> {

//            去掉index为null的


            val arr = CjpmCommand.entries.toMutableList()
            arr.removeIf { it.index == null }

            return arr.toTypedArray()


        }

        //        根据命令判断类型
        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun fromCommand(command: String): CjpmCommand? {
//            根据第一词判断
            val arr = CjpmCommand.entries.toMutableList()
            arr.removeAt(0)
            val firstWord = command.split(" ")[0]
//            return arr.firstOrNull { it.command == firstWord }?.apply {
//                executeCommand = command
//            }
            return when {
                arr.any { (firstWord.isNotEmpty()) && (it.command == firstWord) } -> arr.firstOrNull { it.command == firstWord }
                    ?.apply {
                        executeCommand = command
                    }
//                command.isNotEmpty() -> OTHER.apply {
//                    executeCommand = command
//                }
                else -> when {
                    command.isNotEmpty() -> OTHER.apply {
                        executeCommand = command

                    }

                    else -> UNUSED.apply {

                    }
                }
            }
        }

        //        序列化和反序列化
        @OptIn(ExperimentalStdlibApi::class)
        @JvmStatic
        fun fromInt(index: Int): CjpmCommand {
            val arr = CjpmCommand.entries.toMutableList()
            arr.removeIf { it.index == null }

            return arr.filter { it.index == index }.first()

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

        return command.toString()
    }
}
