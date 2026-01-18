# 仓颉编译器类型推导与约束系统分析

## 文档概述

本文档分析 `external/cangjie_compiler` 中的类型推导和约束求解系统，对比插件实现，为改进插件的类型推导提供参考。

---

## 核心组件

### 1. LocalTypeArgumentSynthesis - 局部类型参数综合

**文件位置**: `external/cangjie_compiler/src/Sema/LocalTypeArgumentSynthesis.{h,cpp}`

#### 核心数据结构

```cpp
// 局部类型参数综合的参数包
struct LocTyArgSynArgPack {
    TyVars tyVarsToSolve;              // 需要求解的类型变量
    std::vector<Ptr<AST::Ty>> argTys;   // 实参类型
    std::vector<Ptr<AST::Ty>> paramTys; // 形参类型
    std::vector<Blame> argBlames;       // 参数归因信息（用于错误报告）
    Ptr<AST::Ty> funcRetTy;            // 函数返回类型
    Ptr<AST::Ty> retTyUB;              // 返回类型的上界
    Blame retBlame;                     // 返回值归因信息
};

// 约束及备忘录
struct ConstraintWithMemo {
    Constraint constraint;              // 约束集合
    MemoForUnifiedTys memo;            // 统一类型的备忘录（避免重复处理）
    bool hasNothingTy;                 // 是否包含 Nothing 类型
    bool hasAnyTy;                     // 是否包含 Any 类型
};
```

#### 核心算法流程

```cpp
std::optional<TypeSubst> SynthesizeTypeArguments(bool allowPartial = false) {
    // 步骤 1: 复制上界约束
    CopyUpperbound();  // 从通用类型变量复制到实例类型变量

    // 步骤 2: 初始化约束（从类型参数的上界开始）
    cms = {{InitConstraints(argPack.tyVarsToSolve), {}, false, false}};

    // 步骤 3: 统一参数类型
    // 按优化顺序检查：非理想类型 > Option嵌套深的 > 理想类型
    std::vector<size_t> orderedIndexes = GetOrderedCheckingIndexes(argPack.argTys);
    for (auto i : orderedIndexes) {
        // 统一 argTy <: paramTy，生成约束
        cms = Unify(cms, {argTy, blame}, {paramTy, blame}).first;
        if (cms.empty()) return {};  // 统一失败
    }

    // 步骤 4: 考虑返回类型约束（如果返回类型包含泛型）
    if (funcRetTy->HasGeneric() && retTyUB) {
        cms = Unify(cms, {funcRetTy, blame}, {retTyUB, blame}).first;
    }

    // 步骤 5: 求解约束系统
    if (auto optSubst = SolveConstraints(allowPartial)) {
        return ResetIdealTypesInSubst(*optSubst);  // 重置理想类型
    }
    return {};
}
```

---

### 2. TyVarConstraintGraph - 类型变量约束图

**文件位置**: `external/cangjie_compiler/src/Sema/TyVarConstraintGraph.h`

#### 设计思想

使用**拓扑排序**解决类型变量之间的依赖关系，确保求解顺序正确。

```cpp
class TyVarConstraintGraph {
    std::map<Ptr<TyVar>, int> indegree;        // 入度（依赖计数）
    std::map<Ptr<TyVar>, TyVars> edges;        // 边（依赖关系）
    TyVars solvedTyVars;                        // 已求解的类型变量
    TyVars usedTyVars;                          // 使用的类型变量

public:
    // 预处理约束图：构建依赖关系
    void PreProcessConstraintGraph(const Constraint& m, const TyVars& mayUsedTyVars);

    // 拓扑排序一次：返回入度为0的类型变量及其约束
    Constraint TopoOnce(const Constraint& m);

    // 应用类型替换后更新图
    void ApplyTypeSubst(const TypeSubst& subst);
};
```

**拓扑排序求解流程**:

```cpp
std::optional<TypeSubst> SolveConstraints(bool allowPartial) {
    // 为每个约束集合独立求解
    for (auto& cm : cms) {
        TypeSubst subst;
        auto graph = TyVarConstraintGraph(cm.constraint, tyVarsToSolve, tyMgr);

        while (true) {
            // 1. 获取入度为0的类型变量（独立变量，可优先求解）
            auto thisM = graph.TopoOnce(cm.constraint);
            if (thisM.empty()) break;  // 所有变量已求解

            // 2. 应用已求解的类型替换到剩余约束
            thisM = ApplyTypeSubstForCS(subst, thisM);

            // 3. 求解当前批次的类型变量
            if (auto optThisSubst = FindSolution(thisM, hasNothingTy, hasAnyTy)) {
                graph.ApplyTypeSubst(*optThisSubst);  // 更新图
                subst.merge(*optThisSubst);           // 合并解
            } else {
                return {};  // 求解失败
            }
        }

        if (allowPartial || !HasUnsolvedTyVars(subst)) {
            substs.insert(subst);
        }
    }

    return GetBestSolution(substs, allowPartial);
}
```

---

### 3. Unify - 类型统一

#### 核心统一规则

```cpp
bool UnifyOne(const Tracked<Ty>& argTTy, const Tracked<Ty>& paramTTy) {
    // 规则 1: 相同类型直接成功
    if (&argTy == &paramTy) return true;

    // 规则 2: Quest 类型（占位符）直接成功
    if (argTy.IsQuest() || paramTy.IsQuest()) return true;

    // 规则 3: 交集类型处理
    if (paramTy.IsIntersection()) {
        // A <: B & C  ⟺  A <: B AND A <: C
        return UnifyParamIntersectionTy(argTTy, paramTTy);
    }

    // 规则 4: 并集类型处理
    if (paramTy.IsUnion()) {
        // A <: B | C  ⟺  A <: B OR A <: C
        return UnifyParamUnionTy(argTTy, paramTTy);
    }

    // 规则 5: 类型变量约束收集
    if (paramTy.IsPlaceholder() || argTy.IsPlaceholder()) {
        return UnifyTyVar(argTTy, paramTTy);
    }

    // 规则 6: Option 类型自动装箱
    if (CountOptionNestedLevel(paramTy) > CountOptionNestedLevel(argTy)) {
        // 自动提升嵌套层级
        return UnifyOne(argTTy, {*paramTy.typeArgs[0], ...});
    }

    // 规则 7: 名义类型（Class/Interface/Enum）
    if (paramTy.IsNominal() && argTy.IsNominal()) {
        return UnifyNominal(argTTy, paramTTy);
    }

    // 规则 8: 函数类型（协变返回，逆变参数）
    if (argTy.IsFunc() && paramTy.IsFunc()) {
        return UnifyFuncTy(argTTy, paramTTy);
    }

    // 规则 9: 元组类型（协变）
    if (argTy.IsTuple() && paramTy.IsTuple()) {
        return UnifyTupleTy(argTTy, paramTTy);
    }

    // 规则 10: 子类型检查（最终兜底）
    return tyMgr.IsSubtype(&argTy, &paramTy);
}
```

#### 名义类型统一（关键：不变性）

