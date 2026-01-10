# Join 与 Meet 操作

本文档描述仓颉语言编译器中的 Join（最小上界）和 Meet（最大下界）操作，用于计算类型的公共父类型和公共子类型。

## 1. 概述

### 1.1 定义

- **Join (⊔)**：计算类型的最小上界 (Least Upper Bound, LUB)
  - `Join(T1, T2)` 返回最小的类型 `T`，使得 `T1 <: T` 且 `T2 <: T`

- **Meet (⊓)**：计算类型的最大下界 (Greatest Lower Bound, GLB)
  - `Meet(T1, T2)` 返回最大的类型 `T`，使得 `T <: T1` 且 `T <: T2`

### 1.2 应用场景

| 场景 | 使用的操作 |
|------|-----------|
| if-else 表达式的结果类型 | Join |
| match 表达式的结果类型 | Join |
| 数组字面量的元素类型 | Join |
| 函数参数的逆变位置 | Meet |
| 约束求解的下界合并 | Join |
| 约束求解的上界合并 | Meet |

## 2. 双模式架构

编译器使用统一的实现处理 Join 和 Meet 操作：

```cpp
struct DualMode {
    Ptr<Ty> bound;                              // 默认结果 (Any for join, Nothing for meet)
    function<Ptr<Ty>(set<Ptr<Ty>>)> coFunc;     // 协变位置的函数
    function<Ptr<Ty>(set<Ptr<Ty>>)> contraFunc; // 逆变位置的函数
    function<bool(Ptr<Ty>, Ptr<Ty>)> coSubtyFunc; // 协变子类型检查
};

// Join 模式
DualMode JoinMode {
    .bound = AnyTy,
    .coFunc = BatchJoin,
    .contraFunc = BatchMeet,
    .coSubtyFunc = IsSubtype
};

// Meet 模式
DualMode MeetMode {
    .bound = NothingTy,
    .coFunc = BatchMeet,
    .contraFunc = BatchJoin,
    .coSubtyFunc = IsSupertype
};
```

## 3. BatchJoin 算法

### 3.1 主函数

```cpp
Ptr<Ty> BatchJoin(const set<Ptr<Ty>>& types) {
    if (types.empty()) {
        return NothingTy;  // 空集的 Join 是 Nothing
    }

    if (types.size() == 1) {
        return *types.begin();
    }

    // 1. 展平联合类型
    set<Ptr<Ty>> flattened = FlattenUnionTypes(types);

    // 2. 过滤被覆盖的类型
    set<Ptr<Ty>> filtered = FilterCoveredTypes(flattened);

    // 3. 尝试找到已存在的最小类型
    auto existing = FindSmallestExisting(filtered);
    if (existing) {
        return existing;
    }

    // 4. 尝试函数类型 Join
    auto funcJoin = TryFuncTypeJoin(filtered);
    if (funcJoin) {
        return funcJoin;
    }

    // 5. 尝试元组类型 Join
    auto tupleJoin = TryTupleTypeJoin(filtered);
    if (tupleJoin) {
        return tupleJoin;
    }

    // 6. 通过继承层次找公共父类型
    auto commonSuper = FindCommonSupertype(filtered);
    if (commonSuper) {
        return commonSuper;
    }

    // 7. 无公共父类型，返回 Any
    return AnyTy;
}
```

### 3.2 展平联合类型

```cpp
set<Ptr<Ty>> FlattenUnionTypes(const set<Ptr<Ty>>& types) {
    set<Ptr<Ty>> result;

    for (auto& ty : types) {
        if (ty->IsUnion()) {
            // 递归展平
            auto& unionTy = ty->AsUnionTy();
            for (auto& alt : unionTy.alternatives) {
                auto flattened = FlattenUnionTypes({alt});
                result.insert(flattened.begin(), flattened.end());
            }
        } else {
            result.insert(ty);
        }
    }

    return result;
}
```

### 3.3 过滤被覆盖的类型

