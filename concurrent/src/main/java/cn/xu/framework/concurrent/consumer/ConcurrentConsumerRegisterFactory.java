package cn.xu.framework.concurrent.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author xuguofei
 * @Date 2023/11/16
 * @Desc TODO
 **/
@Configuration
@Slf4j
public class ConcurrentConsumerRegisterFactory implements ApplicationContextAware, InitializingBean {

    private Map<String, IConcurrentHandleConsumer> concurrentHashMapPool = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    public ConcurrentConsumerRegisterFactory() {
        log.info("ConcurrentConsumerRegisterFactory create success");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, IConcurrentHandleConsumer> concurrentHandleConsumerMap = applicationContext.getBeansOfType(IConcurrentHandleConsumer.class);
        if(concurrentHandleConsumerMap!=null&&concurrentHandleConsumerMap.size()>0){
            for (Map.Entry<String, IConcurrentHandleConsumer> entry: concurrentHandleConsumerMap.entrySet()){
                concurrentHashMapPool.put(entry.getValue().getRoute(), entry.getValue());
            }
        }
    }

    public IConcurrentHandleConsumer getSourceConsumer(String key){
        return concurrentHashMapPool.get(key);
    }
}
