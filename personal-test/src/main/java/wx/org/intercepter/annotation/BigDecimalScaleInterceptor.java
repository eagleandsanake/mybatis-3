package wx.org.intercepter.annotation;

import org.apache.ibatis.annotations.One;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author wuxin
 * @date 2022/02/22 00:46:36
 */
@Intercepts({
 /* @Signature(type = ResultSetHandler.class ,method = "handleResultSets",args = Statement.class),
  @Signature(type = Executor.class,method = "update",args = {MappedStatement.class,Object.class}),*/
  @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
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
    if(target instanceof StatementHandler){
      StatementHandler statementHandler = realTarget(invocation.getTarget());
      MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
      MappedStatement mappedStatement = (MappedStatement) metaObject
        .getValue("delegate.mappedStatement");
      String id = mappedStatement.getId();
      int i = id.lastIndexOf(".");
      String mapper = id.substring(0, i);
      String methodName = id.substring(i + 1, id.length());
      Method[] declaredMethods = Class.forName(mapper).getDeclaredMethods();
      Method m = null;
      Map<Integer,BigDecimalScale> needProcess = new HashMap<>();
      for (Method e : declaredMethods) {
          if(e.getName().equals(methodName)){
              m = e;
            break;
          }
      }
      Annotation[][] parameterAnnotations = m.getParameterAnnotations();
      for (int j = 0; j < parameterAnnotations.length; j++) {
        for (int k = 0; k < parameterAnnotations[j].length; k++) {
          Annotation annotation = parameterAnnotations[j][k];
          if(annotation instanceof BigDecimalScale){
            needProcess.put(j,(BigDecimalScale) annotation);
          }
        }
      }
      BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
      Object paramObj = boundSql.getParameterObject();
      Map<String, Object> paramList = (Map<String, Object>) paramObj;
      List<String> strings = paramList.keySet().stream().collect(Collectors.toList());
      for (int p = 0; p < strings.size(); p+=2) {
        BigDecimalScale bigDecimalScale = needProcess.get(p);
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(paramList.get(strings.get(p)));
        objects.add(paramList.get(strings.get(p + 1)));


        // 外面有
        if(Objects.nonNull(bigDecimalScale)){
          checkAnnotationIsLegal(bigDecimalScale);
          String value = bigDecimalScale.value();
          int roundingMode = bigDecimalScale.roundingMode();
          for (Object o : objects) {
            if(o instanceof BigDecimal){
              ((BigDecimal) o).setScale(Integer.valueOf(value),roundingMode);
            }
            Field[] declaredFields = o.getClass().getDeclaredFields();
            for (int g = 0; g < declaredFields.length; g++) {
              setScale(declaredFields[g],o,bigDecimalScale);
            }
          }
        }
        for (Object e : objects) {
          Field[] declaredFields = e.getClass().getDeclaredFields();
          for (int s = 0; s < declaredFields.length; s++) {
            setScale(declaredFields[s],e);
          }
        }
      }


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

  public static <T> T realTarget(Object target) {
    if (Proxy.isProxyClass(target.getClass())) {
      MetaObject metaObject = SystemMetaObject.forObject(target);
      return realTarget(metaObject.getValue("h.target"));
    }
    return (T) target;
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

  private void setScale(Field f,Object r,BigDecimalScale bigDecimalScale){
    BigDecimalScale annotation = bigDecimalScale;
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