```cpp
bool UnifyNominal(const Tracked<Ty>& argTTy, const Tracked<Ty>& paramTTy) {
    // 1. 提升 argTy 到 paramTy 的父类型路径
    auto prTys = Promotion(tyMgr).Promote(argTy, paramTy);
    if (prTys.empty()) return false;

    ConstraintWithMemos res;
    for (auto prTy : prTys) {
        auto currentCms = cms;

        // 2. 对于名义类型，类型参数是**不变的**（Invariant）
        //    I1<A> <: I2<B>  ⟺  A <: B AND B <: A  ⟺  A == B
        for (size_t i = 0; i < prTy->typeArgs.size(); ++i) {
            // 双向统一，确保类型参数完全相等
            currentCms = Unify(currentCms,
                {*prTy->typeArgs[i], ...}, {*paramTy.typeArgs[i], ...}).first;
            currentCms = Unify(currentCms,
                {*paramTy.typeArgs[i], ...}, {*prTy->typeArgs[i], ...}).first;
        }

        res.insert(res.end(), currentCms.begin(), currentCms.end());
    }

    cms = res;
    return !res.empty();
}
```

**关键发现**: 编译器对名义类型的类型参数采用**不变性（Invariant）**约束，这与插件实现一致。

---

### 4. FindSolution - 求解约束

#### Join 和 Meet 操作

```cpp
std::optional<TypeSubst> FindSolution(Constraint& thisM, bool hasNothingTy, bool hasAnyTy) {
    TypeSubst thisSubst;
    bool newInfo;

    do {
        newInfo = false;
        for (auto tyVar : GetKeys(thisM)) {
            // 1. Join 下界（求最小上界）
            Ptr<Ty> tyJ{};
            auto joinRes = JoinAndMeet(tyMgr, thisM[tyVar].lbs, tyVarsOfThisM)
                .JoinAsVisibleTy();
            SetJoinedType(tyJ, joinRes);

            // 2. Meet 上界（求最大下界）
            Ptr<Ty> tyM = MeetUpperBounds(tyMgr, tyVar, thisM[tyVar].ubs, tyVarsOfThisM);

            // 3. 选择有效解
            if (IsValidSolution(*tyJ, validNothingTy, validAnyTy)) {
                thisSubst.emplace(tyVar, tyJ);  // 优先使用下界的 Join
                newInfo = true;
                thisM.erase(tyVar);
            } else if (IsValidSolution(*tyM, validNothingTy, validAnyTy)) {
                thisSubst.emplace(tyVar, tyM);  // 否则使用上界的 Meet
                newInfo = true;
                thisM.erase(tyVar);
            } else {
                // 无法求解：生成错误信息
            }
        }

        // 4. 应用已求解的类型到剩余约束
        thisM = ApplyTypeSubstForCS(thisSubst, thisM);

    } while (newInfo);  // 迭代直到无新信息

    return thisSubst;
}
```

**验证解的有效性**:

```cpp
bool IsValidSolution(const Ty& ty, bool hasNothingTy, bool hasAnyTy) const {
    bool solution = !ty.HasInvalidTy() && !ty.IsNothing() && !ty.IsAny()
                    && !ty.HasIdealTy() && !ty.IsCType();

    // 特殊情况：允许 Nothing/Any 类型
    solution = solution || (hasNothingTy && ty.IsNothing());
    solution = solution || (hasAnyTy && ty.IsAny());

    return solution;
}
```

---

### 5. GetBestSolution - 选择最佳解

当存在多个候选解时，选择最"具体"的解。

```cpp
std::optional<TypeSubst> GetBestSolution(const TypeSubsts& substs, bool allowPartial) {
    if (substs.empty()) return {};
    if (substs.size() == 1) return {*substs.begin()};

    std::vector<TypeSubst> candidates(substs.begin(), substs.end());
    std::vector<bool> maximals(candidates.size(), true);

    // 1. 如果允许部分解，优先选择未解变量少的
    if (allowPartial) {
        auto minCount = min(CountUnsolvedTyVars(tySub) for tySub in candidates);
        for (size_t i = 0; i < candidates.size(); i++) {
            if (CountUnsolvedTyVars(candidates[i]) > minCount) {
                maximals[i] = false;
            }
        }
    }

    // 2. 对每个类型变量，比较候选解
    for (auto tyVar : tyVarsToSolve) {
        CompareCandidates(tyVar, candidates, maximals);
    }

    // 3. 返回唯一的最大解
    auto idx = GetBestIndex(maximals);
    return idx ? candidates[*idx] : std::nullopt;
}

void CompareCandidates(Ptr<TyVar> tyVar,
                       const std::vector<TypeSubst>& candidates,
                       std::vector<bool>& maximals) {
    for (size_t i = 0; i < candidates.size(); ++i) {
        if (!maximals[i]) continue;

        auto tyI = GetInstantiatedTy(tyVar, candidates[i]);

        for (size_t j = i + 1; j < candidates.size(); ++j) {
            if (!maximals[j]) continue;

            auto tyJ = GetInstantiatedTy(tyVar, candidates[j]);

            // 比较规则：
            // - tyI <: tyJ 且 tyJ ⊄ tyI  →  tyI 更具体，标记 j 为非最大
            // - tyJ <: tyI 且 tyI ⊄ tyJ  →  tyJ 更具体，标记 i 为非最大
            // - 对于数值类型，使用特殊比较器

            if (tyI->IsNumeric() && tyJ->IsNumeric()) {
                auto res = CompareIntAndFloat(tyI, tyJ);
                if (res == GT) maximals[i] = false;
                else if (res == LT) maximals[j] = false;
            } else if (!IsSubtype(tyI, tyJ)) {
                maximals[i] = false;
            } else if (!IsSubtype(tyJ, tyI)) {
                maximals[j] = false;
            }

            if (!maximals[i]) break;
        }
    }
}
```

---

## 关键设计特点

### 1. 迭代式求解

编译器采用**迭代式**求解策略，而非一次性求解：

```cpp
// TypeArgumentInference.cpp 中的迭代逻辑
struct TyArgSynState {
    TyVars tyVarsToSolve;
    std::vector<Ptr<Ty>> argTys;
    std::vector<bool> failSet;           // 标记失败的参数
    std::vector<Ptr<Ty>> questParamTys;  // "quest"参数类型（占位符）
    size_t unsolvedCount;
    bool newInfo = true;                 // 是否有新信息
    bool lastResortUnused = true;        // 是否已使用最后手段（Lambda推导）
    std::optional<TypeSubst> solution;
};

std::optional<TypeSubst> PrepareTyArgsSynthesis(...) {
    auto state = TyArgSynState{...};

    while (state.newInfo && state.unsolvedCount > 0) {
        state.newInfo = false;

        // 1. 综合/检查参数
        for (size_t i = 0; i < argTys.size(); ++i) {
            if (failSet[i]) continue;

            auto paramTy = state.questParamTys.empty()
                ? paramTys[i]
                : state.questParamTys[i];

            // 综合或检查参数
            auto validArgTy = SynthOrCheckArgument(argTys[i], paramTy, ...);
            if (validArgTy) {
                argTys[i] = validArgTy;
            } else {
                failSet[i] = true;
            }
        }

        // 2. 收集有效参数类型
        std::vector<Ptr<Ty>> validArgTys;
        for (size_t i = 0; i < argTys.size(); ++i) {
            if (!failSet[i]) validArgTys.push_back(argTys[i]);
        }

        // 3. 局部类型参数综合
        auto optSubst = LocalTypeArgumentSynthesis(...).SynthesizeTypeArguments(true);

        if (optSubst && CountUnsolvedTyVars(*optSubst) < state.unsolvedCount) {
            state.solution = optSubst;
            state.unsolvedCount = CountUnsolvedTyVars(*optSubst);
            state.newInfo = true;  // 有新信息，继续迭代

            // 4. 为未解变量准备 "quest" 参数类型
            PrepareQuestParamTys(state, candidate, ...);
        }
    }

    // 5. 最后手段：Lambda推导
    if (state.unsolvedCount > 0 && state.lastResortUnused) {
        state.lastResortUnused = false;
        // 尝试从 Lambda 表达式推导类型
        ...
    }

    return state.solution;
}
```

