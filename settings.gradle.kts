import Settings_gradle.BuildType.*

pluginManagement {

    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "intellij-cangjie"


val build_type: String by settings

enum class BuildType {


    //   IDEA Ultimate Edition + nativeDebug本地调试插件  LLDB
    IU_NATIVE_DEBUG,

    // IDEA Community Edition + DAP调试插件
    IC_DAP,

    //    IDEA Community Edition + cidr本地代码调试   LLDB
    IC_CIDR_NATIVE_DEBUG,


    //    CLION
    CLION_NATIVE_DEBUG,
    CLION_DAP;

    companion object {

        fun fromString(str: String): BuildType {
            return when (str) {
                "IU_NATIVE_DEBUG" -> IU_NATIVE_DEBUG
                "IC_DAP" -> IC_DAP
                "IC_CIDR_NATIVE_DEBUG" -> IC_CIDR_NATIVE_DEBUG

                "CLION_NATIVE_DEBUG" -> CLION_NATIVE_DEBUG

                "CLION_DAP" -> CLION_DAP

                else -> IC_DAP
            }
        }
    }
}


//构建方式
val buildType = BuildType.fromString(build_type)
//include("plugin")
// Configure Gradle Build Cache. It is enabled in `gradle.properties` via `org.gradle.caching`.
//buildCache {
//    local {
//        isEnabled = System.getenv("CI") == null
//        directory = File(rootDir, "build/build-cache")
//        removeUnusedEntriesAfterDays = 30
//    }
//}

//pluginManagement {
//    repositories {
//        maven("https://oss.sonatype.org/content/repositories/snapshots/")
//        gradlePluginPortal()
//    }
//}
//include("lsp4j")
include("plugin")

//检查
//include("inspections")
//高亮
//include("highlighter")
//lsp支持
include("lsp")
//描述
//include("descriptors")
//include("debugg/**/er")

//when (buildType) {
//    IU_NATIVE_DEBUG,CLION_NATIVE_DEBUG -> {
//        include("native-debugger")
//
//    }
//
//    IC_DAP ,CLION_DAP-> {
        include("dap-debugger")
//
//    }
//
//    IC_CIDR_NATIVE_DEBUG -> TODO()
//
//}
//include("cidr")

//include("back")
include("utils")

//dap协议序列化
//include("dap")
include("grammar")


include("idea")
include("clion")
