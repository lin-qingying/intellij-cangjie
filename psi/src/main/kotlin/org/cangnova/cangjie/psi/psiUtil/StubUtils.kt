/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.stubs.CangJieClassifierStub
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub 序列化工具
 *
 * ## 核心功能
 * 提供 Stub 索引的序列化/反序列化工具函数，以及创建类 ID 的辅助方法。
 *
 * ## 什么是 Stub？
 * - Stub 是 PSI 元素的轻量级表示，只包含关键信息（如类名、修饰符）
 * - 序列化后保存在索引文件中，用于快速查找而无需解析完整文件
 * - 反序列化时从索引文件读取，重建 Stub 树
 *
 * ## 为什么需要序列化？
 * 1. **性能**：避免每次查找都解析整个文件
 * 2. **内存**：只加载必要的信息到内存
 * 3. **持久化**：索引可以保存到磁盘，重启 IDE 后快速恢复
 *
 * ## 使用场景
 * - 在 StubElementType 的 serialize/deserialize 方法中使用
 * - 创建类、接口、枚举等声明的唯一标识符（ClassId）
 */
object StubUtils {
    /**
     * 反序列化类 ID
     *
     * ## 工作流程
     * 1. 从输入流读取类 ID 的字符串表示
     * 2. 使用 ClassId.fromString 解析为 ClassId 对象
     *
     * ## 数据格式
     * - 类 ID 以字符串形式保存，如 "com/example/MyClass"
     * - null 值表示没有类 ID（例如顶层声明、匿名类）
     *
     * @param dataStream Stub 输入流
     * @return 解析出的 ClassId，如果为 null 则返回 null
     */
    @JvmStatic
    fun deserializeClassId(dataStream: StubInputStream): ClassId? {
        val classId = dataStream.readName() ?: return null
        return ClassId.fromString(classId.string)
    }

    /**
     * 序列化类 ID
     *
     * ## 工作流程
     * 1. 将 ClassId 转换为字符串表示
     * 2. 写入输出流
     *
     * ## 空值处理
     * - 如果 classId 为 null，写入 null 值
     * - 反序列化时会正确处理 null 值
     *
     * @param dataStream Stub 输出流
     * @param classId 要序列化的类 ID（可以为 null）
     */
    @JvmStatic
    fun serializeClassId(dataStream: StubOutputStream, classId: ClassId?) {
        dataStream.writeName(classId?.asString())
    }

    /**
     * 创建嵌套类的类 ID
     *
     * ## ClassId 的概念
     * ClassId 是类的唯一标识符，由包名和类名组成，例如：
     * - 顶层类：`com.example/MyClass`
     * - 嵌套类：`com.example/OuterClass.InnerClass`
     *
     * ## 处理的场景
     *
     * ### 1. 顶层类（父 Stub 是文件 Stub）
     * ```kotlin
     * package com.example
     * class MyClass { ... }
     * ```
     * → ClassId: `com.example/MyClass`
     *
     * ### 2. 嵌套类（父 Stub 是类体 Stub）
     * ```kotlin
     * class OuterClass {
     *     class InnerClass { ... }  // ← 嵌套类
     * }
     * ```
     * → ClassId: `package/OuterClass.InnerClass`
     *
     * ### 3. 枚举条目（特殊处理）
     * ```kotlin
     * enum class Color {
     *     RED, GREEN, BLUE  // ← 枚举条目，不是嵌套类
     * }
     * ```
     * → 返回 null（枚举条目不需要 ClassId）
     *
     * ## 实现逻辑
     * 1. 检查父 Stub 类型
     * 2. 如果是文件 Stub，创建顶层类 ID（包名 + 类名）
     * 3. 如果是类体 Stub，创建嵌套类 ID（外层类 ID + 当前类名）
     * 4. 如果是枚举条目，返回 null
     *
     * ## 为什么枚举条目返回 null？
     * - 枚举条目不是独立的类，而是枚举类的实例
     * - 它们不需要单独的 ClassId 进行索引
     * - 通过枚举类的 ClassId 就可以找到所有条目
     *
     * @param parentStub 父 Stub 元素（文件或类体）
     * @param currentDeclaration 当前类声明
     * @return 创建的 ClassId，如果不适用则返回 null
     */
    @JvmStatic
    fun createNestedClassId(parentStub: StubElement<*>, currentDeclaration: CjClassLikeDeclaration): ClassId? = when {
        // 场景 1：顶层类
        parentStub is CangJieFileStub -> ClassId(parentStub.getPackageFqName(), currentDeclaration.nameAsSafeName)

        // 场景 2：嵌套类（但不是枚举条目）
        parentStub is CangJiePlaceHolderStub<*> && parentStub.stubType == CjStubElementTypes.CLASS_BODY -> {
            val containingClassStub = parentStub.parentStub as? CangJieClassifierStub
            if (containingClassStub != null && currentDeclaration !is CjEnumConstructor) {
                containingClassStub.getClassId()?.createNestedClassId(currentDeclaration.nameAsSafeName)
            } else {
                null
            }
        }

        // 场景 3：其他情况（如局部类、匿名类）
        else -> null
    }
}
