package cn.xu.framework.design.printlog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PrintAopLog {

    String[] securityFields() default {};

    String replaceStr() default "*";

    boolean paramFlag() default false;

    boolean resultFlag() default false;
}
