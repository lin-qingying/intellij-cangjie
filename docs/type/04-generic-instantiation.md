# 泛型类型实例化

本文档描述仓颉语言编译器中的泛型类型实例化机制，包括实例化管理、替换策略和缓存机制。

## 1. 泛型实例化概述

泛型实例化是将泛型类型参数替换为具体类型的过程：

```cangjie
class Box<T> {
    var value: T
}

// 实例化 Box<Int64>
let intBox = Box<Int64>(42)
```

编译器需要：
1. 验证类型参数满足约束
2. 创建实例化后的类型和成员
3. 缓存实例化结果避免重复计算

## 2. 替换包 (SubstPack)

### 2.1 两层替换结构

```cpp
struct SubstPack {
    TypeSubst u2i;    // Universal to Instance: 通用 → 实例
    TypeSubst inst;   // Instance to Concrete: 实例 → 具体类型

    // 应用替换
    Ptr<Ty> Apply(const Ptr<Ty>& ty) const;
};
```

### 2.2 两层替换的必要性

考虑嵌套泛型的情况：

```cangjie
class Outer<T> {
    class Inner<U> {
        func combine(t: T, u: U): (T, U) { ... }
    }
}

// 使用 Outer<Int64>.Inner<String>
```

替换过程：
1. **第一层 (u2i)**：`T → T'`, `U → U'`（创建实例级类型变量）
2. **第二层 (inst)**：`T' → Int64`, `U' → String`（应用具体类型）

```cpp
Ptr<Ty> SubstPack::Apply(const Ptr<Ty>& ty) const {
    // 1. 先应用 u2i 替换
    auto intermediate = ApplySubst(ty, u2i);

    // 2. 再应用 inst 替换
    return ApplySubst(intermediate, inst);
}
```

### 2.3 防止循环替换

```cpp
Ptr<Ty> ApplySubst(const Ptr<Ty>& ty, const TypeSubst& subst) {
    if (!ty->IsGeneric()) {
        return InstantiateComposite(ty, subst);
    }

    auto& gen = ty->AsGenericsTy();

    // 查找替换
    auto it = subst.find(gen);
    if (it == subst.end()) {
        return ty;  // 无替换
    }

    auto replacement = it->second;

    // 防止循环：暂时移除当前映射
    TypeSubst tempSubst = subst;
    tempSubst.erase(gen);

    // 递归应用（使用修改后的替换表）
    return ApplySubst(replacement, tempSubst);
}
```

## 3. 类型实例化器

### 3.1 TyInstantiator 类

```cpp
class TyInstantiator {
private:
    TypeManager& tm;
    const SubstPack& substPack;
    set<Ptr<Ty>> visited;  // 防止无限递归

public:
    TyInstantiator(TypeManager& tm, const SubstPack& sp)
        : tm(tm), substPack(sp) {}

    Ptr<Ty> Instantiate(const Ptr<Ty>& ty);

private:
    Ptr<Ty> InstantiateFunc(const Ptr<FuncTy>& func);
    Ptr<Ty> InstantiateTuple(const Ptr<TupleTy>& tuple);
    Ptr<Ty> InstantiateClass(const Ptr<ClassTy>& cls);
    Ptr<Ty> InstantiateArray(const Ptr<ArrayTy>& arr);
    // ...
};
```

### 3.2 实例化分派

```cpp
Ptr<Ty> TyInstantiator::Instantiate(const Ptr<Ty>& ty) {
    // 防止无限递归
    if (visited.count(ty)) {
        return ty;
    }
    visited.insert(ty);

    switch (ty->kind) {
        case TYPE_GENERIC:
            return InstantiateGeneric(ty->AsGenericsTy());

        case TYPE_FUNC:
            return InstantiateFunc(ty->AsFuncTy());

        case TYPE_TUPLE:
            return InstantiateTuple(ty->AsTupleTy());

        case TYPE_CLASS:
        case TYPE_STRUCT:
        case TYPE_INTERFACE:
        case TYPE_ENUM:
            return InstantiateClass(ty->AsClassLikeTy());

        case TYPE_ARRAY:
            return InstantiateArray(ty->AsArrayTy());

        case TYPE_VARRAY:
            return InstantiateVArray(ty->AsVArrayTy());

        case TYPE_POINTER:
            return InstantiatePointer(ty->AsPointerTy());

        default:
            // 原始类型不需要实例化
            return ty;
    }
}
```

### 3.3 泛型类型实例化

