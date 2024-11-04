package com.linqingying.cangjie.builtins

import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.TypeUtils

//enum class UnsignedType(val classId: ClassId) {
//    UBYTE(ClassId.fromString("UInt8")),
//    USHORT(ClassId.fromString("UInt16")),
//    UINT(ClassId.fromString("UInt32")),
//    ULONG(ClassId.fromString("UInt64"));
//
//    val typeName = classId.shortClassName
////    val arrayClassId = ClassId(classId.packageFqName, Name.identifier(typeName.asString() + "Array"))
//}
object UnsignedTypes {

    //    private val unsignedTypeNames = enumValues<UnsignedType>().map { it.typeName }.toSet()
//
//    private fun isUnsignedClass(descriptor: DeclarationDescriptor): Boolean {
//        val container = descriptor.containingDeclaration
//        return container is PackageFragmentDescriptor &&
//                container.fqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME &&
//                descriptor.name in UnsignedTypes.unsignedTypeNames
//    }
    @JvmStatic
    fun isUnsignedType(type: CangJieType): Boolean {
        if (TypeUtils.noExpectedType(type)) return false

        return CangJieBuiltIns.isUnsignedNumber(type)

    }
}
