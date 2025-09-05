/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.renderer

import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType

interface DescriptorRendererOptions {
    var classifierNamePolicy: ClassifierNamePolicy
    var withDefinedIn: Boolean
    var withSourceFileForTopLevel: Boolean
    var modifiers: Set<DescriptorRendererModifier>
    var startFromName: Boolean
    var startFromDeclarationKeyword: Boolean
    var debugMode: Boolean
    var classWithPrimaryConstructor: Boolean
    var verbose: Boolean
    var unitReturnType: Boolean
    var enhancedTypes: Boolean
    var withoutReturnType: Boolean
    var normalizedVisibilities: Boolean
    var renderDefaultVisibility: Boolean
    var renderDefaultModality: Boolean
    var renderConstructorDelegation: Boolean
    var renderPrimaryConstructorParametersAsProperties: Boolean
    var actualPropertiesInPrimaryConstructor: Boolean
    var uninferredTypeParameterAsName: Boolean

    var overrideRenderingPolicy: OverrideRenderingPolicy
    var valueParametersHandler: DescriptorRenderer.ValueParametersHandler
    var textFormat: RenderingFormat
    var excludedAnnotationClasses: Set<FqName>

    var excludedTypeAnnotationClasses: Set<FqName>
    var annotationFilter: ((AnnotationDescriptor) -> Boolean)?
    var eachAnnotationOnNewLine: Boolean

    var annotationArgumentsRenderingPolicy: AnnotationArgumentsRenderingPolicy
    val includeAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeAnnotationArguments
    val includeEmptyAnnotationArguments: Boolean get() = annotationArgumentsRenderingPolicy.includeEmptyAnnotationArguments

    var boldOnlyForNamesInHtml: Boolean

    var includePropertyConstant: Boolean
    var propertyConstantRenderer: ((ConstantValue<*>) -> String?)?
    var parameterNameRenderingPolicy: ParameterNameRenderingPolicy
    var withoutTypeParameters: Boolean
    var receiverAfterName: Boolean
    var renderCompanionObjectName: Boolean
    var withoutSuperTypes: Boolean
    var typeNormalizer: (CangJieType) -> CangJieType
    var defaultParameterValueRenderer: ((ValueParameterDescriptor) -> String)?
    var secondaryConstructorsAsPrimary: Boolean
    var propertyAccessorRenderingPolicy: PropertyAccessorRenderingPolicy
    var renderDefaultAnnotationArguments: Boolean
    var alwaysRenderModifiers: Boolean
    var renderConstructorKeyword: Boolean
    var renderUnabbreviatedType: Boolean
    var renderTypeExpansions: Boolean

    /**
     * If [renderTypeExpansions] is `true`, additionally renders the abbreviated type as a comment behind the expanded type.
     */
    var renderAbbreviatedTypeComments: Boolean

    var includeAdditionalModifiers: Boolean
    var parameterNamesInFunctionalTypes: Boolean
    var renderFunctionContracts: Boolean
    var presentableUnresolvedTypes: Boolean
    var informativeErrorType: Boolean
}

