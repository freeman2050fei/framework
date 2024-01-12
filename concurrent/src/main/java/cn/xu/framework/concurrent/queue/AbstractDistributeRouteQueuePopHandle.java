package cn.xu.framework.concurrent.queue;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author xuguofei
 * @Date 2023/12/4
 * @Desc TODO
 **/
@Slf4j
public abstract class AbstractDistributeRouteQueuePopHandle implements InitializingBean {
    private static ScheduledExecutorService executor = null;

    private volatile static AtomicInteger popEmptyCount = new AtomicInteger(0);

    @Value("${distributeRouteQueuePopHandle.emptyIgnoreRunCount:3}")
    protected Integer emptyIgnoreRunCount;

    @Value("${distributeRedisQueuePop.takeOnOff:true}")
    protected Boolean takeOnOff;

    @Value("${distributeRedisQueuePop.take.handle.maxRetryCount:3}")
    protected Integer maxRetryCount;

    private final static String EMPTY_IGNORE_KEY = "queue_empty_pop_sleep";

    private static Cache<String, String> emptyIgnoreCache = CacheBuilder.newBuilder().maximumSize(1) // 设置缓存的最大容量
            .expireAfterWrite(15, TimeUnit.SECONDS) //
            .concurrencyLevel(Runtime.getRuntime().availableProcessors()) // 设置并发级别为cpu核心数
            .recordStats() // 开启缓存统计
            .build();

    @Override
    public void afterPropertiesSet() throws Exception {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if(popEmptyCount.get()>=emptyIgnoreRunCount){
                popEmptyCount.set(0);
                emptyIgnoreCache.put(EMPTY_IGNORE_KEY, "1");
            }
            if(StringUtils.isNotBlank(emptyIgnoreCache.getIfPresent(EMPTY_IGNORE_KEY))) {
                return;
            }
            Integer popCount = AbstractDistributeRouteQueuePopHandle.this.popHandle();
            if(popCount==0){
                popEmptyCount.addAndGet(1);
            }
        }, 5000, 20, TimeUnit.MILLISECONDS);
    }

    public Integer popHandle() {
        if(!takeOnOff){
            return 0;
        }
        List<DRQueueDto> drQueueDtos = this.takeFirst();
        Integer total = 0;
        if(drQueueDtos!=null&&drQueueDtos.size()>0){
            total = drQueueDtos.size();
            log.info("redisQueue,takeData,{}", JSON.toJSONString(drQueueDtos));
            drQueueDtos.forEach(qTo->{
                String jsonData = qTo.getJsonData();
                if(StringUtils.isNotBlank(jsonData)) {
                    if (qTo.getRetryCount() == null || qTo.getRetryCount() <= maxRetryCount) {
                        this.afterPopHandle(qTo);
                    }
                }
            });
        }
        return total;
    }

    protected abstract void afterPopHandle(DRQueueDto qTo);

    protected abstract List<DRQueueDto> takeFirst();
}