```cpp
Ptr<Ty> TyInstantiator::InstantiateGeneric(const Ptr<GenericsTy>& gen) {
    // 1. 尝试 u2i 替换
    auto it = substPack.u2i.find(gen);
    if (it != substPack.u2i.end()) {
        auto intermediate = it->second;

        // 如果中间结果也是泛型，继续查找 inst
        if (intermediate->IsGeneric()) {
            auto it2 = substPack.inst.find(intermediate->AsGenericsTy());
            if (it2 != substPack.inst.end()) {
                return Instantiate(it2->second);
            }
        }
        return Instantiate(intermediate);
    }

    // 2. 尝试直接 inst 替换
    auto it2 = substPack.inst.find(gen);
    if (it2 != substPack.inst.end()) {
        return Instantiate(it2->second);
    }

    // 3. 无替换，保持原样
    return gen;
}
```

### 3.4 函数类型实例化

```cpp
Ptr<Ty> TyInstantiator::InstantiateFunc(const Ptr<FuncTy>& func) {
    vector<Ptr<Ty>> newParams;
    for (auto& param : func->paramTys) {
        newParams.push_back(Instantiate(param));
    }

    auto newRet = Instantiate(func->retTy);

    // 检查是否有变化
    bool changed = !IsTyEqual(newRet, func->retTy);
    for (size_t i = 0; i < newParams.size() && !changed; i++) {
        if (!IsTyEqual(newParams[i], func->paramTys[i])) {
            changed = true;
        }
    }

    if (!changed) {
        return func;  // 无变化，返回原类型
    }

    return tm.GetFuncTy(newParams, newRet, func->isVariadic);
}
```

### 3.5 类类型实例化

```cpp
Ptr<Ty> TyInstantiator::InstantiateClass(const Ptr<ClassLikeTy>& cls) {
    vector<Ptr<Ty>> newTypeArgs;
    for (auto& arg : cls->typeArgs) {
        newTypeArgs.push_back(Instantiate(arg));
    }

    // 检查是否有变化
    bool changed = false;
    for (size_t i = 0; i < newTypeArgs.size(); i++) {
        if (!IsTyEqual(newTypeArgs[i], cls->typeArgs[i])) {
            changed = true;
            break;
        }
    }

    if (!changed) {
        return cls;
    }

    // 创建新的实例化类型
    return tm.GetClassTy(cls->decl, newTypeArgs);
}
```

## 4. 泛型实例化管理器

### 4.1 GenericInstantiationManager

```cpp
class GenericInstantiationManager {
private:
    // 泛型声明 → 所有实例化变体
    using Generic2InsMap = map<Ptr<Decl>, vector<Ptr<Decl>>>;
    Generic2InsMap instantiations;

    // (泛型声明, 类型替换) → 实例化声明
    using GenericInfo = pair<Ptr<Decl>, TypeSubst>;
    map<GenericInfo, Ptr<Decl>> declInstantiationByTypeMap;

    // 成员索引映射
    map<Ptr<Decl>, map<string, int>> membersIndexMap;

    // 抽象函数实现映射
    map<pair<Ptr<Ty>, Ptr<FuncDecl>>, Ptr<FuncDecl>> abstractFuncToDeclMap;

public:
    // 实例化泛型声明
    Ptr<Decl> InstantiateGenericDecl(
        Ptr<Decl> genericDecl,
        const vector<Ptr<Ty>>& typeArgs
    );

    // 查找已存在的实例化
    Ptr<Decl> GetInstantiatedDecl(
        Ptr<Decl> genericDecl,
        const TypeSubst& subst
    );

    // 获取泛型的所有实例化
    vector<Ptr<Decl>> GetAllInstantiations(Ptr<Decl> genericDecl);

    // 重置实例化状态
    void ResetGenericInstantiationStage();
};
```

### 4.2 声明实例化

```cpp
Ptr<Decl> GenericInstantiationManager::InstantiateGenericDecl(
    Ptr<Decl> genericDecl,
    const vector<Ptr<Ty>>& typeArgs
) {
    // 1. 构建类型替换
    TypeSubst subst;
    auto typeParams = genericDecl->GetTypeParameters();
    for (size_t i = 0; i < typeParams.size(); i++) {
        subst[typeParams[i]->ty] = typeArgs[i];
    }

    // 2. 检查缓存
    GenericInfo key{genericDecl, subst};
    auto it = declInstantiationByTypeMap.find(key);
    if (it != declInstantiationByTypeMap.end()) {
        return it->second;  // 返回缓存的实例化
    }

    // 3. 创建新的实例化声明
    auto instantiated = CloneAndInstantiate(genericDecl, subst);

    // 4. 存入缓存
    declInstantiationByTypeMap[key] = instantiated;
    instantiations[genericDecl].push_back(instantiated);

    // 5. 实例化成员
    InstantiateMembers(instantiated, subst);

    return instantiated;
}
```