```cpp
set<Ptr<Ty>> FilterCoveredTypes(const set<Ptr<Ty>>& types) {
    set<Ptr<Ty>> result;

    for (auto& ty : types) {
        bool covered = false;

        // 检查是否被其他类型覆盖
        for (auto& other : types) {
            if (ty != other && IsSubtype(ty, other, false)) {
                // ty 是 other 的子类型，ty 被覆盖
                covered = true;
                break;
            }
        }

        if (!covered) {
            result.insert(ty);
        }
    }

    return result;
}
```

### 3.4 找已存在的最小类型

```cpp
optional<Ptr<Ty>> FindSmallestExisting(const set<Ptr<Ty>>& types) {
    for (auto& candidate : types) {
        bool isSuper = true;

        // 检查 candidate 是否是所有类型的父类型
        for (auto& ty : types) {
            if (!IsSubtype(ty, candidate, false)) {
                isSuper = false;
                break;
            }
        }

        if (isSuper) {
            return candidate;
        }
    }

    return nullopt;
}
```

## 4. 函数类型 Join/Meet

### 4.1 函数类型 Join

```cpp
optional<Ptr<Ty>> TryFuncTypeJoin(const set<Ptr<Ty>>& types) {
    // 检查是否全部是函数类型
    if (!AllAreFuncTypes(types)) {
        return nullopt;
    }

    // 检查参数数量是否一致
    size_t paramCount = (*types.begin())->AsFuncTy().paramTys.size();
    for (auto& ty : types) {
        if (ty->AsFuncTy().paramTys.size() != paramCount) {
            return nullopt;
        }
    }

    // 收集各位置的类型
    vector<set<Ptr<Ty>>> paramTypeSets(paramCount);
    set<Ptr<Ty>> retTypes;

    for (auto& ty : types) {
        auto& func = ty->AsFuncTy();
        for (size_t i = 0; i < paramCount; i++) {
            paramTypeSets[i].insert(func.paramTys[i]);
        }
        retTypes.insert(func.retTy);
    }

    // 参数类型：逆变位置使用 Meet
    vector<Ptr<Ty>> joinedParams;
    for (auto& paramSet : paramTypeSets) {
        auto met = BatchMeet(paramSet);
        if (met->IsNothing()) {
            return nullopt;  // 无法找到公共参数类型
        }
        joinedParams.push_back(met);
    }

    // 返回类型：协变位置使用 Join
    auto joinedRet = BatchJoin(retTypes);

    // 验证结果
    auto result = MakeFuncTy(joinedParams, joinedRet);
    for (auto& ty : types) {
        if (!IsSubtype(ty, result, false)) {
            return nullopt;  // 结果不满足要求
        }
    }

    return result;
}
```

### 4.2 函数类型 Meet

```cpp
optional<Ptr<Ty>> TryFuncTypeMeet(const set<Ptr<Ty>>& types) {
    if (!AllAreFuncTypes(types)) {
        return nullopt;
    }

    size_t paramCount = (*types.begin())->AsFuncTy().paramTys.size();
    for (auto& ty : types) {
        if (ty->AsFuncTy().paramTys.size() != paramCount) {
            return nullopt;
        }
    }

    vector<set<Ptr<Ty>>> paramTypeSets(paramCount);
    set<Ptr<Ty>> retTypes;

    for (auto& ty : types) {
        auto& func = ty->AsFuncTy();
        for (size_t i = 0; i < paramCount; i++) {
            paramTypeSets[i].insert(func.paramTys[i]);
        }
        retTypes.insert(func.retTy);
    }

    // 参数类型：逆变位置使用 Join（对偶）
    vector<Ptr<Ty>> metParams;
    for (auto& paramSet : paramTypeSets) {
        auto joined = BatchJoin(paramSet);
        metParams.push_back(joined);
    }

    // 返回类型：协变位置使用 Meet
    auto metRet = BatchMeet(retTypes);

    auto result = MakeFuncTy(metParams, metRet);
    for (auto& ty : types) {
        if (!IsSubtype(result, ty, false)) {
            return nullopt;
        }
    }

    return result;
}
```

## 5. 元组类型 Join/Meet

