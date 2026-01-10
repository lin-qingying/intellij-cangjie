# 子类型检查规则

本文档描述仓颉语言编译器中的子类型检查规则，包括 `IsSubtype` 算法、型变规则和特殊类型处理。

## 1. 子类型关系概述

子类型关系 `<:` 满足以下基本性质：

- **自反性**：`T <: T`
- **传递性**：若 `T1 <: T2` 且 `T2 <: T3`，则 `T1 <: T3`
- **反对称性**：若 `T1 <: T2` 且 `T2 <: T1`，则 `T1 = T2`

## 2. IsSubtype 核心算法

`TypeManager::IsSubtype` 是子类型检查的核心入口：

```cpp
bool TypeManager::IsSubtype(
    const Ptr<Ty>& leaf,        // 待检查的类型 (子类型候选)
    const Ptr<Ty>& root,        // 目标类型 (父类型候选)
    bool implicitBoxed,         // 是否允许隐式装箱
    bool allowOptionBox         // 是否允许 Option 装箱
) {
    // 1. 快速路径：相同类型
    if (IsTyEqual(leaf, root)) return true;

    // 2. 特殊类型处理
    if (leaf->IsNothing()) return true;           // Nothing 是所有类型的子类型
    if (root->IsAny() && implicitBoxed) return true;  // 所有类型都是 Any 的子类型

    // 3. 类型变量处理
    if (leaf->IsGeneric() || root->IsGeneric()) {
        return HandleGenericSubtype(leaf, root, implicitBoxed);
    }

    // 4. 按类型种类分派
    switch (root->kind) {
        case TYPE_FUNC:
            return IsFuncSubtype(leaf, root);
        case TYPE_TUPLE:
            return IsTupleSubtype(leaf, root, implicitBoxed);
        case TYPE_ARRAY:
            return IsArraySubtype(leaf, root);
        case TYPE_VARRAY:
            return IsVArraySubtype(leaf, root);
        case TYPE_POINTER:
            return IsPointerSubtype(leaf, root);
        case TYPE_CLASS:
        case TYPE_INTERFACE:
            return IsClassSubtype(leaf, root, implicitBoxed);
        case TYPE_STRUCT:
        case TYPE_ENUM:
            return IsNominalSubtype(leaf, root);
        default:
            return IsPrimitiveSubtype(leaf, root);
    }
}
```

## 3. 函数类型子类型规则

函数类型遵循**参数逆变、返回值协变**的规则：

```cpp
bool TypeManager::IsFuncSubtype(const Ptr<Ty>& leaf, const Ptr<Ty>& root) {
    auto& leafFunc = leaf->AsFuncTy();
    auto& rootFunc = root->AsFuncTy();

    // 参数数量必须匹配
    if (leafFunc.paramTys.size() != rootFunc.paramTys.size()) {
        return false;
    }

    // 参数类型：逆变 (contravariant)
    // root 的参数必须是 leaf 参数的子类型
    for (size_t i = 0; i < leafFunc.paramTys.size(); i++) {
        if (!IsSubtype(rootFunc.paramTys[i], leafFunc.paramTys[i], false)) {
            return false;
        }
    }

    // 返回类型：协变 (covariant)
    // leaf 的返回类型必须是 root 返回类型的子类型
    return IsSubtype(leafFunc.retTy, rootFunc.retTy, false);
}
```

### 函数子类型示例

```
(Int64) -> String  <:  (Int64) -> String     ✓ (相同)
(Int64) -> String  <:  (Int64) -> Object     ✓ (返回值协变)
(Object) -> String <:  (Int64) -> String     ✓ (参数逆变)
(Object) -> String <:  (Int64) -> Object     ✓ (两者都满足)
(String) -> Int64  <:  (Object) -> Int64     ✗ (参数不满足逆变)
```

## 4. 元组类型子类型规则

元组类型逐元素检查，使用**严格子类型**（禁用隐式装箱）：

```cpp
bool TypeManager::IsTupleSubtype(
    const Ptr<Ty>& leaf,
    const Ptr<Ty>& root,
    bool implicitBoxed
) {
    auto& leafTuple = leaf->AsTupleTy();
    auto& rootTuple = root->AsTupleTy();

    // 元素数量必须相同
    if (leafTuple.elementTys.size() != rootTuple.elementTys.size()) {
        return false;
    }

    // 逐元素检查：使用 implicitBoxed = false 确保严格性
    for (size_t i = 0; i < leafTuple.elementTys.size(); i++) {
        if (!IsSubtype(leafTuple.elementTys[i], rootTuple.elementTys[i], false)) {
            return false;
        }
    }

    return true;
}
```

