
pluginManagement {

    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "intellij-cangjie"



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
//include("debugger1")
//include("cidr")

//include("back")

//dap协议序列化
//include("dap")
include("grammar")