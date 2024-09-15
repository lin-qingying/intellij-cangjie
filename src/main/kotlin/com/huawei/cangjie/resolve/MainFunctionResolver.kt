package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.diagnostics.Errors.MAIN_FUNCTION_NUMBER_ERROR
import com.huawei.cangjie.ide.stubindex.CangJieMainFunctionFqnNameIndex
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
