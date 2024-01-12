package cn.xu.framework.design.strategy.aspect;

import cn.xu.framework.design.strategy.annotation.SelectSpecificService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Author xuguofei clark2021@qq.com || WX clark_2023
 * @Date 2022/7/6
 * @Desc
 **/
@Aspect
@Slf4j
public class SpfServiceAspect implements ApplicationContextAware {
    private Map<String, Object> specificServiceMap = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    @Value("${max.log.cost:30}")
    private long maxLogCost;

    @Value("${spf.warnExceptionNames:,YTOWarningException,DuplicateKeyException,}")
    private String warnExceptionNames;

    public SpfServiceAspect() {
    }

    @Pointcut("@annotation(cn.xu.framework.design.strategy.annotation.SelectSpecificService)")
    private void cutMethod() {
    }

    @Around("cutMethod()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        boolean success = true;
        long startTime = System.currentTimeMillis();
        Method targetMethod = this.getTargetMethod(proceedingJoinPoint);
        Object specificService = null;
        String targetMethodName = null;
        Object[] targetArgs = null;
        Parameter[] parameters = null;
        try {
            //！！！关键拿到路由
            parameters = targetMethod.getParameters();
            if (ArrayUtils.isEmpty(parameters)||parameters.length < 1) {
                throw new RuntimeException("SpecificService_is_allowed at most one param");
            }
            if(proceedingJoinPoint.getArgs()[0]==null){
                throw new RuntimeException("SpecificService_route is null");
            }
            String route = (String)proceedingJoinPoint.getArgs()[0];
            if(StringUtils.isBlank(route)){
                throw new RuntimeException("SpecificService_route is blank");
            }
            SelectSpecificService spfService = targetMethod.getAnnotation(SelectSpecificService.class);
            Class<?> specificTypeClass = spfService.specificServiceInterface();
            if (spfService == null || specificTypeClass == null) {
                return this.execute(proceedingJoinPoint);
            } else {
                specificService = this.specificServiceMap.computeIfAbsent(route+specificTypeClass.getSimpleName(), (clazz) -> {
                    try {
                        Map<String, ?> allSpecificServices = applicationContext.getBeansOfType(specificTypeClass);
                        Set<String> keys = allSpecificServices.keySet();
                        for (String key : keys) {
                            if(allSpecificServices.get(key).toString().equals(route)){
                                return allSpecificServices.get(key);
                            }
                        }
                        return null;
                    } catch (Exception var2) {
                        return null;
                    }
                });
                if(specificService == null){
                    return this.execute(proceedingJoinPoint);
                }
                targetMethodName = targetMethod.getName();
                Method sourceMethod = specificService.getClass().getMethod(targetMethodName, targetMethod.getParameterTypes());
                targetArgs = proceedingJoinPoint.getArgs();
                Object var8 = sourceMethod.invoke(specificService, proceedingJoinPoint.getArgs());
                return var8;
            }
        } catch (Throwable e) {
            success = false;
            String paramsStr = parameters!=null? JSON.toJSONString(targetArgs):"";
            if(e.getCause()!=null){
                if(warnExceptionNames.contains(","+e.getCause().getClass().getSimpleName()+",")){
                    log.warn("selectSpfService_aop_around_warn,{}, method : {}", e.getCause().getMessage(),targetMethod.getName());
                }else{
                    log.error("selectSpfService_aop_around_error, method : {}, p : {}",targetMethod.getName(), paramsStr, e);
                }
                throw e.getCause();
            }else{
                log.error("selectSpfService_aop_around_error, method : {}, p : {}",targetMethod.getName(), paramsStr, e);
            }
            throw e;
        }  finally {
            long cost = System.currentTimeMillis()-startTime;
            if(cost>maxLogCost) {
                if (specificService!=null) {
                    try {
                        log.info("selectSpfService_aop_around target : {}, method : {}, result : {}, cost : {}, args : {}",
                                specificService.getClass().getSimpleName(), targetMethodName, success,
                                cost, (targetArgs==null?"": JSON.toJSONString(targetArgs)));
                    } catch (Exception e) {
                        log.error("around_print_log_error", e);
                    }
                }else{
                    log.info("selectSpfService_aop_around result : {}, cost : {}", success, (System.currentTimeMillis()-startTime));
                }
            }

        }
    }

    public Object execute(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        return result;
    }

    private Method getTargetMethod(ProceedingJoinPoint pjp) throws NoSuchMethodException {
        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature)signature;
        Method agentMethod = methodSignature.getMethod();
        return pjp.getTarget().getClass().getMethod(agentMethod.getName(), agentMethod.getParameterTypes());
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}