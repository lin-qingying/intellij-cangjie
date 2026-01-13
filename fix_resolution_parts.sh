#!/bin/bash

FILE="analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/components/ResolutionParts.kt"

# 修复行1249: 比较条件
sed -i '1249s/substitutedKnownTypeParameter !== typeParameterType/substitutedKnownTypeParameter !== typeParameterType.unwrap()/' "$FILE"

# 修复行1250: 移除!!.unwrap()
sed -i '1250s/substitutedKnownTypeParameter!!.unwrap()/substitutedKnownTypeParameter/' "$FILE"

# 修复行1255-1259: 修改返回语句
sed -i '1255,1259d' "$FILE"
sed -i '1254a\
\
        // 组合已知参数替换器和类型变量映射\
        return knownTypeParametersSubstitutor.compose(\
            ComposableTypeSubstitutor.create(knownTypeParameterByTypeVariable)\
        )' "$FILE"

# 修复行1305和1308: 修改EmptySubstitutor为ComposableTypeSubstitutor.EMPTY
sed -i '1305s/EmptySubstitutor/ComposableTypeSubstitutor.EMPTY/' "$FILE"

# 修复行1330: 修改substitute为safeSubstitute
sed -i '1330s/knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)/knownTypeParametersResultingSubstitutor?.safeSubstitute(typeParameter.defaultType.unwrap())/' "$FILE"

# 修复行1769: ErrorDescriptorResolutionPart中的EmptySubstitutor
sed -i '1769s/EmptySubstitutor/ComposableTypeSubstitutor.EMPTY/' "$FILE"

echo "修复完成"
