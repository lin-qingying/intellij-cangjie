package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import javax.swing.Icon

interface DeclarationLookupObject : Iconable {
    val psiElement: PsiElement?
    val name: Name?

    @Deprecated("Use 'descriptor' available in 'DescriptorBasedDeclarationLookupObject' instead")
    val descriptor: DeclarationDescriptor?
}
interface DescriptorBasedDeclarationLookupObject : DeclarationLookupObject {
    @Deprecated("Use 'descriptor' available in 'DescriptorBasedDeclarationLookupObject' instead")
    override val descriptor: DeclarationDescriptor?
    val importableFqName: FqName?
    val isDeprecated: Boolean
}

data class PackageLookupObject(val fqName: FqName) : DescriptorBasedDeclarationLookupObject {
    override val psiElement: PsiElement? get() = null
    @Deprecated("Use 'descriptor' available in 'DescriptorBasedDeclarationLookupObject' instead")
    override val descriptor: DeclarationDescriptor? get() = null
    override val name: Name get() = fqName.shortName()
    override val importableFqName: FqName get() = fqName
    override val isDeprecated: Boolean get() = false
    override fun getIcon(flags: Int): Icon = AllIcons.Nodes.Package
}

