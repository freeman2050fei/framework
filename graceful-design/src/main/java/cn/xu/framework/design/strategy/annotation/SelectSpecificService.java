package cn.xu.framework.design.strategy.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SelectSpecificService {
    Class<?> specificServiceInterface();
}
