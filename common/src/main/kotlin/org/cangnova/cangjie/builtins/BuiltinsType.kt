package org.cangnova.cangjie.builtins;

import org.cangnova.cangjie.name.Name

//    内置类型名称
enum class BuiltinsType(private val _typeName: String) {
    CPOINTER("CPointer"),
    CSTRING("CString")
    ,CFUNC("CFunc")
    ;

    val typeName: Name
        get() {
            return Name.identifier(_typeName)

        }
}