**迭代过程**:
1. 第1轮：用已知类型综合参数
2. 求解约束，获得部分解
3. 第2轮：用部分解生成"quest"参数类型，继续综合
4. 重复直到无新信息
5. 最后手段：Lambda推导

---

### 2. 参数检查顺序优化

```cpp
std::vector<size_t> GetOrderedCheckingIndexes(const std::vector<Ptr<Ty>>& tys) {
    std::vector<size_t> ideals, options, others;

    for (size_t i = 0; i < tys.size(); ++i) {
        if (tys[i]->IsIdeal()) {
            ideals.push_back(i);  // 理想类型（如理想整数、理想浮点）
        } else if (tys[i]->IsCoreOptionType()) {
            options.push_back(i);  // Option 类型
        } else {
            others.push_back(i);   // 其他类型
        }
    }

    // 按 Option 嵌套深度排序（深的优先）
    std::stable_sort(options.begin(), options.end(), [&](auto l, auto r) {
        return CountOptionNestedLevel(*tys[l]) > CountOptionNestedLevel(*tys[r]);
    });

    // 合并顺序：Option（深→浅） → 其他 → 理想类型
    options.insert(options.end(), others.begin(), others.end());
    options.insert(options.end(), ideals.begin(), ideals.end());

    return options;
}
```

**优化原因**:
- **Option 类型优先**: 因为仓颉支持自动装箱，先处理 Option 类型可以正确推导 `Equatable<Option<A>>`
- **非理想类型优先**: 先处理具体类型，限制理想类型的范围
- **理想类型最后**: 理想类型可以转换为多种具体类型，最后处理避免过度泛化

---

### 3. 确定性求解模式

```cpp
bool deterministic = false;  // 确定性模式标志

bool IsGreedySolution(const TyVar& tv, const Ty& bound, bool isUpperbound) {
    // 贪婪解：立即固定类型变量的条件

    // 1. bound 是通用类型参数（如函数签名中的 T）
    bool tyParam = bound.IsGeneric() && !bound.IsPlaceholder();

    // 2. bound 是外层类型变量（不会泄露作用域）
    bool outerTyVar = bound.IsPlaceholder() &&
        (ScopeDepthOfTyVar(bound) <= ScopeDepthOfTyVar(tv));

    // 3. bound 是最终类型（无继承）
    bool finalType = (isUpperbound && bound.IsClass() && !IsInheritableClass(bound))
        || (!bound.IsGeneric() && !bound.IsClassLike() && !bound.IsAny() && !bound.IsNothing());

    // 4. bound 是 Any/Nothing
    bool anyOrNothing = (bound.IsAny() && !isUpperbound)
        || (bound.IsNothing() && isUpperbound);

    return tyParam || outerTyVar || finalType || anyOrNothing;
}

bool UnifyTyVar(...) {
    // ...
    if (deterministic && IsGreedySolution(*tyVar, *one, isUb)) {
        other = one;
        auto& eq = cms.front().constraint[tyVar].eq;
        if (eq.empty()) {
            eq.insert(one);  // 设置相等约束，立即固定
        }
    }
    // ...
}
```

**确定性模式用途**:
- 提供稳定的错误消息
- 避免歧义导致的不确定结果
- 在某些编译阶段启用，确保一致性

---

## 与插件实现的对比

### 相同点

| 方面 | 编译器 | 插件 |
|------|--------|------|
| **约束类型** | LOWER, UPPER, EQUALITY | LOWER, UPPER, EQUALITY |
| **不变性** | 名义类型参数不变 | 名义类型参数不变 |
| **Fresh Variables** | 创建占位符类型变量 | 创建 TypeVariableFromCallableDescriptor |
| **Join/Meet** | 用于求解约束 | 用于求解约束 |
| **拓扑排序** | TyVarConstraintGraph | 未使用 |

### 不同点

| 方面 | 编译器 | 插件 |
|------|--------|------|
| **求解策略** | **迭代式**多轮求解 | 单轮求解 |
| **参数顺序** | **优化顺序**（Option优先） | 原始顺序 |
| **部分解** | 支持部分解（`allowPartial`） | 不支持 |
| **确定性模式** | 支持贪婪求解 | 不支持 |
| **错误归因** | **Blame** 机制，精确定位 | 简单位置信息 |
| **Lambda推导** | 迭代失败后的最后手段 | 不明确 |
| **显式类型参数** | ？（需进一步分析） | **存在问题**（被丢弃） |

---

## 显式类型参数处理（完整分析）

### 编译器的处理机制

#### 1. 显式类型参数的获取

**AST 节点存储**: 显式类型参数存储在 AST 节点的 `typeArguments` 字段

```cpp
// Node.h - Expr 基类
class Expr : public Node {
    virtual std::vector<Ptr<Type>> GetTypeArgs() const {
        return {};  // 默认返回空
    }
};

// ReferenceExpr 重写
class ReferenceExpr : public Expr {
    std::vector<Ptr<Type>> typeArguments;  // 存储显式类型参数

    std::vector<Ptr<Type>> GetTypeArgs() const override {
        return ConverVector(typeArguments);
    }
};
```

**多处获取点**:

```cpp
// TypeCheckCall.cpp:1489 - 函数调用时
auto typeArgs = expr.GetTypeArgs();

// ExtraScopes.cpp:289 - CallExpr 处理时
std::vector<Ptr<Type>> typeArgs = ce.baseFunc->GetTypeArgs();

// ExtraScopes.cpp:255 - MemberAccess 基表达式处理时
auto baseTypeArgs = ma.baseExpr->GetTypeArgs();
```

#### 2. 两阶段类型映射机制

**核心数据结构** `SubstPack`:

```cpp
struct SubstPack {
    TypeSubst u2i;   // Universal to Instance: 通用类型参数 -> 实例类型变量
    MultiTypeSubst inst;  // Instance to concrete: 实例类型变量 -> 具体类型
};
```

**阶段一：创建新鲜变量** (`GenerateSubstPackByTyArgs` - ExtraScopes.cpp:187-216)