### 5.1 元组类型 Join

```cpp
optional<Ptr<Ty>> TryTupleTypeJoin(const set<Ptr<Ty>>& types) {
    if (!AllAreTupleTypes(types)) {
        return nullopt;
    }

    // 检查元素数量是否一致
    size_t elemCount = (*types.begin())->AsTupleTy().elementTys.size();
    for (auto& ty : types) {
        if (ty->AsTupleTy().elementTys.size() != elemCount) {
            return nullopt;
        }
    }

    // 收集各位置的元素类型
    vector<set<Ptr<Ty>>> elemTypeSets(elemCount);
    for (auto& ty : types) {
        auto& tuple = ty->AsTupleTy();
        for (size_t i = 0; i < elemCount; i++) {
            elemTypeSets[i].insert(tuple.elementTys[i]);
        }
    }

    // 逐元素 Join（元组在所有位置都是协变的）
    vector<Ptr<Ty>> joinedElems;
    for (auto& elemSet : elemTypeSets) {
        joinedElems.push_back(BatchJoin(elemSet));
    }

    return MakeTupleTy(joinedElems);
}
```

### 5.2 元组类型 Meet

```cpp
optional<Ptr<Ty>> TryTupleTypeMeet(const set<Ptr<Ty>>& types) {
    if (!AllAreTupleTypes(types)) {
        return nullopt;
    }

    size_t elemCount = (*types.begin())->AsTupleTy().elementTys.size();
    for (auto& ty : types) {
        if (ty->AsTupleTy().elementTys.size() != elemCount) {
            return nullopt;
        }
    }

    vector<set<Ptr<Ty>>> elemTypeSets(elemCount);
    for (auto& ty : types) {
        auto& tuple = ty->AsTupleTy();
        for (size_t i = 0; i < elemCount; i++) {
            elemTypeSets[i].insert(tuple.elementTys[i]);
        }
    }

    // 逐元素 Meet
    vector<Ptr<Ty>> metElems;
    for (auto& elemSet : elemTypeSets) {
        auto met = BatchMeet(elemSet);
        if (met->IsNothing()) {
            return nullopt;  // 某元素位置不兼容
        }
        metElems.push_back(met);
    }

    return MakeTupleTy(metElems);
}
```

## 6. 通过继承层次查找公共父类型

```cpp
optional<Ptr<Ty>> FindCommonSupertype(const set<Ptr<Ty>>& types) {
    if (types.empty()) {
        return nullopt;
    }

    // 1. 收集第一个类型的所有父类型
    auto first = *types.begin();
    set<Ptr<Ty>> candidates = GetAllSuperTypes(first);
    candidates.insert(first);

    // 2. 与其他类型的父类型取交集
    for (auto& ty : types) {
        if (ty == first) continue;

        set<Ptr<Ty>> supers = GetAllSuperTypes(ty);
        supers.insert(ty);

        set<Ptr<Ty>> intersection;
        for (auto& candidate : candidates) {
            if (supers.count(candidate)) {
                intersection.insert(candidate);
            }
        }
        candidates = intersection;
    }

    // 3. 找到最小的公共父类型
    if (candidates.empty()) {
        return nullopt;
    }

    Ptr<Ty> smallest = nullptr;
    for (auto& candidate : candidates) {
        if (smallest == nullptr) {
            smallest = candidate;
        } else if (IsSubtype(candidate, smallest, false)) {
            smallest = candidate;
        }
    }

    return smallest;
}
```

## 7. BatchMeet 算法

```cpp
Ptr<Ty> BatchMeet(const set<Ptr<Ty>>& types) {
    if (types.empty()) {
        return AnyTy;  // 空集的 Meet 是 Any
    }

    if (types.size() == 1) {
        return *types.begin();
    }

    // 1. 展平交集类型
    set<Ptr<Ty>> flattened = FlattenIntersectionTypes(types);

    // 2. 找到最大的已存在类型
    auto existing = FindLargestExisting(flattened);
    if (existing) {
        return existing;
    }

    // 3. 尝试函数类型 Meet
    auto funcMeet = TryFuncTypeMeet(flattened);
    if (funcMeet) {
        return funcMeet;
    }

    // 4. 尝试元组类型 Meet
    auto tupleMeet = TryTupleTypeMeet(flattened);
    if (tupleMeet) {
        return tupleMeet;
    }

    // 5. 类型不兼容，返回 Nothing
    return NothingTy;
}
```

