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
package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.psi.CjElementImplStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 定义一个占位符Stub元素类型类，用于处理特定的编程结构，
 * 该类继承自CjStubElementType，针对占位符类型提供具体实现。
 *
 * @param <T> 继承自CjElementImplStub的泛型类型，代表Psi元素类型。
</T> */
open class CjPlaceHolderStubElementType<T : CjElementImplStub<out StubElement<*>>>
/**
 * 构造函数，初始化占位符Stub元素类型。
 *
 * @param debugName 用于调试的名称，帮助开发者识别对象类型。
 * @param psiClass 与Stub关联的Psi元素类，用于类型绑定。
 */
(debugName: @NonNls String, psiClass: Class<T>) :
    CjStubElementType<CangJiePlaceHolderStub<T>, T>(debugName, psiClass, CangJiePlaceHolderStub::class.java) {
    /**
     * 创建占位符Stub对象。
     *
     * @param psi 用于创建Stub的Psi元素，提供必要的数据。
     * @param parentStub 父Stub元素，表示Psi元素的树结构关系。
     * @return 返回创建的CangJiePlaceHolderStub对象。
     */
    override fun createStub(psi: T, parentStub: StubElement<*>?): CangJiePlaceHolderStub<T> {
        return CangJiePlaceHolderStubImpl(parentStub, this)
    }

    /**
     * 序列化Stub到输出流中，对于占位符类型，不需要实际的序列化操作。
     *
     * @param stub 待序列化的Stub对象。
     * @param dataStream 输出流，用于写入序列化数据。
     * @throws IOException 当序列化过程中发生I/O错误时抛出。
     */
    @Throws(IOException::class)
    override fun serialize(stub: CangJiePlaceHolderStub<T>, dataStream: StubOutputStream) {
        // do nothing
    }

    /**
     * 从输入流中反序列化Stub对象，对于占位符类型，直接创建新的Stub实例。
     *
     * @param dataStream 输入流，用于读取序列化数据。
     * @param parentStub 父Stub元素，用于构建Psi树结构。
     * @return 返回新创建的CangJiePlaceHolderStub对象。
     * @throws IOException 当反序列化过程中发生I/O错误时抛出。
     */
    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJiePlaceHolderStub<T> {
        return CangJiePlaceHolderStubImpl(parentStub, this)
    }
}
