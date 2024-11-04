package com.linqingying.cangjie.serialization.builtins

import java.io.File

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    if (args.size < 2) {
        println(
            """CangJie built-ins serializer

Usage: ... <destination dir> (<source dir>)+

Analyzes CangJie sources found in the given source directories and serializes
found top-level declarations to <destination dir> (*.cangjie_builtins files)"""
        )
        return
    }

    val destDir = File(args[0])

    val srcDirs = args.drop(1).map( ::File)
    assert(srcDirs.isNotEmpty()) { "At least one source directory should be specified" }

    val missing = srcDirs.filterNot(File::exists)
    assert(missing.isEmpty()) { "These source directories are missing: $missing" }

    BuiltInsSerializer.analyzeAndSerialize(
        destDir,
        srcDirs,
        extraClassPath = listOf(),

        dependOnOldBuiltIns = false
    ) { totalSize, totalFiles ->
        if (System.getProperty("cangjie.builtins.serializer.log") == "true") {
            println("Total bytes written: $totalSize to $totalFiles files")
        }
    }
}