### 展平交集类型

```cpp
set<Ptr<Ty>> FlattenIntersectionTypes(const set<Ptr<Ty>>& types) {
    set<Ptr<Ty>> result;

    for (auto& ty : types) {
        if (ty->IsIntersection()) {
            auto& intersect = ty->AsIntersectionTy();
            for (auto& comp : intersect.components) {
                auto flattened = FlattenIntersectionTypes({comp});
                result.insert(flattened.begin(), flattened.end());
            }
        } else {
            result.insert(ty);
        }
    }

    return result;
}
```

### 找最大已存在类型

```cpp
optional<Ptr<Ty>> FindLargestExisting(const set<Ptr<Ty>>& types) {
    for (auto& candidate : types) {
        bool isSubOfAll = true;

        for (auto& ty : types) {
            if (!IsSubtype(candidate, ty, false)) {
                isSubOfAll = false;
                break;
            }
        }

        if (isSubOfAll) {
            return candidate;
        }
    }

    return nullopt;
}
```

## 8. 用户可见类型转换

联合类型和交集类型在某些情况下需要转换为用户可见的类型：

```cpp
Ptr<Ty> ToUserVisibleTy(const Ptr<Ty>& ty) {
    if (ty->IsUnion()) {
        // 联合类型 → Join 结果
        auto& unionTy = ty->AsUnionTy();
        set<Ptr<Ty>> alternatives(
            unionTy.alternatives.begin(),
            unionTy.alternatives.end()
        );
        return BatchJoin(alternatives);
    }

    if (ty->IsIntersection()) {
        // 交集类型 → 最具体的类型
        auto& intersect = ty->AsIntersectionTy();
        for (auto& comp : intersect.components) {
            bool isSmallest = true;
            for (auto& other : intersect.components) {
                if (comp != other && !IsSubtype(comp, other, false)) {
                    isSmallest = false;
                    break;
                }
            }
            if (isSmallest) {
                return ToUserVisibleTy(comp);
            }
        }
        return intersect.components[0];
    }

    // 递归处理函数和元组的返回类型
    if (ty->IsFunc()) {
        auto& func = ty->AsFuncTy();
        auto visibleRet = ToUserVisibleTy(func.retTy);
        if (!IsTyEqual(visibleRet, func.retTy)) {
            return MakeFuncTy(func.paramTys, visibleRet);
        }
    }

    return ty;
}
```

## 9. 错误处理

### 9.1 错误累积

```cpp
class JoinMeetContext {
private:
    vector<ErrorInfo> errorStack;

public:
    void PushError(const string& msg, const SourceLocation& loc) {
        errorStack.push_back({msg, loc});
    }

    void PopError() {
        if (!errorStack.empty()) {
            errorStack.pop_back();
        }
    }

    void AddFinalErrMsgs(Diagnostic& diag) {
        for (auto& err : errorStack) {
            diag.AddNote(err.message, err.location);
        }
    }
};
```

### 9.2 归责追踪

```cpp
Ptr<Ty> BatchJoinWithBlame(
    const set<Ptr<Ty>>& types,
    const map<Ptr<Ty>, BlameInfo>& blames
) {
    auto result = BatchJoin(types);

    if (result->IsAny() && types.size() > 1) {
        // 无法找到具体的公共类型，报告详细信息
        stringstream ss;
        ss << "Cannot find common type for:\n";
        for (auto& ty : types) {
            ss << "  - " << TypeToString(ty);
            if (blames.count(ty)) {
                ss << " (from " << blames.at(ty).loc << ")";
            }
            ss << "\n";
        }
        ReportWarning(ss.str());
    }

    return result;
}
```

## 10. 特殊情况处理

