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

package cn.cangnova.cangjie.codegen.state


import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.config.LanguageVersionSettingsImpl
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext
import cn.cangnova.cangjie.types.checker.TypeSystemCommonBackendContext
import cn.cangnova.cangjie.types.model.CangJieTypeMarker
import org.jetbrains.org.objectweb.asm.Type

abstract class CangJieTypeMapperBase {
    abstract val typeSystem: TypeSystemCommonBackendContext

//    abstract fun mapClass(classifier: ClassifierDescriptor): Type
//
////    abstract fun mapTypeCommon(type: CangJieTypeMarker, mode: TypeMappingMode): Type
//
//    fun mapDefaultImpls(descriptor: ClassDescriptor): Type =
//        Type.getObjectType(mapClass(descriptor).internalName  )
}
class CangJieTypeMapper @JvmOverloads constructor(
    val bindingContext: BindingContext,
//    val classBuilderMode: ClassBuilderMode,
    private val moduleName: String,
    val languageVersionSettings: LanguageVersionSettings,
    private val useOldInlineClassesManglingScheme: Boolean,

    private val isIrBackend: Boolean = false,
    private val typePreprocessor: ((CangJieType) -> CangJieType?)? = null,
    private val namePreprocessor: ((ClassDescriptor) -> String?)? = null
) : CangJieTypeMapperBase() {
    companion object{
        val LANGUAGE_VERSION_SETTINGS_DEFAULT: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT

    }

    override val typeSystem: TypeSystemCommonBackendContext
        get() = SimpleClassicTypeSystemContext
//    override fun mapClass(classifier: ClassifierDescriptor): Type {
//        return mapType(classifier.defaultType, null, TypeMappingMode.CLASS_DECLARATION)
//    }



}