```cpp
void InstCtxScope::GenerateSubstPackByTyArgs(
    SubstPack& tmaps, const std::vector<Ptr<Type>>& typeArgs, const Generic& generic) const
{
    auto tyParamSize = generic.typeParameters.size();

    for (size_t i = 0; i < tyParamSize; ++i) {
        if (!generic.typeParameters[i]) continue;

        auto uTy = generic.typeParameters[i]->ty;  // 通用类型参数（如 T）
        if (Ty::IsTyCorrect(uTy)) {
            auto uGenTy = StaticCast<GenericsTy*>(uTy);

            // 1. 始终创建新鲜类型变量（即使已有显式类型参数）
            if (tmaps.u2i.count(uGenTy) == 0) {
                auto iGenTy = tyMgr.AllocTyVar();  // 创建新鲜变量 T'
                tmaps.u2i.emplace(uGenTy, iGenTy);  // T -> T'
            }

            // 2. 如果有显式类型参数，直接固定为具体类型
            if (i < typeArgs.size() && typeArgs[i] &&
                Ty::IsTyCorrect(typeArgs[i]->ty) && !typeArgs[i]->ty->HasIntersectionTy()) {
                tmaps.inst[StaticCast<GenericsTy*>(tmaps.u2i[uGenTy])] = {typeArgs[i]->ty};
                // T' -> Int64（显式类型参数）
            }
            // 否则 T' 保持为新鲜变量，等待约束求解
        }
    }
}
```

**阶段二：方法级类型参数映射** (`GenerateTypeMappingByTyArgs` - TypeCheckCall.cpp:874-901)

```cpp
TypeSubst TypeChecker::TypeCheckerImpl::GenerateTypeMappingByTyArgs(
    const std::vector<Ptr<Type>>& typeArgs, const Generic& generic) const
{
    TypeSubst typeMapping;
    auto typeArgsSize = typeArgs.size();

    // 检查数量匹配
    if (generic.typeParameters.size() != typeArgsSize) {
        return typeMapping;
    }

    // 直接生成类型参数 -> 显式类型的映射
    for (size_t i = 0; i < typeArgsSize; ++i) {
        if (!generic.typeParameters[i]) continue;

        Ptr<Ty> typeArgTy = TypeManager::GetInvalidTy();
        if (auto type = typeArgs[i]; type) {
            if (type->ty && type->ty->HasIntersectionTy()) {
                continue;
            }
            typeArgTy = type->ty;
        }

        if (Ty::IsTyCorrect(generic.typeParameters[i]->ty) && Ty::IsTyCorrect(typeArgTy)) {
            // 直接映射：T -> Int64
            typeMapping.emplace(StaticCast<GenericsTy*>(generic.typeParameters[i]->ty), typeArgTy);
        }
    }
    return typeMapping;
}
```

#### 3. 外层类型参数处理

**处理静态调用时的类类型参数** (`GenerateTypeMappingForBaseExpr` - ExtraScopes.cpp:220-275)

```cpp
void TypeChecker::TypeCheckerImpl::GenerateTypeMappingForBaseExpr(const Expr& baseExpr, SubstPack& typeMapping)
{
    if (baseExpr.astKind != ASTKind::MEMBER_ACCESS) return;

    auto& ma = static_cast<const MemberAccess&>(baseExpr);
    if (!Ty::IsTyCorrect(ma.baseExpr->ty)) return;

    // ... (处理 this/super 等)

    auto maTarget = ma.GetTarget();
    bool typeDeclMemberAccess = realBase && maTarget && maTarget->outerDecl &&
        (realBase->IsTypeDecl() || realBase->TestAttr(Attribute::ENUM_CONSTRUCTOR));

    if (typeDeclMemberAccess) {
        // 关键：获取 baseExpr 的显式类型参数
        auto baseTypeArgs = ma.baseExpr->GetTypeArgs();
        std::unordered_set<Ptr<Ty>> baseTyArgs;
        std::for_each(baseTypeArgs.begin(), baseTypeArgs.end(),
            [&baseTyArgs](auto type) { baseTyArgs.emplace(type->ty); });

        auto genericTys = GetAllGenericTys(realBase->ty);

        // 如果类型参数已由用户提供，从推断映射中移除
        for (auto it = promoteMapping.begin(); it != promoteMapping.end(); ++it) {
            // 移除那些存在于泛型类型但不在用户定义中的类型参数
            Utils::EraseIf(it->second, [&genericTys, &baseTyArgs](auto ty) {
                return genericTys.count(ty) != 0 && baseTyArgs.count(ty) == 0;
            });
        }

        // ... (合并映射)
    }
}
```

#### 4. 完整调用流程示例

**场景**: `a<Int64>.a1()` 调用

```cpp
// Step 1: TypeCheckCall.cpp - CollectValidFuncTys (Line 1489)
auto typeArgs = expr.GetTypeArgs();  // 返回空（a1 没有显式类型参数）

for (auto fd : funcs) {
    SubstPack mts;

    // Step 2: 处理函数自己的泛型（如果有）
    if (auto generic = fd->GetGeneric()) {
        if (!typeArgs.empty()) {
            // 如果函数调用有显式类型参数，使用它们
            typeManager.PackMapping(mts, GenerateTypeMappingByTyArgs(typeArgs, *generic));
        }
    }

    // Step 3: 处理外层类型参数（关键！）
    // expr 是 MemberAccess: a<Int64>.a1
    MergeSubstPack(mts, GenerateGenericTypeMapping(ctx, expr));
    // ↓ 调用 GenerateTypeMappingForBaseExpr
    // ↓ 获取 a<Int64> 的 typeArgs
    // ↓ 生成 T -> Int64 的映射

    // Step 4: 应用类型替换
    typeMappings = typeManager.ZipSubstPack(mts);
    auto instTy = typeManager.GetInstantiatedTy(fd->ty, mapping);
}
```

**Step 3 详细流程**:

```cpp
// GenerateGenericTypeMapping (TypeCheckGeneric.cpp:333)
SubstPack GenerateGenericTypeMapping(const ASTContext& ctx, const Expr& expr)
{
    SubstPack typeMapping;

    if (auto ma = DynamicCast<const MemberAccess*>(&expr); ma) {
        // expr 是 a<Int64>.a1，ma->baseExpr 是 a<Int64>
        GenerateTypeMappingForBaseExpr(*ma, typeMapping);
        // ↓
        // ma.baseExpr->GetTypeArgs() 返回 [Int64]
        // 生成 SubstPack:
        //   u2i: {T -> T'}  (T 是类的类型参数，T' 是新鲜变量)
        //   inst: {T' -> Int64}  (T' 直接固定为 Int64)
    }

    return typeMapping;
}
```

**最终结果**:
- 方法 `a1` 的类型被实例化为 `a<Int64>.a1()`
- 不需要运行约束求解器推导 T
- T 已经通过 `inst` 映射直接固定为 `Int64`

### 关键设计原则

**编译器采用的策略**:

1. **始终创建新鲜变量**: 即使类型参数已显式给出，也创建新鲜类型变量
2. **两阶段映射**:
   - `u2i`: 通用类型参数 -> 新鲜变量（总是创建）
   - `inst`: 新鲜变量 -> 具体类型（仅当显式给出时）
3. **统一处理**: 显式类型和推导类型都经过相同的替换机制
4. **延迟固定**: 通过 `inst` 映射固定类型，而非约束系统

