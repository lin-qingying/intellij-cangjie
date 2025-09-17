# [deserialization](deserialization)模块
该模块主要负责二进制的反序列化
```
    文件内不在需要nameResolver，
    因为nameResolver是用来解析proto数据中的string的，文件内也不要包含proto字样,这些原本是使用proto进行序列化的，现在改成使用flatbuffers，org.cangnova.cangjie.metadata.model包中是将flatbuffers初步整理后的类型，现在所有这些原本使用proto的，改为使用org.cangnova.cangjie.metadata.model包    
```

## [DeserializedMemberScope](deserialization/src/main/kotlin/org/cangnova/cangjie/serialization/deserialization/descriptors/DeserializedMemberScope.kt)
该文件管理反序列化的Scope，已完成从proto到flatbuffers的重构：
- 移除了nameResolver的依赖，直接使用Decl.identifier
- 更新了所有proto相关的注释和变量名
- 将functionProtos/variableProtos/propertyProtos重命名为functionDecls/variableDecls/propertyDecls
- 注释中的"ProtoBuf"已更新为"flatbuffer"


# [metadata](metadata)
该模块声明了元数据格式

# [analysis](analysis)
## [decompiler-to-psi](analysis/decompiler-to-psi)
这个模块的作用是将仓颉语言的编译后文件（如.cjo文件或元数据文件）反编译成PSI（Program Structure Interface）树结构，以便IntelliJ
IDEA能够理解和分析仓颉代码，提供代码补全、导航、重构等IDE功能。

# [descriptors](descriptors)

```
ClassifierDescriptor是所有可以成为类型的接口(class，struct，类型参数，类型别名...)它与cangjieType发生直接关系，ClassifierDescriptor不一定可以继承，也不一定有作用域信息           │
ClassDescriptor是struct，class,interface共用的，它有作用域信息，也可以继承其他类型
EnumDescriptor是enum类型，它和ClassDescriptor是同一级别的，只不过由于enum的特殊性，所以需要新接口
ExtendDescriptor表示一个扩展声明，它不与cangjieType发生关系，但是它又是一个可以继承，有作用域的
    extend Type <: 接口1，接口2{
        func 方法1(){}
    }
    同时ExtendDescriptor也拥有declaredTypeParameters
    
    
某些位置需要处理作用域，例如(descriptors/src/main/kotlin/org/cangnova/cangjie/descriptors/impl/ModuleAwareClassDescriptor.kt)
某些位置需要处理重写，例如(descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/OverridingUtil.kt)

现在接口设计非常混乱，导致有些位置访问不了某个属性或方法
```                 