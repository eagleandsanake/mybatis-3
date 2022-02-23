package wx.org.intercepter;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import wx.org.intercepter.annotation.BigDecimalScale;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author wuxin
 * @date 2022/02/22 00:46:36
 */
@Intercepts({
  @Signature(type = ResultSetHandler.class ,method = "handleResultSets",args = Statement.class),
  @Signature(type = Executor.class,method = "update",args = {MappedStatement.class,Object.class})
})
public class BigDecimalScaleInterceptor implements Interceptor {

  /**
   * Legal Rounding Mode
   */
  private static List<Integer> roundingMode;

  static {
    roundingMode = new ArrayList<Integer>(){{
      add(BigDecimal.ROUND_UP);
      add(BigDecimal.ROUND_DOWN);
      add(BigDecimal.ROUND_CEILING);
      add(BigDecimal.ROUND_FLOOR);
      add(BigDecimal.ROUND_HALF_UP);
      add(BigDecimal.ROUND_HALF_DOWN);
      add(BigDecimal.ROUND_HALF_EVEN);
      add(BigDecimal.ROUND_UNNECESSARY);
    }};
  }

  @Override
  public Object plugin(Object target) {
    return Interceptor.super.plugin(target);
  }

  @Override
  public void setProperties(Properties properties) {
    Interceptor.super.setProperties(properties);
  }

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object target = invocation.getTarget();
    if (target instanceof Executor) {
      return processExecutor(invocation);
    }
    if (target instanceof ResultSetHandler) {
      return processResultSet(invocation);
    }
    return invocation.proceed();
  }

  /**
   * Process result set to set BigDecimal Scale before return query data
   * @param invocation
   * @return
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  private Object processResultSet(Invocation invocation) throws InvocationTargetException, IllegalAccessException {
    List<Object> proceed = (List<Object>) invocation.proceed();
    if(Objects.nonNull(proceed) && proceed.size() != 0){
      proceed.stream().forEach(r->{
        Class<?> aClass = r.getClass();
        Field[] declaredFields = aClass.getDeclaredFields();
        for (Field f: declaredFields) {
          setScale(f,r);
        }
      });
    }
    return proceed;
  }

  /**
   *  Process executor to set BigDecimal Scale Before updated data from DB
   * @param invocation
   * @return
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  Object processExecutor(Invocation invocation) throws InvocationTargetException, IllegalAccessException {
    Object[] args = invocation.getArgs();
    if(Objects.nonNull(args) && args.length > 0){
      Object arg1 = args[1];
      if(arg1 instanceof Map){
        Map<String, Object> arg = (Map<String, Object>) args[1];
        arg.values().stream().forEach(r->{
          Class<?> rClass = r.getClass();
          Field[] declaredFields = rClass.getDeclaredFields();
          for (Field f: declaredFields) {
            setScale(f,r);
          }
        });
      } else {
        Field[] declaredFields = arg1.getClass().getDeclaredFields();
        for (Field f: declaredFields) {
          setScale(f,arg1);
        }
      }
    }
    return invocation.proceed();
  }

  private void setScale(Field f,Object r){
    BigDecimalScale annotation = f.getDeclaredAnnotation(BigDecimalScale.class);
    Class<?> type = f.getType();
    if(Objects.nonNull(annotation) && (type == BigDecimal.class)){
      checkAnnotationIsLegal(annotation);
      String scale = annotation.value();
      int mode = annotation.roundingMode();
      f.setAccessible(true);
      try {
        BigDecimal bigDecimal = (BigDecimal) f.get(r);
        f.set(r,bigDecimal.setScale(Integer.valueOf(scale),mode));
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  private void checkAnnotationIsLegal(BigDecimalScale scale){
    // Scale is valid
    String regex = "^[1-9]\\d*|0$";
    if(!Pattern.matches(regex,scale.value())){
      throw new RuntimeException("Scale must be numeric type and NonNegative Integer");
    }
    // RoundingMode is valid
    if(!roundingMode.contains(scale.roundingMode())){
      throw new RuntimeException("Rounding Mode illegal");
    }
  }

}