### 4.3 成员实例化

```cpp
void GenericInstantiationManager::InstantiateMembers(
    Ptr<Decl> instantiated,
    const TypeSubst& subst
) {
    SubstPack substPack;
    substPack.inst = subst;

    TyInstantiator instantiator(tm, substPack);

    // 实例化字段
    for (auto& field : instantiated->GetFields()) {
        field->ty = instantiator.Instantiate(field->ty);
    }

    // 实例化方法
    for (auto& method : instantiated->GetMethods()) {
        auto& funcTy = method->ty->AsFuncTy();

        vector<Ptr<Ty>> newParams;
        for (auto& param : funcTy.paramTys) {
            newParams.push_back(instantiator.Instantiate(param));
        }

        auto newRet = instantiator.Instantiate(funcTy.retTy);
        method->ty = tm.GetFuncTy(newParams, newRet);
    }
}
```

## 5. 类型参数约束检查

### 5.1 约束验证

```cpp
bool GenericInstantiationManager::ValidateTypeArguments(
    Ptr<Decl> genericDecl,
    const vector<Ptr<Ty>>& typeArgs
) {
    auto typeParams = genericDecl->GetTypeParameters();

    if (typeArgs.size() != typeParams.size()) {
        ReportError("Wrong number of type arguments");
        return false;
    }

    for (size_t i = 0; i < typeParams.size(); i++) {
        auto& param = typeParams[i];
        auto& arg = typeArgs[i];

        // 检查上界约束
        for (auto& upperBound : param->upperBounds) {
            // 需要先实例化上界（可能引用其他类型参数）
            auto instantiatedBound = InstantiateBound(upperBound, typeArgs);

            if (!tm.IsSubtype(arg, instantiatedBound, false)) {
                ReportError("Type argument does not satisfy constraint");
                return false;
            }
        }
    }

    return true;
}
```

### 5.2 上界约束实例化

```cpp
Ptr<Ty> GenericInstantiationManager::InstantiateBound(
    const Ptr<Ty>& bound,
    const vector<Ptr<Ty>>& typeArgs
) {
    // 上界可能引用其他类型参数
    // 例如：class Foo<T, U: Comparable<T>>
    // 当实例化 Foo<Int64, X> 时，U 的上界是 Comparable<Int64>

    TypeSubst subst;
    auto typeParams = currentGenericDecl->GetTypeParameters();
    for (size_t i = 0; i < typeParams.size(); i++) {
        subst[typeParams[i]->ty] = typeArgs[i];
    }

    SubstPack substPack;
    substPack.inst = subst;

    TyInstantiator instantiator(tm, substPack);
    return instantiator.Instantiate(bound);
}
```

## 6. 实例化上下文

### 6.1 InstCtxScope

```cpp
class InstCtxScope {
private:
    GenericInstantiationManager& mgr;
    InstCtx previousCtx;

public:
    InstCtxScope(GenericInstantiationManager& mgr, const InstCtx& ctx)
        : mgr(mgr), previousCtx(mgr.currentCtx)
    {
        mgr.currentCtx = ctx;
    }

    ~InstCtxScope() {
        mgr.currentCtx = previousCtx;
    }
};
```

### 6.2 上下文感知的类型操作

```cpp
struct InstCtx {
    Ptr<Decl> triggerDecl;        // 触发实例化的声明
    TypeSubst currentSubst;        // 当前替换映射
    int depth;                     // 嵌套深度

    bool IsInGenericContext() const {
        return !currentSubst.empty();
    }
};
```

## 7. 缓存策略

### 7.1 类型级缓存

```cpp
// TypeManager 中的类型缓存
class TypeManager {
private:
    // 类型去重缓存
    unordered_set<TypePointer, TypeHash> typeCache;

    // 实例化状态缓存
    map<pair<Ptr<Decl>, TypeSubst>, InstantiationStatus> declInstantiationStatus;

    enum class InstantiationStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    };
};
```

### 7.2 防止递归实例化

```cpp
Ptr<Decl> GenericInstantiationManager::InstantiateGenericDecl(...) {
    GenericInfo key{genericDecl, subst};

    // 检查是否正在实例化（递归检测）
    auto status = tm.declInstantiationStatus[key];
    if (status == InstantiationStatus::IN_PROGRESS) {
        // 递归实例化 - 返回占位符
        return CreatePlaceholder(genericDecl, subst);
    }

    // 标记为进行中
    tm.declInstantiationStatus[key] = InstantiationStatus::IN_PROGRESS;

    try {
        auto result = DoInstantiate(genericDecl, subst);
        tm.declInstantiationStatus[key] = InstantiationStatus::COMPLETED;
        return result;
    } catch (...) {
        tm.declInstantiationStatus[key] = InstantiationStatus::FAILED;
        throw;
    }
}
```