### 元组子类型示例

```
(Int64, String)     <:  (Int64, String)      ✓
(Int64, String)     <:  (Int64, Object)      ✓ (元素协变)
(Int64, String)     <:  (Object, Object)     ✓
(Int64, String)     <:  (Int64,)             ✗ (元素数量不同)
(Int64,)            <:  (Int64, String)      ✗ (元素数量不同)
```

## 5. 数组类型子类型规则

数组类型的元素类型是**不变的 (invariant)**：

```cpp
bool TypeManager::IsArraySubtype(const Ptr<Ty>& leaf, const Ptr<Ty>& root) {
    if (!leaf->IsArray()) return false;

    auto& leafArray = leaf->AsArrayTy();
    auto& rootArray = root->AsArrayTy();

    // 元素类型必须完全相等（不变性）
    return IsTyEqual(leafArray.elementTy, rootArray.elementTy);
}

bool TypeManager::IsVArraySubtype(const Ptr<Ty>& leaf, const Ptr<Ty>& root) {
    if (!leaf->IsVArray()) return false;

    auto& leafArray = leaf->AsVArrayTy();
    auto& rootArray = root->AsVArrayTy();

    // 元素类型必须完全相等
    if (!IsTyEqual(leafArray.elementTy, rootArray.elementTy)) {
        return false;
    }

    // 大小约束检查（如果有）
    if (rootArray.size.has_value()) {
        if (!leafArray.size.has_value()) return false;
        if (leafArray.size.value() != rootArray.size.value()) return false;
    }

    return true;
}
```

### 为什么数组不变？

数组的不变性防止以下类型不安全情况：

```cangjie
// 假设 Array<String> <: Array<Object>（协变）
var strings: Array<String> = ["hello", "world"]
var objects: Array<Object> = strings  // 如果允许...
objects[0] = 42  // 类型系统允许，因为 42 是 Object
// 但 strings[0] 现在是 Int64，违反了类型安全！
```

## 6. 指针类型子类型规则

指针类型同样是**不变的**：

```cpp
bool TypeManager::IsPointerSubtype(const Ptr<Ty>& leaf, const Ptr<Ty>& root) {
    if (!leaf->IsPointer()) return false;

    auto& leafPtr = leaf->AsPointerTy();
    auto& rootPtr = root->AsPointerTy();

    // 指向类型必须完全相等
    return IsTyEqual(leafPtr.pointeeTy, rootPtr.pointeeTy);
}
```

## 7. 类和接口子类型规则

类和接口使用**名义子类型**，基于继承层次和扩展声明：

```cpp
bool TypeManager::IsClassSubtype(
    const Ptr<Ty>& leaf,
    const Ptr<Ty>& root,
    bool implicitBoxed
) {
    // 1. 检查直接继承
    if (IsDirectSupertype(leaf, root)) {
        return true;
    }

    // 2. 检查扩展声明 (extend)
    if (root->IsInterface()) {
        if (HasExtendDecl(leaf, root)) {
            return true;
        }
    }

    // 3. 检查传递继承
    for (auto& superTy : GetAllSuperTys(leaf)) {
        if (IsTyEqual(superTy, root)) {
            return true;
        }
    }

    // 4. 隐式装箱到 Any
    if (implicitBoxed && root->IsAny()) {
        return true;
    }

    return false;
}
```

### 获取所有父类型

```cpp
vector<Ptr<Ty>> TypeManager::GetAllSuperTys(const Ptr<Ty>& ty) {
    // 检查缓存
    if (tyToSuperTysMap.count(ty)) {
        return tyToSuperTysMap[ty];
    }

    vector<Ptr<Ty>> result;
    set<Ptr<Ty>> visited;
    queue<Ptr<Ty>> worklist;

    // 初始化：添加直接父类型
    for (auto& superTy : GetNominalSuperTy(ty)) {
        worklist.push(superTy);
    }

    // BFS 遍历继承层次
    while (!worklist.empty()) {
        auto current = worklist.front();
        worklist.pop();

        if (visited.count(current)) continue;
        visited.insert(current);
        result.push_back(current);

        for (auto& superTy : GetNominalSuperTy(current)) {
            worklist.push(superTy);
        }
    }

    // 缓存结果
    tyToSuperTysMap[ty] = result;
    return result;
}
```

