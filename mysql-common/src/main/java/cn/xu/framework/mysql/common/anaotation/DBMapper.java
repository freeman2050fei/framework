package cn.xu.framework.mysql.common.anaotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DBMapper {
    String mapping();
    String dateFormat() default "";
    boolean checkTbFieldDuplicate() default true;
}
