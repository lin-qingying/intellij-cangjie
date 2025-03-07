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

package cn.cangnova.cangjie.references

import cn.cangnova.cangjie.configurable.services.CangJieLanguageServerServices
import cn.cangnova.cangjie.configurable.services.Feature
import cn.cangnova.cangjie.psi.CjElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.containers.MultiMap


interface CangJiePsiReferenceProvider {
    fun getReferencesByElement(element: PsiElement): Array<PsiReference>
}

interface CangJieReferenceProviderContributor {
    fun registerReferenceProviders(registrar: CangJiePsiReferenceRegistrar)

    companion object {
        fun getInstance(project: Project): CangJieReferenceProviderContributor =
            project.getService(CangJieReferenceProviderContributor::class.java)
    }
}


class CangJiePsiReferenceRegistrar {
    val providers: MultiMap<Class<out PsiElement>, CangJiePsiReferenceProvider> = MultiMap(LinkedHashMap())

    inline fun <reified E : CjElement> registerProvider(crossinline factory: (E) -> PsiReference?) {
        registerMultiProvider<E> { element ->
            factory(element)?.let { reference -> arrayOf(reference) } ?: PsiReference.EMPTY_ARRAY
        }
    }

    inline fun <reified E : CjElement> registerMultiProvider(crossinline factory: (E) -> Array<PsiReference>) {
        val provider: CangJiePsiReferenceProvider = object : CangJiePsiReferenceProvider {
            override fun getReferencesByElement(element: PsiElement): Array<PsiReference> {


                return factory(element as E)
            }
        }

        registerMultiProvider(E::class.java, provider)
    }

    fun registerMultiProvider(klass: Class<out PsiElement>, provider: CangJiePsiReferenceProvider) {
        providers.putValue(klass, provider)
    }
}
