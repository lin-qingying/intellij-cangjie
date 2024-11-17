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

package com.linqingying.cangjie.resolve.lazy.data

import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.psi.CjTypeParameterList
import com.linqingying.cangjie.psi.CjTypeReference
import com.linqingying.cangjie.psi.CjTypeStatement

class CjEnmuEntryInfo(
    override val element: CjEnumEntry,

    ) : CjClassInfo<CjEnumEntry>(
    element
) {
    override val classKind: ClassKind
        get() = ClassKind.ENUM_ENTRY



    val typeReferences:List<CjTypeReference> = element.typeReferences
}

open class CjClassInfo<T : CjTypeStatement>(
    element: T,
    override val classKind: ClassKind = ClassKind.CLASS
) : CjTypeStatementInfo<T>(element) {
    override val typeParameterList: CjTypeParameterList?
        get() = element.typeParameterList

}
//public class CjClassInfo extends CjClassOrObjectInfo<CjClass> {
//    private final ClassKind kind;
//
//    protected CjClassInfo(@NotNull CjClass classOrObject) {
//        super(classOrObject);
//        if (element instanceof CjEnumEntry) {
//            this.kind = ClassKind.ENUM_ENTRY;
//        }
//        else if (element.isInterface()) {
//            this.kind = ClassKind.INTERFACE;
//        }
//        else if (element.isEnum()) {
//            this.kind = ClassKind.ENUM_CLASS;
//        }
//        else if (element.isAnnotation()) {
//            this.kind = ClassKind.ANNOTATION_CLASS;
//        }
//        else {
//            this.kind = ClassKind.CLASS;
//        }
//    }
//
//    @Nullable
//    @Override
//    public CjTypeParameterList getTypeParameterList() {
//        return element.getTypeParameterList();
//    }
//
//    @NotNull
//    @Override
//    public ClassKind getClassKind() {
//        return kind;
//    }
//}
