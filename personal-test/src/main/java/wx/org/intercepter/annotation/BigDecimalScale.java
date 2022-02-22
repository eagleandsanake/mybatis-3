package wx.org.intercepter.annotation;

import java.lang.annotation.*;
import java.math.BigDecimal;

/**
 * @author wuxin
 * @date 2022/02/22 01:04:28
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface BigDecimalScale {

  String value();

  int roundingMode() default BigDecimal.ROUND_HALF_UP;

  boolean activeIn() default false;

}
