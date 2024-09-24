import Settings_gradle.BuildType.*
pluginManagement {
//    includeBuild("gradle-util")

    repositories {

        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        maven { url = uri("https://www.jitpack.io") }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "intellij-cangjie"

val build_type: String by settings

enum class BuildType {


    //    IU + nativeDebug
    IU_NATIVE_DEBUG,
    IC_DAP,
    IC_CIDR_NATIVE_DEBUG;


    companion object {

        fun fromString(str: String): BuildType {
            return when (str) {
                "IU_NATIVE_DEBUG" -> IU_NATIVE_DEBUG
                "IC_DAP" -> IC_DAP
                "IC_CIDR_NATIVE_DEBUG" -> IC_CIDR_NATIVE_DEBUG
                else -> IC_DAP
            }
        }
    }
}


//构建方式
val buildType = BuildType.fromString(build_type)



include("plugin")
include("utils")
//include("gradle-util")



//lsp支持
include("lsp")
//include("lsp1")



include("idea")

include("clion")



when (buildType) {
    IU_NATIVE_DEBUG -> {
        include("native-debugger")

    }

    IC_DAP -> {
        include("dap-debugger")

    }

    IC_CIDR_NATIVE_DEBUG -> TODO()
}