**与约束系统的关系**:

- 显式类型参数**不进入**约束求解器
- 它们通过 `SubstPack.inst` 映射**直接固定**
- 约束求解器只处理**未固定的**新鲜变量

### 插件的问题回顾

插件当前实现:

```kotlin
fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
    if (resolvedCall.dispatchReceiverArgument != null &&
        resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

        // ✗ 问题：只看 declaredTypeParameters，忽略显式类型参数
        return (resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver)
            .classQualifier.descriptor.declaredTypeParameters +  // 总是返回 [T]
            candidateDescriptor.original.typeParameters
    }

    return candidateDescriptor.original.typeParameters
}
```

**问题根源**:
1. `ClassValueReceiver.type` 包含 `a<Int64>` 的完整信息（包括 `arguments = [Int64]`）
2. `getTypeParameters()` 只返回原始声明的类型参数 `[T]`
3. 创建 Fresh Variable `T'` 时，完全忽略了 `Int64`
4. **缺失**: 没有建立 `T' -> Int64` 的固定映射
5. **结果**: 约束求解器无法推导出 T，因为 `a1()` 不使用 T

---

## 改进建议

### 建议 1: 检查显式类型参数

参考编译器做法，在收集类型参数前检查是否已有显式类型参数：

```kotlin
fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
    if (resolvedCall.dispatchReceiverArgument != null &&
        resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

        val classValueReceiver = resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver
        val classDescriptor = classValueReceiver.classQualifier.descriptor
        val classType = classValueReceiver.type

        // ✓ 改进：检查类型是否已包含显式类型参数
        if (classType.arguments.isNotEmpty()) {
            // 类型参数已显式给出（如 a<Int64>）
            // 不需要推导类的类型参数，直接使用已有类型
            return candidateDescriptor.original.typeParameters  // 只收集方法的类型参数
        }

        // 类型参数未给出（如 a）
        return classDescriptor.declaredTypeParameters +
               candidateDescriptor.original.typeParameters
    }

    return candidateDescriptor.original.typeParameters
}
```

### 建议 2: 添加显式类型约束

即使不收集类型参数，也需要在约束系统中记录显式类型：

```kotlin
override fun ResolutionCandidate.process(workIndex: Int) {
    val csBuilder = getSystem().getBuilder()

    val typeParameters = getTypeParameters()
    val knownTypeArguments = extractKnownTypeArguments()  // 新增：提取显式类型参数

    val (toFreshVariables, freshTypeVariables) =
        if (typeParameters.isEmpty())
            ComposableTypeSubstitutor.EMPTY to emptyList()
        else
            createToFreshVariableSubstitutorAndAddInitialConstraints(...)

    // ✓ 新增：为显式给出的类型参数添加相等约束
    for ((typeParam, explicitType) in knownTypeArguments) {
        val freshVar = freshTypeVariables.find { it.originalTypeParameter == typeParam }
        if (freshVar != null) {
            csBuilder.addEqualityConstraint(
                freshVar.defaultType,
                explicitType,
                ExplicitTypeParameterConstraintPositionImpl(...)
            )
        }
    }

    // 继续原有逻辑...
}

private fun ResolutionCandidate.extractKnownTypeArguments(): Map<TypeParameterDescriptor, UnwrappedType> {
    val receiver = resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue

    if (receiver is ClassValueReceiver) {
        val classDescriptor = receiver.classQualifier.descriptor
        val classType = receiver.type

        // 从 a<Int64> 中提取 [T -> Int64] 的映射
        return classDescriptor.declaredTypeParameters.zip(classType.arguments)
            .mapNotNull { (param, arg) ->
                val argType = arg.type as? UnwrappedType
                if (argType != null) param to argType else null
            }
            .toMap()
    }

    return emptyMap()
}
```

### 建议 3: 考虑迭代式求解（可选）

如果遇到复杂场景（如 Lambda 推导），可以参考编译器的迭代式求解：

```kotlin
// 伪代码示例
fun resolveWithIteration(candidate: ResolutionCandidate): Boolean {
    var state = InferenceState(...)
    var iteration = 0

    while (state.hasNewInfo && iteration < MAX_ITERATIONS) {
        state.hasNewInfo = false

        // 1. 综合参数类型
        for (arg in arguments) {
            val paramType = if (state.hasPartialSolution) {
                applyPartialSolution(arg.expectedType)
            } else {
                arg.expectedType
            }

            val inferredType = synthesize(arg, paramType)
            if (inferredType != null) {
                state.hasNewInfo = true
                arg.type = inferredType
            }
        }

        // 2. 求解约束
        val solution = solveConstraints(allowPartial = true)
        if (solution.unsolvedCount < state.unsolvedCount) {
            state.partialSolution = solution
            state.unsolvedCount = solution.unsolvedCount
            state.hasNewInfo = true
        }

        iteration++
    }

    return state.unsolvedCount == 0
}
```

---

## 插件约束系统完整分析

### 1. 约束系统架构概览

**核心接口层次**:

```
ConstraintSystem (顶层接口)
    ├── hasContradiction: Boolean
    ├── errors: List<ConstraintSystemError>
    ├── getBuilder(): ConstraintSystemBuilder
    ├── asReadOnlyStorage(): ConstraintStorage
    └── asConstraintSystemCompleterContext(): ConstraintSystemCompletionContext

ConstraintSystemBuilder (构建器接口)
    ├── extends ConstraintSystemOperation
    ├── prepareTransaction(): ConstraintSystemTransaction
    ├── buildCurrentSubstitutor(): TypeSubstitutorMarker
    └── currentStorage(): ConstraintStorage

ConstraintSystemOperation (操作接口)
    ├── registerVariable(variable)
    ├── addSubtypeConstraint(lowerType, upperType, position)
    ├── addEqualityConstraint(a, b, position)
    ├── isProperType(type): Boolean
    └── isTypeVariable(type): Boolean
```

**文件位置**:
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ConstraintSystem.kt`
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ConstraintSystemBuilder.kt`

---

### 2. 约束存储系统 (ConstraintStorage)

**文件位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/model/ConstraintStorage.kt`

#### 核心数据结构

```kotlin
interface ConstraintStorage {
    // 所有类型变量（包括已固定和未固定）
    val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

    // 未固定的类型变量及其约束
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    // 已固定的类型变量映射
    val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>

    // 初始约束列表
    val initialConstraints: List<InitialConstraint>

    // 延迟处理的类型变量
    val postponedTypeVariables: List<TypeVariableMarker>

    // 是否存在矛盾
    val hasContradiction: Boolean

    // 错误列表
    val errors: List<ConstraintSystemError>
}
```

#### 类型变量生命周期

```
创建 → 注册到 allTypeVariables
            ↓
       添加到 notFixedTypeVariables（带约束）
            ↓
       收集约束（LOWER/UPPER/EQUALITY）
            ↓
       约束求解（找到结果类型）
            ↓
       移出 notFixedTypeVariables
            ↓
       添加到 fixedTypeVariables
