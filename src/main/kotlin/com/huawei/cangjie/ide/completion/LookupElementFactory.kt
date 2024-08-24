package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.utils.CallType
import com.huawei.cangjie.utils.ReceiverType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Editor
interface AbstractLookupElementFactory {
    fun createStandardLookupElementsForDescriptor(descriptor: DeclarationDescriptor, useReceiverTypes: Boolean): Collection<LookupElement>

    fun createLookupElement(
        descriptor: DeclarationDescriptor,
        useReceiverTypes: Boolean,
        qualifyNestedClasses: Boolean = false,
        includeClassTypeArguments: Boolean = true,
        parametersAndTypeGrayed: Boolean = false
    ): LookupElement?
}
//data /* we need copy() */
//class LookupElementFactory(
//    val basicFactory: BasicLookupElementFactory,
//    private val editor: Editor,
//    private val receiverTypes: Collection<ReceiverType>?,
//    private val callType: CallType<*>,
//    private val inDescriptor: DeclarationDescriptor,
//    private val contextVariablesProvider: ContextVariablesProvider,
//    private val standardLookupElementsPostProcessor: (LookupElement) -> LookupElement = { it }
//) : AbstractLookupElementFactory {
//    override fun createStandardLookupElementsForDescriptor(
//        descriptor: DeclarationDescriptor,
//        useReceiverTypes: Boolean
//    ): Collection<LookupElement> {
//        TODO("Not yet implemented")
//    }
//
//    override fun createLookupElement(
//        descriptor: DeclarationDescriptor,
//        useReceiverTypes: Boolean,
//        qualifyNestedClasses: Boolean,
//        includeClassTypeArguments: Boolean,
//        parametersAndTypeGrayed: Boolean
//    ): LookupElement? {
//        TODO("Not yet implemented")
//    }
//}
