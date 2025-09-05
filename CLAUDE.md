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
