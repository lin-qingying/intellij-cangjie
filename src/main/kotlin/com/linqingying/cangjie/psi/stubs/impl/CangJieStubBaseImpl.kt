package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjElementImplStub
import com.linqingying.cangjie.psi.stubs.CangJieClassifierStub
import com.linqingying.cangjie.psi.stubs.CangJieTypeStatementStub
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import java.lang.reflect.Method

val STUB_TO_STRING_PREFIX = "CangJieStub$"

open class CangJieStubBaseImpl<T : CjElementImplStub<*>>(parent: StubElement<*>?, elementType: IStubElementType<*, *>) :
    StubBase<T>(parent, elementType) {

    companion object {
        private val LOGGER: Logger = Logger.getInstance(CangJieStubBaseImpl::class.java)
        private val BASE_STUB_INTERFACES = listOf(
//                CangJieStubWithFqName::class.java,
                CangJieClassifierStub::class.java,
                CangJieTypeStatementStub::class.java,
            NamedStub::class.java,
//                CangJieCallableStubBase::class.java
        )
    }

    override fun getStubType(): IStubElementType<out StubElement<*>, *> =
        super.getStubType() as IStubElementType<out StubElement<*>, *>

    private fun renderPropertyValues(stubInterface: Class<out Any?>): List<String> {
        return collectProperties(stubInterface).mapNotNull { property -> renderProperty(property) }.sorted()
    }

    private fun getPropertyName(method: Method): String {
        val methodName = method.name
        if (methodName.startsWith("get")) {
            return methodName.substring(3).replaceFirstChar(Char::lowercaseChar)
        }
        return methodName
    }

    private fun renderProperty(property: Method): String? {
        return try {
            val value = property.invoke(this)
            val name = getPropertyName(property)
            "$name=$value"
        } catch (e: Exception) {
            LOGGER.error(e)
            null
        }
    }

    private fun collectProperties(stubInterface: Class<*>): Collection<Method> {
        val result = ArrayList<Method>()
        result.addAll(stubInterface.declaredMethods.filter { it.parameterTypes.isEmpty() })
        for (baseInterface in stubInterface.interfaces) {
            if (baseInterface in BASE_STUB_INTERFACES) {
                result.addAll(collectProperties(baseInterface))
            }
        }
        return result
    }

    override fun toString(): String {
        val stubInterface = this::class.java.interfaces.single { it.name.contains("Stub") }
        val propertiesValues = renderPropertyValues(stubInterface)
        if (propertiesValues.isEmpty()) {
            return "$STUB_TO_STRING_PREFIX$stubType"
        }
        val properties = propertiesValues.joinToString(separator = ", ", prefix = "[", postfix = "]")
        return "$STUB_TO_STRING_PREFIX$stubType$properties"
    }

}
