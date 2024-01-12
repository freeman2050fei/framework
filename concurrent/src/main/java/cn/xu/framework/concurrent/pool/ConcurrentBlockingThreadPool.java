package cn.xu.framework.concurrent.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ConcurrentBlockingThreadPool {
	
	private static ThreadPoolExecutor pool = null;
	
	private static ScheduledExecutorService executor = null;
	
	static {
		String corePoolSize = System.getProperty("corePoolSize");
		String maximumPoolSize = System.getProperty("maximumPoolSize");
		String queueCapacity = System.getProperty("queueCapacity");
		String keepAliveSeconds = System.getProperty("keepAliveSeconds");
		pool = new ThreadPoolExecutor(
				StringUtils.isNotBlank(corePoolSize)? Integer.parseInt(corePoolSize):Runtime.getRuntime().availableProcessors()*6,
				StringUtils.isNotBlank(maximumPoolSize)? Integer.parseInt(maximumPoolSize):Runtime.getRuntime().availableProcessors()*6,
				StringUtils.isNotBlank(keepAliveSeconds)? Integer.parseInt(keepAliveSeconds):30,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(StringUtils.isNotBlank(queueCapacity)?Integer.parseInt(queueCapacity):50),
                new CustomThreadFactory(),
                new CustomRejectedExecutionHandler());
		executor = Executors.newScheduledThreadPool(1);
		//线程池监控
		executor.scheduleAtFixedRate(new CheckThreadPool(), 5, 300, TimeUnit.SECONDS);
	}
	
	public static void exe(Runnable runnable) {
		pool.execute(runnable);
	}
	
	public static ThreadPoolExecutor getPool() {
		return pool;
	}
	
}

class CustomThreadFactory implements ThreadFactory {
    private AtomicInteger count = new AtomicInteger(0);
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        String threadName = ConcurrentBlockingThreadPool.class.getSimpleName() + count.addAndGet(1);
        t.setName(threadName);
        return t;
    }
}

@Slf4j
class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        try {
            executor.getQueue().put(runnable);
        } catch (Exception e) {
        	log.error("CustomRejectedExecutionHandler,error,", e);
        }
    }
}

@Slf4j
class CheckThreadPool implements Runnable {
    @Override
    public void run() {
    	log.info("【 wait_thread_num : {"+ ConcurrentBlockingThreadPool.getPool().getQueue().size()+"} 】");
		log.info("【 run_thread_num : {"+ ConcurrentBlockingThreadPool.getPool().getActiveCount()+"} 】");
		log.info("【 end_thread_num : {"+ ConcurrentBlockingThreadPool.getPool().getCompletedTaskCount()+"} 】");
		log.info("【 total_thread_num : {"+ ConcurrentBlockingThreadPool.getPool().getTaskCount()+"} 】");
    }
}