## 8. 部分实例化

### 8.1 部分类型参数应用

```cpp
// 支持部分实例化：只提供部分类型参数
Ptr<Ty> TypeManager::PartialInstantiate(
    const Ptr<Ty>& ty,
    const TypeSubst& partialSubst
) {
    SubstPack substPack;
    substPack.inst = partialSubst;

    TyInstantiator instantiator(*this, substPack);
    return instantiator.Instantiate(ty);
}
```

### 8.2 示例

```cangjie
class Map<K, V> { ... }

// 部分实例化：Map<String, _> 仍然是泛型
type StringMap<V> = Map<String, V>
```

## 9. Walker 模式

### 9.1 实例化 Walker

```cpp
class InstantiationWalker {
private:
    GenericInstantiationManager& mgr;
    int walkerId;

public:
    void WalkPackage(Package* pkg) {
        for (auto& decl : pkg->GetDecls()) {
            WalkDecl(decl);
        }
    }

    void WalkDecl(Ptr<Decl> decl) {
        if (decl->IsGeneric() && decl->HasPendingInstantiations()) {
            ProcessPendingInstantiations(decl);
        }

        // 递归遍历嵌套声明
        for (auto& nested : decl->GetNestedDecls()) {
            WalkDecl(nested);
        }
    }

    void ProcessPendingInstantiations(Ptr<Decl> decl) {
        auto pending = mgr.GetPendingInstantiations(decl);
        for (auto& typeArgs : pending) {
            mgr.InstantiateGenericDecl(decl, typeArgs);
        }
    }
};
```

### 9.2 重排 Walker

```cpp
class RearrangementWalker {
    // 处理实例化后的成员重排
    void RearrangeMembers(Ptr<Decl> instantiated) {
        // 确保成员顺序与原始声明一致
        auto original = GetOriginalDecl(instantiated);
        auto& originalIndex = mgr.membersIndexMap[original];

        sort(instantiated->members.begin(), instantiated->members.end(),
             [&](auto& a, auto& b) {
                 return originalIndex[a->name] < originalIndex[b->name];
             });
    }
};
```

## 10. 多类型替换

### 10.1 MultiTypeSubst

当一个类型变量可能有多个选择时：

```cpp
using MultiTypeSubst = map<Ptr<GenericsTy>, set<Ptr<Ty>>>;

// 例如：接口约束可能有多个实现
// T: Printable 可能是 {String, Int64, CustomClass, ...}
```

### 10.2 应用多类型替换

```cpp
vector<TypeSubst> ExpandMultiSubst(const MultiTypeSubst& multi) {
    vector<TypeSubst> results;
    results.push_back({});  // 初始空替换

    for (auto& [tyVar, options] : multi) {
        vector<TypeSubst> newResults;

        for (auto& existing : results) {
            for (auto& option : options) {
                TypeSubst extended = existing;
                extended[tyVar] = option;
                newResults.push_back(extended);
            }
        }

        results = newResults;
    }

    return results;
}
```

## 11. 实例化流程总结

```
泛型实例化流程
│
├─ 1. 解析类型参数
│     ├─ 显式提供：Foo<Int64, String>
│     └─ 推导得到：从使用上下文推导
│
├─ 2. 验证约束
│     ├─ 参数数量检查
│     ├─ 上界约束检查
│     └─ 实例化上界（处理参数间依赖）
│
├─ 3. 查找缓存
│     ├─ 已存在 → 直接返回
│     └─ 不存在 → 继续实例化
│
├─ 4. 创建实例化声明
│     ├─ 克隆原始声明
│     ├─ 构建 SubstPack
│     └─ 应用类型替换
│
├─ 5. 实例化成员
│     ├─ 字段类型实例化
│     ├─ 方法签名实例化
│     └─ 嵌套类型实例化
│
├─ 6. 更新缓存
│     ├─ 存入 declInstantiationByTypeMap
│     └─ 添加到 instantiations 列表
│
└─ 7. 返回实例化结果
```

## 12. 性能优化

1. **类型去重**：相同结构的类型只分配一次
2. **延迟实例化**：只在实际使用时实例化
3. **缓存复用**：相同类型参数的实例化共享结果
4. **递归检测**：防止无限递归实例化
5. **Walker ID**：单次遍历处理所有待实例化项
