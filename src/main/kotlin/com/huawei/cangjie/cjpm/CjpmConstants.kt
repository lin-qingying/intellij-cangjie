package com.huawei.cangjie.cjpm


object CjpmConstants {



    const val LOCK_FILE = "module-lock.json"





    const val BUILD_FILE = "build.cj"





    const val MANIFEST_FILE = "module.json"

    object ProjectLayout {
        val sources = listOf("src", "examples")
        val tests = listOf("tests", "benches")
        const val target = "build"
    }
}
