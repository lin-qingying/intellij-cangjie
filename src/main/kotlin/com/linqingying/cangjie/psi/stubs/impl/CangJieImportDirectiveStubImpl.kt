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

package com.linqingying.cangjie.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjImportDirective
import com.linqingying.cangjie.psi.CjMultiImportDirective
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveStub
import com.linqingying.cangjie.psi.stubs.CangJieMultiImportDirectiveStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CangJieImportDirectiveStubImpl(
    parent: StubElement<*>,
    private val isAllUnder: Boolean,
    private val importedFqName: StringRef? = null,

    private val isValid: Boolean,
    private val visibility: DescriptorVisibility

) : CangJieStubBaseImpl<CjImportDirective>(parent, CjStubElementTypes.IMPORT_DIRECTIVE), CangJieImportDirectiveStub {
    override fun isAllUnder(): Boolean = isAllUnder

    override fun getImportedFqName(): FqName? {
        val fqNameString = StringRef.toString(importedFqName)
        return if (fqNameString != null) FqName(fqNameString) else null
    }



    override fun isValid(): Boolean = isValid
    override fun getModifierVisibility(): DescriptorVisibility {
        return visibility
    }

    override fun getPackageFqName(): FqName {
        return psi.getContainingCjFile().packageFqName

    }

}


class CangJieMulitImportDirectiveStubImpl(
    parent: StubElement<*>,


    private val visibility: DescriptorVisibility

) : CangJieStubBaseImpl<CjMultiImportDirective>(parent, CjStubElementTypes.MULTI_IMPORT_DIRECTIVE),
    CangJieMultiImportDirectiveStub {


    override fun getModifierVisibility(): DescriptorVisibility {
        return visibility
    }

    override fun getPackageFqName(): FqName {
        return psi.getContainingCjFile().packageFqName

    }

}
