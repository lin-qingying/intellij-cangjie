package com.linqingying.cangjie.cjpm


import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import kotlin.io.path.exists


//interface CjpmConstantsService {
//    val MANIFEST_FILE: String
//    val BUILD_FILE: String
////    val BUILD_FILE_NAME: String
//    val LOCK_FILE: String
//
//    companion object {
//        fun getInstance(project: Project): CjpmConstantsService {
//            return project.getService(CjpmConstantsService::class.java)
////            return ApplicationManager.getApplication().getService(CjpmConstantsService::class.java)
//
//        }
//    }
//}

//class CjpmConstantsService(val project: Project? = null, toolchain: CjToolchainBase? = null) {
//
//    private val myToolchain: CjToolchainBase?
//
//    init {
//        if (toolchain == null) {
//            myToolchain = project?.toolchain
//        } else {
//            myToolchain = toolchain
//        }
//
//
//    }
//
//
//    val MANIFEST_FILE: String
//        get() {
//            if (myToolchain?.cjc()?.version?.semver == null) return "module.json"
//            if (myToolchain.cjc().version?.semver!! < SemVer.parseFromText("0.49.2")) {
//                return "module.json"
//            } else {
//                return "cjpm.toml"
//            }
//        }
//    val BUILD_FILE: String
//        get() = "build.cj"
//
//    //    override val BUILD_FILE_NAME: String
////        get() = TODO("Not yet implemented")
//    val LOCK_FILE: String
//        get() {
//            if (myToolchain?.cjc()?.version?.semver == null) return "module-lock.json"
//            if (myToolchain.cjc().version?.semver!! < SemVer.parseFromText("0.49.2")) {
//                return "module-lock.json"
//            } else {
//                return "cjpm.lock"
//            }
//        }

//}

object CjpmConstants {


    val LOCK_FILE = listOf("cjpm.lock", "module-lock.json")
//        get() {
//            return CjpmConstantsService.getInstance().LOCK_FILE
//        }


    const val BUILD_FILE = "build.cj"


    //
    val MANIFEST_FILE = listOf("cjpm.toml", "module.json")
//        get() {
//
//            return CjpmConstantsService.getInstance().MANIFEST_FILE
//
//        }

//    const val MANIFEST_FILE = "module.json"

    object ProjectLayout {
        val sources = listOf("src", "examples")
        val tests = listOf("tests", "benches")
        const val target = "target"
    }
}


fun VirtualFile.findChild(names: List<String>): VirtualFile? {

//    return names.map { findChild(it) }.firstOrNull { it != null } ?: this
//    return names.map { findChild(it) }.firstOrNull { it != null } ?: this
    return names.map { findChild(it) }.firstOrNull { it != null }
}

  fun Path.resolve(lockFile: List<String>): Path {
    return lockFile.map { resolve(it) }.firstOrNull { it.exists() } ?: this

}
