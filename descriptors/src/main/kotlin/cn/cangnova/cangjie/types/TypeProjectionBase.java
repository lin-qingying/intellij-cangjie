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

package cn.cangnova.cangjie.types;

import cn.cangnova.cangjie.types.util.TypeUtils;

public abstract class TypeProjectionBase implements TypeProjection {
    @Override
    public String toString() {
//        if (isStarProjection()) {
//            return "*";
//        }
        if (projectionKind == Variance.INVARIANT) {
            return type.toString();
        }
        return projectionKind + " " + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeProjection that)) return false;


        if (projectionKind != that.projectionKind) return false;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = projectionKind.hashCode();
        if (TypeUtils.noExpectedType(type)) {
            result = 31 * result +19;
        } else {
            result = 31 * result + ( type.hashCode());
        }
        return result;
    }
}
