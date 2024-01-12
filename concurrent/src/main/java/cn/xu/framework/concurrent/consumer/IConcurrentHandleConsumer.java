package cn.xu.framework.concurrent.consumer;

/**
 * @Author xuguofei
 * @Date 2023/11/16
 * @Desc TODO
 **/
public interface IConcurrentHandleConsumer {

    ThreadLocal contextTl = new ThreadLocal<>();

    void lockFailHandle(String message, String tags, String keys);

    boolean getLock(String existKey, String lockValue);

    void unLockLastValue(String existKey, String lockValue);

    boolean preCheck(String delayTagRoute) throws Exception;

    void callCoreAfterLock(String delayTagRoute) throws Exception;

    String generateLockKey(String delayTagRoute);

    void callFramework(String message, String keys, String tags, Integer reconsumeTimes) throws Exception;

    String preHandleMessage(String message);

    Object convertDataModel(String message, String delayTagRoute) throws Exception;

    String getRoute();
}
