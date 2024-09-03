package com.huawei.cangjie.psi.typeRefHelpers

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
import com.huawei.cangjie.psi.psiUtil.siblings

fun CjFunctionType.setReceiverTypeReference(typeRef: CjTypeReference?) =
    doSetReceiverTypeReference(
        typeRef,
        { receiverTypeReference },
        {
            (addBefore(
               CjPsiFactory(project).createFunctionTypeReceiver(it),
                parameterList ?: firstChild
            ) as CjFunctionTypeReceiver).typeReference
        }
    )
fun CjCallableDeclaration.setReceiverTypeReference(typeRef: CjTypeReference?) =
    doSetReceiverTypeReference(
        typeRef,
        { receiverTypeReference },
        { this.addBefore(it, nameIdentifier ?: valueParameterList) as CjTypeReference }
    )

private inline fun <T : CjElement> T.doSetReceiverTypeReference(
    typeRef: CjTypeReference?,
    getReceiverTypeReference: T.() -> CjTypeReference?,
    addReceiverTypeReference: T.(typeRef: CjTypeReference) -> CjTypeReference
): CjTypeReference? {
    val needParentheses = typeRef != null && typeRef.typeElement is CjFunctionType && !typeRef.hasParentheses()
    val oldTypeRef = getReceiverTypeReference()
    if (typeRef != null) {
        val newTypeRef =
            if (oldTypeRef != null) {
                oldTypeRef.replace(typeRef) as CjTypeReference
            } else {
                val newTypeRef = addReceiverTypeReference(typeRef)
                addAfter(CjPsiFactory(project).createDot(), newTypeRef.parentsWithSelf.first { it.parent == this })
                newTypeRef
            }
        if (needParentheses) {
            val argList = CjPsiFactory(project).createCallArguments("()")
            newTypeRef.addBefore(argList.leftParenthesis!!, newTypeRef.firstChild)
            newTypeRef.add(argList.rightParenthesis!!)
        }
        return newTypeRef
    } else {
        if (oldTypeRef != null) {
            val dotSibling = oldTypeRef.parent as? CjFunctionTypeReceiver ?: oldTypeRef
            val dot = dotSibling.siblings(forward = true).firstOrNull { it.node.elementType == CjTokens.DOT }
            deleteChildRange(dotSibling, dot ?: dotSibling)
        }
        return null
    }
}