### 10.1 Nothing 类型

```cpp
// Nothing 是所有类型的子类型
// Join 时 Nothing 被任何其他类型覆盖
// Meet 时 Nothing 覆盖任何其他类型

Ptr<Ty> BatchJoin(const set<Ptr<Ty>>& types) {
    // 过滤掉 Nothing
    set<Ptr<Ty>> nonNothing;
    for (auto& ty : types) {
        if (!ty->IsNothing()) {
            nonNothing.insert(ty);
        }
    }

    if (nonNothing.empty()) {
        return NothingTy;
    }

    // 继续处理非 Nothing 类型...
}

Ptr<Ty> BatchMeet(const set<Ptr<Ty>>& types) {
    // 如果有 Nothing，结果就是 Nothing
    for (auto& ty : types) {
        if (ty->IsNothing()) {
            return NothingTy;
        }
    }

    // 继续处理...
}
```

### 10.2 Any 类型

```cpp
// Any 是所有类型的父类型
// Join 时 Any 覆盖任何其他类型
// Meet 时 Any 被任何其他类型覆盖

Ptr<Ty> BatchJoin(const set<Ptr<Ty>>& types) {
    // 如果有 Any，结果就是 Any
    for (auto& ty : types) {
        if (ty->IsAny()) {
            return AnyTy;
        }
    }
    // ...
}

Ptr<Ty> BatchMeet(const set<Ptr<Ty>>& types) {
    // 过滤掉 Any
    set<Ptr<Ty>> nonAny;
    for (auto& ty : types) {
        if (!ty->IsAny()) {
            nonAny.insert(ty);
        }
    }

    if (nonAny.empty()) {
        return AnyTy;
    }
    // ...
}
```

### 10.3 Option 类型

```cpp
// Option<T> 等价于 T | None
// Join(Option<T>, Option<U>) = Option<Join(T, U)>

Ptr<Ty> JoinOptionTypes(const set<Ptr<Ty>>& types) {
    bool hasNone = false;
    set<Ptr<Ty>> innerTypes;

    for (auto& ty : types) {
        if (ty->IsOption()) {
            innerTypes.insert(ty->AsOptionTy().wrappedTy);
            hasNone = true;
        } else if (ty->IsNone()) {
            hasNone = true;
        } else {
            innerTypes.insert(ty);
        }
    }

    auto innerJoin = BatchJoin(innerTypes);

    if (hasNone) {
        return MakeOptionTy(innerJoin);
    } else {
        return innerJoin;
    }
}
```

## 11. 示例

### 示例 1：简单 Join

```cangjie
let x = if (cond) 1 else 2  // Join(Int64, Int64) = Int64
```

### 示例 2：类层次 Join

```cangjie
open class Animal {}
class Dog <: Animal {}
class Cat <: Animal {}

let pet = if (cond) Dog() else Cat()  // Join(Dog, Cat) = Animal
```

### 示例 3：函数类型 Join

```cangjie
let f: (Int64) -> String = ...
let g: (Int64) -> Object = ...

// Join(f, g) = (Int64) -> Object
// 参数：Meet(Int64, Int64) = Int64
// 返回：Join(String, Object) = Object
```

### 示例 4：无公共父类型

```cangjie
class A {}
class B {}

let x = if (cond) A() else B()  // Join(A, B) = Any
```

## 12. 算法复杂度

| 操作 | 时间复杂度 | 说明 |
|------|-----------|------|
| 展平类型 | O(n) | n 为类型数量 |
| 过滤覆盖 | O(n² × S) | S 为子类型检查代价 |
| 查找公共父类型 | O(n × H) | H 为继承层次深度 |
| 函数/元组 Join | O(n × m) | m 为参数/元素数量 |

## 13. 性能优化

1. **早期退出**：检测到 Any (Join) 或 Nothing (Meet) 时立即返回
2. **覆盖过滤**：先移除被覆盖的类型减少后续计算
3. **缓存继承层次**：`GetAllSuperTypes` 结果缓存
4. **类型去重**：使用类型指针相等避免结构比较
