package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.name.Name
import java.util.*

class ControlStructureTypingUtils {


    enum class ResolveConstruct(  name: String) {
        IF("if"), ELVIS("elvis"), EXCL_EXCL("ExclExcl"), MATCH("match"), TRY("try");

        val specialFunctionName: Name =  Name.identifier(
            "<SPECIAL-FUNCTION-FOR-" + name.uppercase(
                Locale.getDefault()
            ) + "-RESOLVE>"
        )
          val specialTypeParameterName:  Name =
             Name.identifier(
                "<TYPE-PARAMETER-FOR-" + name.uppercase(
                    Locale.getDefault()
                ) + "-RESOLVE>"
            )


    }

}
