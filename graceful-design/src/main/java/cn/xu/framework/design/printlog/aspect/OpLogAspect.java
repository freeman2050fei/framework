package cn.xu.framework.design.printlog.aspect;

import cn.xu.framework.design.printlog.annotation.PrintAopLog;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author xuguofei
 * @Date 2021/11/29
 * @Desc 对@PrintAopLog注解的方法执行日志打印
 **/
@Aspect
@Slf4j
public class OpLogAspect {

    @Around("@annotation(printAopLog)")
    public Object aroundPoint(ProceedingJoinPoint pjp, PrintAopLog printAopLog) throws Throwable {
        Object result = null;
        Signature signature = pjp.getSignature();
        String methodName = null;
        Object[] args = null;
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            // 被切的方法
            Method method = methodSignature.getMethod();
            methodName = method.getName();
            args = pjp.getArgs();
        }
        long start = System.currentTimeMillis();
        try {
            result =  pjp.proceed();
        } finally {
            if(result!=null){
                String paramJson = (args!=null? JSON.toJSONString(args):"");
                if(printAopLog.paramFlag()&&StringUtils.isNotBlank(paramJson)){
                    paramJson = printAopLog.paramFlag()?replace(printAopLog, paramJson):paramJson;
                }
                String resultJson = result!=null? JSON.toJSONString(result):"";
                if(printAopLog.resultFlag()&&StringUtils.isNotBlank(resultJson)){
                    resultJson = printAopLog.resultFlag()?replace(printAopLog, resultJson):resultJson;
                }
                log.info(pjp.getTarget().getClass().getSimpleName()+"_"+methodName+"_end,{}, {},cost : {}",
                        paramJson,
                        resultJson,
                        (System.currentTimeMillis()-start));
            }
            return result;
        }
    }

    private static Map<String, String> keyMap = new ConcurrentHashMap<>();
    private static Map<String, String> replaceMap = new ConcurrentHashMap<>();


    private String replace(PrintAopLog printAopLog, String srcStr){
        if(StringUtils.isNotBlank(srcStr)){
            if(printAopLog.securityFields()!=null&&printAopLog.securityFields().length>0){
                String reg = null;
                String replaceStr = null;
                for (String key:printAopLog.securityFields()){
                    reg = keyMap.computeIfAbsent(key,  (String)-> key+".*?([,}$])");
                    replaceStr = replaceMap.computeIfAbsent(key,  (String)-> key+"\":\""+printAopLog.replaceStr()+"\"$1");
                    srcStr = srcStr.replaceAll(reg, replaceStr);
                }
            }
        }
        return srcStr;
    }
}