## 8. 结构体和枚举子类型规则

结构体和枚举使用**严格名义子类型**：

```cpp
bool TypeManager::IsNominalSubtype(const Ptr<Ty>& leaf, const Ptr<Ty>& root) {
    // 必须是同一声明
    if (leaf->decl != root->decl) {
        return false;
    }

    // 类型参数必须相等
    return IsTyEqual(leaf, root);
}
```

结构体和枚举**不支持继承**，只有相同类型之间才有子类型关系。

## 9. 原始类型子类型规则

原始类型之间的子类型关系主要涉及**数值提升**：

```cpp
bool TypeManager::IsPrimitiveSubtype(const Ptr<Ty>& leaf, const Ptr<Ty>& root) {
    // 相同类型
    if (leaf->kind == root->kind) return true;

    // 数值类型提升
    if (leaf->IsInteger() && root->IsInteger()) {
        return IsIntegerPromotion(leaf->kind, root->kind);
    }

    if (leaf->IsFloating() && root->IsFloating()) {
        return IsFloatPromotion(leaf->kind, root->kind);
    }

    // 整数到浮点的隐式转换
    if (leaf->IsInteger() && root->IsFloating()) {
        return true;  // 允许 Int -> Float
    }

    return false;
}
```

### 整数提升规则

```
Int8  <:  Int16  <:  Int32  <:  Int64
UInt8 <:  UInt16 <:  UInt32 <:  UInt64

IntNative  <:  Int64  (在 64 位平台)
UIntNative <:  UInt64 (在 64 位平台)
```

### 浮点提升规则

```
Float16 <:  Float32  <:  Float64
```

### 整数到浮点

```
Int8/Int16/Int32/Int64  <:  Float64  (可能有精度损失)
```

## 10. 泛型类型子类型规则

泛型类型的子类型检查需要考虑类型参数的约束：

```cpp
bool TypeManager::HandleGenericSubtype(
    const Ptr<Ty>& leaf,
    const Ptr<Ty>& root,
    bool implicitBoxed
) {
    // 情况 1：两者都是泛型类型参数
    if (leaf->IsGeneric() && root->IsGeneric()) {
        auto& leafGen = leaf->AsGenericsTy();
        auto& rootGen = root->AsGenericsTy();

        // 同一类型参数
        if (leafGen.index == rootGen.index &&
            leafGen.declaredIn == rootGen.declaredIn) {
            return true;
        }

        // 检查 leaf 的上界是否满足 root
        for (auto& ub : leafGen.upperBounds) {
            if (IsSubtype(ub, root, implicitBoxed)) {
                return true;
            }
        }
        return false;
    }

    // 情况 2：leaf 是泛型，root 是具体类型
    if (leaf->IsGeneric()) {
        auto& leafGen = leaf->AsGenericsTy();
        // 检查是否有上界满足 root
        for (auto& ub : leafGen.upperBounds) {
            if (IsSubtype(ub, root, implicitBoxed)) {
                return true;
            }
        }
        return false;
    }

    // 情况 3：leaf 是具体类型，root 是泛型
    if (root->IsGeneric()) {
        // 具体类型不能是泛型类型参数的子类型
        // （除非通过约束收集）
        return false;
    }

    return false;
}
```

## 11. 扩展类型子类型规则

扩展 (extend) 声明为现有类型添加接口实现：

```cpp
bool TypeManager::HasExtendDecl(const Ptr<Ty>& ty, const Ptr<Ty>& interfaceTy) {
    // 查找所有适用的扩展声明
    auto extends = GetAllExtendInterfaceTy(ty);

    for (auto& extendInterface : extends) {
        if (IsTyEqual(extendInterface, interfaceTy)) {
            return true;
        }

        // 检查扩展接口的父接口
        if (IsSubtype(extendInterface, interfaceTy, false)) {
            return true;
        }
    }

    return false;
}

vector<Ptr<Ty>> TypeManager::GetAllExtendInterfaceTy(const Ptr<Ty>& ty) {
    vector<Ptr<Ty>> result;

    // 1. 查找直接扩展
    if (declToExtendMap.count(ty->decl)) {
        for (auto& extendDecl : declToExtendMap[ty->decl]) {
            result.push_back(extendDecl->interfaceTy);
        }
    }

    // 2. 内置类型的扩展
    if (builtinTyToExtendMap.count(ty->kind)) {
        for (auto& extendDecl : builtinTyToExtendMap[ty->kind]) {
            result.push_back(extendDecl->interfaceTy);
        }
    }

    return result;
}
```

