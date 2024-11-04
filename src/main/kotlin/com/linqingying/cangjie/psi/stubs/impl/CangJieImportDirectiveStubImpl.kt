package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjImportDirective
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CangJieImportDirectiveStubImpl(
    parent: StubElement<*>,
    private val isAllUnder: Boolean,
    private val importedFqName: StringRef? = null,
//    private val importedFqNames: List<StringRef> = emptyList(),
    private val isValid: Boolean,
    private val visibility: DescriptorVisibility

) : CangJieStubBaseImpl<CjImportDirective>(parent, CjStubElementTypes.IMPORT_DIRECTIVE), CangJieImportDirectiveStub {
    override fun isAllUnder(): Boolean = isAllUnder

    override fun getImportedFqName(): FqName? {
        val fqNameString = StringRef.toString(importedFqName)
        return if (fqNameString != null) FqName(fqNameString) else null
    }

//    override fun getImportedFqNames(): List<FqName> {
//     return   importedFqNames.map {
//         StringRef.toString(it)?.let { FqName(it) }!!
//        }
//    }

    override fun isValid(): Boolean = isValid
    override fun getModifierVisibility(): DescriptorVisibility {
        return visibility
    }

    override fun getPackageFqName(): FqName {
        return psi.getContainingCjFile().packageFqName

    }

}
