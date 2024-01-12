package cn.xu.framework.concurrent.lock;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;

/**
 * @Author xuguofei
 * @Date 2021/3/16
 * @Desc 基于redis.clients JedisCluster实现分布式锁
 **/
@Slf4j
public class RedisDistributeLock {

    public static final String OK = "OK";

    //基于lua脚本删除key 保证原子操作
    private static final String RELEASE_LOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static final Long UNLOCK_OK = 1L;

    private static final ThreadLocal<String> uniqueValueTl = new ThreadLocal<>();

    /**
     * 加锁，必须调用unlock释放
     * @param key
     * @param seconds
     * @return
     */
    public static boolean tryLock(JedisCluster idempotentPipelineCluster, String key, int seconds){
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        uniqueValueTl.set(uuid);
        String isExist = idempotentPipelineCluster.set(key, uniqueValueTl.get(), SetParams.setParams().nx().ex(seconds));
        return OK.equalsIgnoreCase(isExist);
    }

    public static boolean unLock(JedisCluster idempotentPipelineCluster, String key){
        try {
            Object unlock = idempotentPipelineCluster.eval(RELEASE_LOCK_SCRIPT, Collections.singletonList(key), Collections.singletonList(uniqueValueTl.get()));
            return UNLOCK_OK.equals(unlock);
        } finally {
            uniqueValueTl.remove();
        }
    }

    /**
     * 加锁，可以依赖自动过期
     * @param key
     * @param seconds
     * @return
     */
    public static boolean tryLockExpire(JedisCluster idempotentPipelineCluster, String key, int seconds, String value){
        String isExist = idempotentPipelineCluster.set(key, value, SetParams.setParams().nx().ex(seconds));
        return OK.equalsIgnoreCase(isExist);
    }

    public static boolean unLockLastValue(JedisCluster idempotentPipelineCluster, String key, String oldValue){
        Object unlock = idempotentPipelineCluster.eval(RELEASE_LOCK_SCRIPT, Collections.singletonList(key), Collections.singletonList(oldValue));
        return UNLOCK_OK.equals(unlock);
    }


    /**
     * 加锁，可以依赖自动过期，获取不到时自旋等待一段时间
     * @param key
     * @param seconds
     * @return
     */
    public static boolean tryLockExpireTimeOut(JedisCluster idempotentPipelineCluster, String key, int seconds, String value, int timeOutMillis){
        String isExist = idempotentPipelineCluster.set(key, value, SetParams.setParams().nx().ex(seconds));
        long start = System.currentTimeMillis();
        while(!OK.equalsIgnoreCase(isExist)&&((System.currentTimeMillis()-start)<timeOutMillis)){
            try {
                //尽量避免非公平锁导致的线程竞争
                Thread.sleep(getRandom(5,100));
            } catch (InterruptedException e) {
                log.warn("tryLockTimeOut interrupted:", e);
            }
            isExist = idempotentPipelineCluster.set(key, value, SetParams.setParams().nx().ex(seconds));
        }
        return OK.equalsIgnoreCase(isExist);
    }

    public static long getRandom(int min, int max){
        Random random = new Random();
        int result = random.nextInt(max-min+1)+min;
        return (long)result;
    }

    /**
     * 加锁，可以依赖自动过期
     * @param key
     * @param seconds
     * @return
     */
    public static boolean tryLockExpire(JedisCluster idempotentPipelineCluster, String key, int seconds, String value, Jedis jedis){
        try {
            String isExist = jedis.set(key, value, SetParams.setParams().nx().ex(seconds));
            return OK.equalsIgnoreCase(isExist);
        } finally {
            //上层方法没有关闭连接，需要手动关闭
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static boolean unLockLastValue(JedisCluster idempotentPipelineCluster, String key, String oldValue, Jedis jedis){
        try {
            Object unlock = jedis.eval(RELEASE_LOCK_SCRIPT, Collections.singletonList(key), Collections.singletonList(oldValue));
            return UNLOCK_OK.equals(unlock);
        } finally {
            //上层方法没有关闭连接，需要手动关闭
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
