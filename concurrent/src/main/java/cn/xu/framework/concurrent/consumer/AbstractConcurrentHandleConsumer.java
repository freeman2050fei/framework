package cn.xu.framework.concurrent.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * @Author xuguofei
 * @Date 2023/11/9
 * @Desc TODO
 **/
@Slf4j
public abstract class AbstractConcurrentHandleConsumer implements IConcurrentHandleConsumer{

    @Override
    public void callFramework(String message, String keys, String tags, Integer reconsumeTimes) throws Exception {
        String lockKey = null;
        boolean lockFlag = true;
        String lockValue = UUID.randomUUID().toString().replaceAll("-", "");
        try{
            if(StringUtils.isBlank(message)){
                log.warn("message_blank,{}", keys);
                return;
            }
            message = this.preHandleMessage(message);
            Object dataModel = this.convertDataModel(message, tags);
            contextTl.set(dataModel);
            if(!this.preCheck(tags)){
                return;
            }
            lockKey = this.generateLockKey(tags);
            lockFlag = this.getLock(lockKey, lockValue);
            if (!lockFlag){
                this.lockFailHandle(message, tags, keys);
                return;
            }
            this.callCoreAfterLock(tags);
        } finally {
            contextTl.remove();
            if (StringUtils.isNotBlank(lockKey) && lockFlag) {
                this.unLockLastValue(lockKey, lockValue);
            }
        }
    }

    public String preHandleMessage(String message){
        return message;
    }

    public String getRoute(){
        return this.toString();
    }

}