```

#### 约束类型定义

```kotlin
enum class ConstraintKind {
    LOWER,      // 下界约束: A <: T
    UPPER,      // 上界约束: T <: A
    EQUALITY;   // 相等约束: T == A

    fun opposite() = when (this) {
        LOWER -> UPPER
        UPPER -> LOWER
        EQUALITY -> EQUALITY
    }
}
```

#### 约束数据结构

```kotlin
class Constraint(
    val kind: ConstraintKind,                    // 约束类型
    val type: CangJieTypeMarker,                 // 约束涉及的类型
    val position: IncorporationConstraintPosition,  // 位置信息
    val typeHashCode: Int,                       // 类型哈希（优化）
    val derivedFrom: Set<TypeVariableMarker>,    // 派生来源
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition?  // 合并前位置
)

interface VariableWithConstraints {
    val typeVariable: TypeVariableMarker
    val constraints: List<Constraint>
}
```

---

### 3. 类型变量系统 (TypeVariable)

**文件位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/model/TypeVariable.kt`

#### 核心类层次

```kotlin
// 类型变量类型构造器
class TypeVariableTypeConstructor(
    builtIns: CangJieBuiltIns,
    val debugName: String,
    val originalTypeParameter: TypeParameterDescriptor?
) : TypeConstructor {
    // 标记：是否出现在不变/逆变位置
    var isContainedInInvariantOrContravariantPositions: Boolean = false
}

// 新类型变量基类
sealed class NewTypeVariable(
    builtIns: CangJieBuiltIns,
    name: String,
    originalTypeParameter: TypeParameterDescriptor? = null
) : TypeVariableMarker {
    val freshTypeConstructor = TypeVariableTypeConstructor(builtIns, name, originalTypeParameter)
    val defaultType: SimpleType = freshTypeConstructor.typeForTypeVariable()
}

// 从可调用描述符创建的类型变量（最常用）
class TypeVariableFromCallableDescriptor(
    val originalTypeParameter: TypeParameterDescriptor
) : NewTypeVariable(
    originalTypeParameter.builtIns,
    originalTypeParameter.name.toString(),
    originalTypeParameter
)

// 其他特化类型变量
class TypeVariableForLambdaParameterType(...)  // Lambda 参数类型
class TypeVariableForCallableReferenceParameterType(...)  // 可调用引用参数
class TypeVariableForCallableReferenceReturnType(...)  // 可调用引用返回类型
```

#### Fresh Variable 创建流程（关键！）

**文件位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/components/ResolutionParts.kt`

```kotlin
object CreateFreshVariablesSubstitutor : ResolutionPart() {

    // ★ 关键方法：获取需要推导的类型参数
    fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
        // 如果是静态调用（通过 ClassValueReceiver）
        if (resolvedCall.dispatchReceiverArgument != null &&
            resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

            // ★ 问题所在：合并类的类型参数 + 方法的类型参数
            // 没有检查 ClassValueReceiver.type 中是否已有显式类型参数！
            return (resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver)
                .classQualifier.descriptor.declaredTypeParameters +  // 类的类型参数 [T]
                candidateDescriptor.original.typeParameters            // 方法的类型参数
        }

        return candidateDescriptor.original.typeParameters
    }

    // 创建 Fresh Variable 和初始约束
    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        candidateDescriptor: CallableDescriptor,
        cangjieCall: CangJieCall,
        csBuilder: ConstraintSystemOperation,
        typeParameters: List<TypeParameterDescriptor>
    ): Pair<ComposableTypeSubstitutor, List<TypeVariableFromCallableDescriptor>> {

        // 1. 为每个类型参数创建 Fresh Variable
        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }

        // 2. 创建替换器 T -> T'
        val toFreshVariables = ComposableTypeSubstitutor.create(
            SubstitutorFunction.fromFreshVariables(freshTypeVariables),
            SubstitutionOptions.INFERENCE
        )

        // 3. 注册类型变量到约束系统
        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        // 4. 添加上界约束
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]
            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter, cangjieCall)

            for (upperBound in typeParameter.upperBounds) {
                csBuilder.addSubtypeConstraint(
                    freshVariable.defaultType,
                    toFreshVariables.safeSubstitute(upperBound.unwrap()),
                    position
                )
            }
        }

        return toFreshVariables to freshTypeVariables
    }
}
```

---

### 4. 类型变量固定判定 (VariableFixationFinder)

**文件位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/components/VariableFixationFinder.kt`

#### 固定就绪状态枚举

```kotlin
enum class TypeVariableFixationReadiness {
    // 禁止固定
    FORBIDDEN,

    // 没有正确的参数约束
    WITHOUT_PROPER_ARGUMENT_CONSTRAINT,

    // 依赖外层类型变量
    OUTER_TYPE_VARIABLE_DEPENDENCY,

    // 准备好固定（来自声明的上界，有自引用类型）
    READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES,

    // 有复杂依赖
    WITH_COMPLEX_DEPENDENCY,

    // 所有约束都是平凡的或非正确的
    ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER,

    // 关联到输出类型
    RELATED_TO_ANY_OUTPUT_TYPE,

    // 来自声明上界的合并
    FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND,

    // 准备好固定（有上界）
    READY_FOR_FIXATION_UPPER,

    // 准备好固定（有下界）
    READY_FOR_FIXATION_LOWER,

    // 准备好固定
    READY_FOR_FIXATION,

    // 准备好固定（具体化）
    READY_FOR_FIXATION_REIFIED,
}
```

#### 正确类型检查

```kotlin
inline fun TypeSystemInferenceExtensionContext.isProperTypeForFixation(
    type: CangJieTypeMarker,
    notFixedTypeVariables: Set<TypeConstructorMarker>,
    isProper: (CangJieTypeMarker) -> Boolean
): Boolean {
    // 如果类型构造器是未固定的类型变量，则不是正确类型
    if (type.typeConstructor() in notFixedTypeVariables) return false

    // 检查类型本身和所有捕获类型的投影
    return isProper(type) && extractProjectionsForAllCapturedTypes(type).all(isProper)
}
```

#### 固定顺序决策

```kotlin
class VariableFixationFinder(...) {

    // 查找下一个要固定的类型变量
    fun findFirstVariableForFixation(
        c: Context,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        ...
    ): VariableForFixation? {
        // 1. 收集所有未固定的类型变量
        val notFixedTypeVariables = c.notFixedTypeVariables.keys

        // 2. 计算每个变量的固定就绪状态
        // 3. 选择最佳候选变量
        // 4. 返回变量和固定方向
    }

    // 获取变量的固定就绪状态
    private fun Context.getTypeVariableReadiness(
        variable: VariableWithConstraints,
        ...
    ): TypeVariableFixationReadiness {
        // 检查各种条件，返回对应的就绪状态
    }
}
```

---

### 5. 结果类型解析器 (ResultTypeResolver)

**文件位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/components/ResultTypeResolver.kt`

#### 核心求解逻辑

```kotlin
class ResultTypeResolver(...) {

