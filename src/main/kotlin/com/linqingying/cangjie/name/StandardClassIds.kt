package com.linqingying.cangjie.name

import com.linqingying.cangjie.builtins.StandardNames


object StandardClassIds {
    val BASE_CANGJIE_PACKAGE = FqName("cangjie")
    val BASE_STD_PACKAGE = FqName("std")
    val BASE_STD_CORE_PACKAGE =  BASE_STD_PACKAGE.child( Name.identifier("core"))



    val BASE_STD_PACKAGES = setOf(BASE_STD_PACKAGE,BASE_STD_CORE_PACKAGE)

    val builtInsPackagesWithDefaultNamedImport:Set<FqName> = setOf(

    )

    val builtInsPackages = setOf(
        BASE_STD_CORE_PACKAGE
//          *BASE_STD_PACKAGES.toTypedArray()
    )



    fun byName(name: String) = name.baseId()




    @Suppress("FunctionName")
    fun FunctionN(n: Int):  ClassId {
        return "Function$n".baseId()
    }






    object Annotations {

        object ParameterNames {

        }
    }

    object Callables {

    }

    object Collections {

    }


}
private fun String.baseId() = ClassId(StandardClassIds.BASE_CANGJIE_PACKAGE, Name.identifier(this))
