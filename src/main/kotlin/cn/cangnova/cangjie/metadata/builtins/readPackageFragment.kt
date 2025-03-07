/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.metadata.builtins

import com.google.protobuf.ExtensionRegistryLite
import cn.cangnova.cangjie.metadata.ProtoBuf

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

