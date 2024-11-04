package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.diagnostics.Errors.MAIN_FUNCTION_NUMBER_ERROR
import com.linqingying.cangjie.ide.stubindex.CangJieMainFunctionFqnNameIndex
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project

class MainFunctionResolver(
    private val trace: BindingTrace

) {
    fun check(c: BodiesResolveContext) {


        c.files.firstOrNull()?.let {
            checkMainFunctionNumber(it.packageFqName.moduleName.asString(), it.project)
        }
    }

    //    检查main方法数量
    private fun checkMainFunctionNumber(fqname: String, project: Project) {
        runReadAction {

            val mainFunctions =
                CangJieMainFunctionFqnNameIndex.get("$fqname.main", project)
            if (mainFunctions.size > 1) {
                mainFunctions.forEach {
                    trace.report(MAIN_FUNCTION_NUMBER_ERROR.on(it))

                }
            }

        }
    }

}