    // 查找结果类型
    fun findResultType(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection
    ): CangJieTypeMarker? {
        // 1. 查找单位约束（单个 EQUALITY 约束）
        findResultTypeOfTheUnitConstraint(c, variableWithConstraints)?.let { return it }

        // 2. 根据方向选择求解策略
        return when (direction) {
            TO_SUBTYPE -> findSubType(c, variableWithConstraints)     // 求下界的 Join
            TO_SUPERTYPE -> findSuperType(c, variableWithConstraints) // 求上界的 Meet
            UNKNOWN -> findResultTypeOrNull(c, variableWithConstraints)
        }
    }

    // 求子类型（从 LOWER 约束求 LUB）
    private fun findSubType(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        val lowerConstraints = variableWithConstraints.constraints.filter { it.kind == LOWER }

        if (lowerConstraints.isEmpty()) return null

        // 计算所有下界的 LUB（最小上界）
        val types = lowerConstraints.map { it.type }
        return c.commonSuperTypeOrNull(types) ?: c.intersectTypes(types)
    }

    // 求超类型（从 UPPER 约束求 GLB）
    private fun findSuperType(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        val upperConstraints = variableWithConstraints.constraints.filter { it.kind == UPPER }

        if (upperConstraints.isEmpty()) return null

        // 计算所有上界的 GLB（最大下界）
        val types = upperConstraints.map { it.type }
        return c.intersectTypes(types)
    }

    // 通用求解（先尝试下界，再尝试上界）
    private fun findResultTypeOrNull(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        // 先尝试 LOWER 约束
        findSubType(c, variableWithConstraints)?.let { return it }

        // 再尝试 UPPER 约束
        return findSuperType(c, variableWithConstraints)
    }
}
```

---

### 6. 约束系统完成器 (CangJieConstraintSystemCompleter)

**文件位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/components/CangJieConstraintSystemCompleter.kt`

#### 9 阶段完成流程

```kotlin
class CangJieConstraintSystemCompleter(...) {

    fun runCompletion(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        ...
    ) {
        while (true) {
            // Phase 1: 分析具有固定参数类型的参数
            if (analyzeArgumentsWithFixedParameterTypes(c, ...)) continue

            // Phase 2: 收集参数类型并构建新的期望类型
            if (collectParameterTypesAndBuildNewExpectedTypes(c, ...)) continue

            // Phase 3: 固定参数类型变量
            if (fixParameterTypeVariables(c, ...)) continue

            // Phase 4: 用新的函数期望类型创建原子
            if (createAtomsWithNewFunctionalExpectedTypes(c, ...)) continue

            // Phase 5: 分析下一个就绪的延迟参数
            if (analyzeNextReadyPostponedArgument(c, ...)) continue

            // Phase 6: 固定下一个就绪的类型变量
            if (fixNextReadyTypeVariable(c, ...)) continue

            // Phase 7: 尝试 Builder 推断
            if (tryBuilderInference(c, ...)) continue

            // Phase 8: 报告"信息不足"错误
            if (reportNotEnoughInformation(c, ...)) continue

            // Phase 9: 强制分析剩余的延迟参数
            if (forceAnalyzeRemainingPostponedArguments(c, ...)) continue

            // 所有阶段都没有进展，退出
            break
        }
    }

    // 固定类型变量的核心方法
    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection
    ) {
        // 1. 使用 ResultTypeResolver 找到结果类型
        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)
            ?: return

        // 2. 固定类型变量
        c.fixVariable(variableWithConstraints.typeVariable, resultType)
    }
}
```

---

### 7. 事务支持

**文件位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/inference/ConstraintSystemBuilder.kt`

```kotlin
// 事务抽象类
abstract class ConstraintSystemTransaction {
    abstract fun closeTransaction()    // 提交事务
    abstract fun rollbackTransaction() // 回滚事务
}

// 事务运行扩展函数
inline fun ConstraintSystemBuilder.runTransaction(
    crossinline runOperations: ConstraintSystemOperation.() -> Boolean
): Boolean {
    val transactionState = prepareTransaction()

    if (runOperations()) {
        transactionState.closeTransaction()  // 成功，提交
        return true
    }

    transactionState.rollbackTransaction()  // 失败，回滚
    return false
}

// 兼容性检查（只读测试）
fun ConstraintSystemBuilder.isSubtypeConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean {
    var isCompatible = false
    runTransaction {
        if (!hasContradiction) {
            addSubtypeConstraint(lowerType, upperType, position)
        }
        isCompatible = !hasContradiction
        false  // 始终回滚
    }
    return isCompatible
}
```

---

## 编译器与插件完整对比

### 数据结构对比

| 方面 | 编译器 (C++) | 插件 (Kotlin) |
|------|-------------|---------------|
| **约束存储** | `Constraint` map | `ConstraintStorage` interface |
| **类型变量** | `TyVar` (Placeholder) | `TypeVariableFromCallableDescriptor` |
| **约束类型** | `lbs` (下界), `ubs` (上界), `eq` (相等) | `ConstraintKind.LOWER/UPPER/EQUALITY` |
| **类型变量状态** | 单一 map + 标记 | `notFixedTypeVariables` + `fixedTypeVariables` |
| **类型映射** | `SubstPack` (u2i + inst) | `ComposableTypeSubstitutor` |

### 算法对比

| 方面 | 编译器 (C++) | 插件 (Kotlin) |
|------|-------------|---------------|
| **求解策略** | 迭代式多轮求解 | 单轮求解 |
| **固定顺序** | 拓扑排序 `TyVarConstraintGraph` | `VariableFixationFinder` 启发式 |
| **参数顺序** | 优化顺序（Option 优先） | 原始顺序 |
| **部分解** | 支持 `allowPartial` | 不支持 |
| **Join/Meet** | `JoinAndMeet` 类 | `ResultTypeResolver` |
| **事务支持** | 无明确事务 | `ConstraintSystemTransaction` |

### 显式类型参数处理对比

| 方面 | 编译器 (C++) | 插件 (Kotlin) |
|------|-------------|---------------|
| **获取位置** | `Expr::GetTypeArgs()` | `ClassValueReceiver.type.arguments` |
| **映射机制** | 两阶段 (u2i + inst) | 单一替换器 |
| **显式类型处理** | `inst` 直接固定 | ★ **缺失！** |
| **Fresh Variable 创建** | 始终创建，显式类型存入 inst | 始终创建，显式类型被忽略 |

### 完成流程对比

| 编译器 | 插件 |
|--------|------|
| 1. CopyUpperbound | 1. analyzeArgumentsWithFixedParameterTypes |
| 2. InitConstraints | 2. collectParameterTypesAndBuildNewExpectedTypes |
| 3. Unify (按优化顺序) | 3. fixParameterTypeVariables |
| 4. 返回类型约束 | 4. createAtomsWithNewFunctionalExpectedTypes |
| 5. SolveConstraints (迭代) | 5. analyzeNextReadyPostponedArgument |
| - | 6. fixNextReadyTypeVariable |
| - | 7. tryBuilderInference |
| - | 8. reportNotEnoughInformation |
| - | 9. forceAnalyzeRemainingPostponedArguments |

---

## 问题根源分析

### 核心问题

**文件**: `ResolutionParts.kt:1268-1280`

```kotlin
fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
    if (resolvedCall.dispatchReceiverArgument != null &&
        resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

        // ★ 问题：只看 declaredTypeParameters，完全忽略 type.arguments
        return (resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver)
            .classQualifier.descriptor.declaredTypeParameters +  // 返回 [T]，而非 []
            candidateDescriptor.original.typeParameters
    }

    return candidateDescriptor.original.typeParameters
}
```

### 信息丢失链路

```
a<Int64>.a1()
    ↓
