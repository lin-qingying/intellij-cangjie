/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.diagnostics.Errors.MAIN_FUNCTION_NUMBER_ERROR
import cn.cangnova.cangjie.ide.stubindex.CangJieMainFunctionFqnNameIndex
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
