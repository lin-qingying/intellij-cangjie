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
package cn.cangnova.cangjie.psi.stubs.elements

import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.parsing.CangJieParser.Companion.parse
import cn.cangnova.cangjie.psi.stubs.CangJieFileStub
import cn.cangnova.cangjie.psi.stubs.CangJieStubVersions
import cn.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjFileElementType : IStubFileElementType<CangJieFileStub> {
    private constructor() : super(NAME, CangJieLanguage)

      constructor(debugName: @NonNls String?) : super(debugName, CangJieLanguage)

    override fun getBuilder(): StubBuilder {
        return CjFileStubBuilder()
    }

    override fun getStubVersion(): Int {
        return CangJieStubVersions.SOURCE_STUB_VERSION
    }

    override fun getExternalId(): String {
        return NAME
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieFileStub, dataStream: StubOutputStream) {
        getInstance().serializeFileStub(stub, dataStream)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieFileStub {
        return getInstance().deserializeFileStub(dataStream)
    }

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode {
        val project = psi.project
        val languageForParser = getLanguageForParser(psi)
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.chars)
        return parse(builder, psi.containingFile).firstChildNode
    }



    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        getInstance().indexFile(stub as CangJieFileStub , sink)

    }



    companion object {
        private const val NAME = "cangjie.FILE"

        val INSTANCE: CjFileElementType = CjFileElementType()
    }
}