限定符解析 → 创建 ClassValueReceiver
    ↓
ClassValueReceiver {
    type: a<Int64>              // ✓ 包含显式类型参数
    classQualifier.descriptor   // ✗ 只有 declaredTypeParameters = [T]
}
    ↓
getTypeParameters()
    ↓
返回 [T]，忽略 type.arguments = [Int64]
    ↓
createToFreshVariableSubstitutorAndAddInitialConstraints()
    ↓
创建 TypeVariableFromCallableDescriptor(T)  // T'
    ↓
注册到约束系统（无约束，因为 a1() 不使用 T）
    ↓
约束求解失败：T' 无法推导
```

### 编译器如何避免此问题

```cpp
// 编译器的两阶段映射
void InstCtxScope::GenerateSubstPackByTyArgs(SubstPack& tmaps,
    const std::vector<Ptr<Type>>& typeArgs, const Generic& generic) const {

    for (size_t i = 0; i < tyParamSize; ++i) {
        auto uTy = generic.typeParameters[i]->ty;  // 通用类型参数 T

        // 1. 始终创建 Fresh Variable
        auto iGenTy = tyMgr.AllocTyVar();  // T'
        tmaps.u2i.emplace(uGenTy, iGenTy);  // T -> T'

        // 2. 如果有显式类型参数，直接固定！
        if (i < typeArgs.size() && typeArgs[i]) {
            tmaps.inst[iGenTy] = {typeArgs[i]->ty};  // T' -> Int64
        }
    }
}
```

**关键区别**:
- 编译器：`u2i` (T→T') + `inst` (T'→Int64)
- 插件：只有替换器 (T→T')，没有显式类型固定

---

## 解决方案

### 方案 A：不收集已显式给出的类型参数

```kotlin
fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {
    if (resolvedCall.dispatchReceiverArgument != null &&
        resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

        val classValueReceiver = resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver
        val classType = classValueReceiver.type

        // ✓ 改进：检查类型是否已包含显式类型参数
        if (classType.arguments.isNotEmpty()) {
            // 类型参数已显式给出，不需要推导
            return candidateDescriptor.original.typeParameters  // 只返回方法的类型参数
        }

        // 类型参数未给出，需要推导
        return classValueReceiver.classQualifier.descriptor.declaredTypeParameters +
               candidateDescriptor.original.typeParameters
    }

    return candidateDescriptor.original.typeParameters
}
```

### 方案 B：为显式类型参数添加相等约束

```kotlin
override fun ResolutionCandidate.process(workIndex: Int) {
    val csBuilder = getSystem().getBuilder()
    val typeParameters = getTypeParameters()

    // ✓ 新增：提取显式类型参数
    val knownTypeArguments = extractKnownTypeArguments()

    val (toFreshVariables, freshTypeVariables) =
        createToFreshVariableSubstitutorAndAddInitialConstraints(...)

    // ✓ 新增：为显式类型参数添加 EQUALITY 约束
    for ((typeParam, explicitType) in knownTypeArguments) {
        val freshVar = freshTypeVariables.find { it.originalTypeParameter == typeParam }
        if (freshVar != null) {
            csBuilder.addEqualityConstraint(
                freshVar.defaultType,
                explicitType,
                ExplicitTypeParameterConstraintPositionImpl(...)
            )
        }
    }

    // ... 继续原有逻辑
}

private fun ResolutionCandidate.extractKnownTypeArguments(): Map<TypeParameterDescriptor, UnwrappedType> {
    val receiver = resolvedCall.dispatchReceiverArgument?.receiver?.receiverValue

    if (receiver is ClassValueReceiver) {
        val classDescriptor = receiver.classQualifier.descriptor
        val classType = receiver.type

        return classDescriptor.declaredTypeParameters.zip(classType.arguments)
            .mapNotNull { (param, arg) ->
                val argType = arg.type as? UnwrappedType
                if (argType != null) param to argType else null
            }
            .toMap()
    }

    return emptyMap()
}
```

### 推荐方案

**方案 A + B 结合**:
1. 方案 A 避免为已知类型创建不必要的 Fresh Variable
2. 方案 B 确保显式类型与参数约束的一致性检查（如 `a<String>.a2(1)` 应报错）

---

## 测试验证矩阵

| 场景 | 当前行为 | 修复后行为 | 预期 |
|------|---------|-----------|------|
| `a.a1()` | ✗ 推导失败 | ✗ 推导失败 | ✓ |
| `a<Int64>.a1()` | ✗ 推导失败 | ✓ 成功 | ✓ **修复** |
| `Option.None` | ✗ 推导失败 | ✗ 推导失败 | ✓ |
| `Option<String>.None` | ✗ 推导失败 | ✓ 成功 | ✓ **修复** |
| `a.a2(1)` | ✓ T=Int64 | ✓ T=Int64 | ✓ |
| `a<String>.a2(1)` | ✓ T=Int64 (忽略显式) | ✗ 类型错误 | ✓ **修复** |
| `Some(1)` | ✓ T=Int64 | ✓ T=Int64 | ✓ |
| `Some<Int64>(1)` | ✓ T=Int64 | ✓ T=Int64 | ✓ |

---

## 总结

### 编译器的优势

1. **两阶段类型映射**: `u2i` + `inst` 机制优雅处理显式类型参数
2. **迭代式求解**: 支持复杂场景的逐步求解
3. **拓扑排序**: `TyVarConstraintGraph` 优化求解顺序
4. **Blame 机制**: 精确错误归因
5. **参数顺序优化**: Option 类型优先处理
6. **确定性模式**: 贪婪求解提供稳定行为

### 插件的优势

1. **事务支持**: `ConstraintSystemTransaction` 提供回滚能力
2. **详细的固定状态**: `TypeVariableFixationReadiness` 精细控制
3. **多阶段完成**: 9 阶段完成流程处理复杂场景
4. **Builder 推断**: 支持高级 Lambda 推断

### 插件需要改进的地方

1. **✓ 立即修复**: 处理显式类型参数（方案 A + B）
2. **可考虑**: 添加参数顺序优化（Option 优先）
3. **可考虑**: 实现 Blame 机制改进错误消息
4. **可选**: 迭代式求解（仅在遇到复杂场景时）

### 实现路线图

1. **Phase 1**: 核心修复
   - 修改 `getTypeParameters()` 检查显式类型参数
   - 实现 `extractKnownTypeArguments()`
   - 添加显式类型 EQUALITY 约束

2. **Phase 2**: 测试验证
   - 单元测试覆盖所有场景
   - 回归测试确保兼容性

3. **Phase 3**: 可选优化
   - 参数顺序优化
   - Blame 机制
   - 错误消息改进

---

**文档版本**: 2.0
**创建日期**: 2026-01-18
**更新日期**: 2026-01-18
**状态**: 完整分析完成，待实现修复方案