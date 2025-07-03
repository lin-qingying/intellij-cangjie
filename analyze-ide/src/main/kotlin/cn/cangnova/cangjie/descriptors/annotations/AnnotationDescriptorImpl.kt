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

package cn.cangnova.cangjie.descriptors.annotations;


import cn.cangnova.cangjie.descriptors.SourceElement;
import cn.cangnova.cangjie.name.FqName;
import cn.cangnova.cangjie.name.Name;
import cn.cangnova.cangjie.renderer.DescriptorRenderer;
import cn.cangnova.cangjie.resolve.constants.ConstantValue;
import cn.cangnova.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AnnotationDescriptorImpl implements AnnotationDescriptor {
    private final CangJieType annotationType;
    private final Map<Name, ConstantValue<?>> valueArguments;
    private final SourceElement source;

    public AnnotationDescriptorImpl(
            @NotNull CangJieType annotationType,
            @NotNull Map<Name, ConstantValue<?>> valueArguments,
            @NotNull SourceElement source
    ) {
        this.annotationType = annotationType;
        this.valueArguments = valueArguments;
        this.source = source;
    }

    @Override
    @NotNull
    public CangJieType getType() {
        return annotationType;
    }

//    @Nullable
//    @Override
//    public FqName getFqName() {
//        return AnnotationDescriptor.DefaultImpls.getFqName(this);
//    }

    @NotNull
    @Override
    public Map<Name, ConstantValue<?>> getAllValueArguments() {
        return valueArguments;
    }

    @Override
    @NotNull
    public SourceElement getSource() {
        return source;
    }

    @Override
    public String toString() {
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderAnnotation(this, null);
    }
}