### 扩展示例

```cangjie
interface Printable {
    func print(): String
}

struct Point {
    var x: Int64
    var y: Int64
}

extend Point <: Printable {
    func print(): String {
        return "(${x}, ${y})"
    }
}

// 现在 Point <: Printable
```

## 12. 子类型缓存

为提高性能，编译器缓存子类型检查结果：

```cpp
struct SubtypeCacheKey {
    Ptr<Ty> leaf;
    Ptr<Ty> root;
    bool implicitBoxed;
    bool allowOptionBox;

    bool operator==(const SubtypeCacheKey& other) const;
};

struct SubtypeCacheHash {
    size_t operator()(const SubtypeCacheKey& key) const;
};

class TypeManager {
private:
    unordered_map<SubtypeCacheKey, bool, SubtypeCacheHash> subtypeCache;

public:
    bool IsSubtype(...) {
        SubtypeCacheKey key{leaf, root, implicitBoxed, allowOptionBox};

        // 查找缓存
        auto it = subtypeCache.find(key);
        if (it != subtypeCache.end()) {
            return it->second;
        }

        // 计算结果
        bool result = ComputeIsSubtype(leaf, root, implicitBoxed, allowOptionBox);

        // 存入缓存
        subtypeCache[key] = result;
        return result;
    }
};
```

## 13. 子类型判断决策树

完整的子类型判断流程：

```
IsSubtype(Leaf, Root)?
│
├─ Leaf == Root → true
│
├─ Leaf is Nothing → true
│
├─ Root is Any (with implicitBoxed) → true
│
├─ Leaf or Root is Generic
│   └─ HandleGenericSubtype()
│
├─ Root is Function
│   └─ IsFuncSubtype()
│       ├─ 参数数量相同?
│       ├─ 参数逆变检查
│       └─ 返回值协变检查
│
├─ Root is Tuple
│   └─ IsTupleSubtype()
│       ├─ 元素数量相同?
│       └─ 逐元素子类型检查 (implicitBoxed=false)
│
├─ Root is Array/VArray
│   └─ IsArraySubtype()
│       └─ 元素类型相等? (不变)
│
├─ Root is Pointer
│   └─ IsPointerSubtype()
│       └─ 指向类型相等? (不变)
│
├─ Root is Class/Interface
│   └─ IsClassSubtype()
│       ├─ 直接继承检查
│       ├─ 扩展声明检查
│       └─ 传递继承检查
│
├─ Root is Struct/Enum
│   └─ IsNominalSubtype()
│       └─ 类型相等检查
│
└─ Root is Primitive
    └─ IsPrimitiveSubtype()
        ├─ 相同类型?
        ├─ 整数提升?
        ├─ 浮点提升?
        └─ 整数到浮点?
```

## 14. 型变规则总结

| 类型构造器 | 型变规则 | 说明 |
|-----------|---------|------|
| 函数参数 | 逆变 (contravariant) | `(S) -> T` 中 `S` 位置 |
| 函数返回值 | 协变 (covariant) | `(S) -> T` 中 `T` 位置 |
| 元组元素 | 协变 | `(T1, T2, ...)` 中各元素 |
| 数组元素 | 不变 (invariant) | `Array<T>` 中 `T` |
| 可变数组元素 | 不变 | `VArray<T>` 中 `T` |
| 指针指向 | 不变 | `Pointer<T>` 中 `T` |
| 类类型参数 | 不变 | `Class<T>` 中 `T` |
| Option 内部 | 协变 | `Option<T>` 中 `T` |

## 15. 特殊规则

### Nothing 类型

`Nothing` 是底类型，是所有类型的子类型：

```
Nothing <: T  (对于任意类型 T)
```

用途：表示永不返回的表达式（如 `throw`、`return`、无限循环）

### Any 类型

`Any` 是顶类型，所有类型都是它的子类型（需要隐式装箱）：

```
T <: Any  (对于任意类型 T，当 implicitBoxed = true)
```

### Unit 类型

`Unit` 是单元类型，只与自身有子类型关系：

```
Unit <: Unit  (仅自反性)
```
