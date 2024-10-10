



Try-with-resources表达式 
```text
try (Resource res = ...) {
    // Use res
}
//可以没有catch和finally
```


元组访问
```text
    let a = (1, 2, 3)
    a[0] // 1
    
    a[4] //error 
```

元组赋值  //使用for表达式的模式进行实现
```text
    let a = (1, 2, 3)
    let (x, y, z) = a
    
```

重名检查
```text
    由于枚举是展开的，可能与其他声明的重名检查不触发
    接口与变量重名检查不触发
```
