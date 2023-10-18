package com.huawei.cangjie1.utils;

import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface ReadOnly {
}
