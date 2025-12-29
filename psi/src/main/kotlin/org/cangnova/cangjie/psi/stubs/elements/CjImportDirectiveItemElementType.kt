/*
 * Copyright 2025 LinQingYing. and contributors.
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

import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.psi.CjNodeType
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjImportDirectiveElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieImportDirectiveStub, CjImportDirective>(
        debugName,
        CjImportDirective::class.java,
        CangJieImportDirectiveStub::class.java,
    ) {
    override fun createStub(psi: CjImportDirective, parentStub: StubElement<*>?): CangJieImportDirectiveStub {
        // 从 PSI 收集所有导入项信息
        val importItems = psi.importItems.map { item ->
            CangJieImportDirectiveStub.ImportItemInfo(
                importedFqName = item.importedFqName,
                isAllUnder = item.isAllUnder,
                aliasName = item.aliasName
            )
        }

        return CangJieImportDirectiveStubImpl(
            parentStub!!,
            importItems
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieImportDirectiveStub, dataStream: StubOutputStream) {
        val items = stub.getImportItems()
        dataStream.writeInt(items.size)

        for (item in items) {
            // 序列化 FqName (可能为 null)
            val fqName = item.importedFqName?.asString()
            dataStream.writeBoolean(fqName != null)
            if (fqName != null) {
                dataStream.writeName(fqName)
            }

            // 序列化 isAllUnder
            dataStream.writeBoolean(item.isAllUnder)

            // 序列化 aliasName (可能为 null)
            dataStream.writeBoolean(item.aliasName != null)
            if (item.aliasName != null) {
                dataStream.writeName(item.aliasName)
            }
        }
    }

    override fun indexStub(stub: CangJieImportDirectiveStub, sink: IndexSink) {
        getInstance().indexImports(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieImportDirectiveStub {
        val itemCount = dataStream.readInt()
        val items = mutableListOf<CangJieImportDirectiveStub.ImportItemInfo>()

        for (i in 0 until itemCount) {
            // 反序列化 FqName
            val hasFqName = dataStream.readBoolean()
            val fqName = if (hasFqName) {
                val fqNameStr = dataStream.readNameString()
                if (fqNameStr != null) org.cangnova.cangjie.name.FqName(fqNameStr) else null
            } else {
                null
            }

            // 反序列化 isAllUnder
            val isAllUnder = dataStream.readBoolean()

            // 反序列化 aliasName
            val hasAlias = dataStream.readBoolean()
            val aliasName = if (hasAlias) dataStream.readNameString() else null

            items.add(CangJieImportDirectiveStub.ImportItemInfo(fqName, isAllUnder, aliasName))
        }

        return CangJieImportDirectiveStubImpl(
            parentStub,
            items
        )
    }
}

/**
 * 导入项元素类型（轻量级，不使用 Stub）
 *
 * CjImportItem 不需要索引，因为索引由父 CjImportDirective 处理
 */
class CjImportItemElementType(debugName: @NonNls String) :
    CjNodeType(debugName, CjImportItem::class.java)
