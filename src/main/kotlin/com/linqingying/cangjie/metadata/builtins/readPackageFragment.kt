/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.linqingying.cangjie.metadata.builtins

import com.google.protobuf.ExtensionRegistryLite
import com.linqingying.cangjie.metadata.ProtoBuf

import java.io.InputStream

/**
 *
 * 从输入流中读取内置包片段信息和版本信息
 * 此函数首先从输入流中读取版本信息，然后根据版本信息的兼容性决定是否解析包片段的protobuf表示
 *
 * @return 一个Pair对象，包含可能为null的PackageFragment实例和非null的BuiltInsBinaryVersion实例
 */
fun InputStream.readBuiltinsPackageFragment(): Pair<ProtoBuf.PackageFragment?, BuiltInsBinaryVersion> =
    use { stream ->
        // 从输入流中读取内置二进制版本信息
        val version = BuiltInsBinaryVersion.readFrom(stream)
        // 根据版本信息判断是否与当前编译器版本兼容
        val proto =
            if (version.isCompatibleWithCurrentCompilerVersion()) ProtoBuf.PackageFragment.parseFrom(
                stream,
                ExtensionRegistryLite.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions)
            )
            else null
        // 将解析的proto对象与版本信息作为Pair返回
        proto to version
    }

