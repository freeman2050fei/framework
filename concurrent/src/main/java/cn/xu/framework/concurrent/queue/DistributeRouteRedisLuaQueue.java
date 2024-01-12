package cn.xu.framework.concurrent.queue;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import redis.clients.jedis.JedisCluster;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.zip.CRC32;

/**
 * @Author xuguofei
 * @Date 2023/11/22
 * @Desc TODO
 **/
@Slf4j
public class DistributeRouteRedisLuaQueue {

    private String distributeRouteQueue_script_initQueue;
    private String distributeRouteQueue_script_resetQueue;
    private String distributeRouteQueue_script_regMachine;
    private String distributeRouteQueue_script_mgr;
    private String distributeRouteQueue_script_heartbeat;
    private String distributeRouteQueue_script_time;
    private String distributeRouteQueue_script_test;

    private final static String prefixMgr = "drq_mgr_";
    private final static String prefixInit = "drq_init_";
    private final static String prefix_d1 = "drq_q_last_";
    private final static String prefix_d2 = "drq_m_last_";
    private final static String prefix_d3 = "drq_m_qrel_";
    private final static String prefix_d4 = "drq_m_num_";


    private final static String prefix_queue = "drq_que_";

    @Value("${distributeRouteRedisLuaQueue.queue.qId:default}")
    private String queueAppId;
    @Value("${distributeRouteRedisLuaQueue.queue.lua.log:false}")
    private Boolean luaLogOnOff;

    @Value("${distributeRouteRedisLuaQueue.queue.num:8}")
    private Integer queueNum;
    @Value("${distributeRouteRedisLuaQueue.queue.max.size:50000}")
    private Integer queueMaxSize;
    @Value("${distributeRouteRedisLuaQueue.queue.max.size.rejectStrategy:AbortPolicy}")
    private String queueMaxSizeRejectStrategy;

    private final static String queueMaxSizeRejectStrategy_AbortPolicy = "AbortPolicy";
    private final static String queueMaxSizeRejectStrategy_DiscardPolicy = "DiscardPolicy";
    private final static String queueMaxSizeRejectStrategy_CallerRunsPolicy = "CallerRunsPolicy";
    private final static String queueMaxSizeRejectStrategy_DiscardOldestPolicy = "DiscardOldestPolicy";

    @Value("${distributeRouteRedisLuaQueue.queue.machine.offLineMillisecond:150000}")
    private Integer offLineMaxMillisecond;
    @Value("${distributeRouteRedisLuaQueue.queue.idle.Millisecond:300000}")
    private Integer queueIdleMillisecond;

    private volatile static boolean daemonRunning = false;

    private final static String daemonRunningLock = "lock";

    private JedisCluster jedisCluster;
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile static boolean initQueue = false;
    private volatile static boolean registerMachine = false;

    private static String machineId;

    static{
        //访问标识，同一个take数据的服务器，此标识不应改变，由此建立 服务器:队列 1:n 关系映射
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        machineId = (mxBean.getStartTime()+"-"+mxBean.getSystemProperties().get("PID")+"-"+mxBean.getName()).replaceAll(" ", "");
        if(machineId.length()>30){
            machineId = StringUtils.substring(machineId, 0, 30);
        }
        log.info("DistributeRouteRedisLuaQueue,generateMachineId,{}", machineId);
    }

    private static Cache<String,Set<String>> d3QmRelGuavaCache = CacheBuilder.newBuilder().maximumSize(100) // 设置缓存的最大容量
            .expireAfterWrite(60, TimeUnit.SECONDS) //
            .concurrencyLevel(Runtime.getRuntime().availableProcessors()) // 设置并发级别为cpu核心数
            .recordStats() // 开启缓存统计
            .build();


    @PostConstruct
    public void init(){
        initLuaScript();
    }

    public String test(JedisCluster jedisCluster){
        List<String> params = new ArrayList<>();
        params.add("test");
        String result = executeLua(jedisCluster, distributeRouteQueue_script_test, params, 1);
        return result;
    }

    /**
     * @param concurrentId 根据此标识路由对应的队列，存在并发的数据必须路由到同一个队列
     * @param jedisCluster
     * @param data
     * @return
     */
    public boolean pushEnd(String concurrentId, JedisCluster jedisCluster, DRQueueDto data, int... expireSeconds) {
        if(StringUtils.isBlank(concurrentId)||data==null
                ||StringUtils.isBlank(data.getDataType())||StringUtils.isBlank(data.getJsonData())){
            return false;
        }
        String queueName = this.routeQueueName(concurrentId, jedisCluster);
        Long queueSize = jedisCluster.llen(queueName);
        if(queueSize >= queueMaxSize&&queueMaxSizeRejectStrategy_AbortPolicy.equals(queueMaxSizeRejectStrategy)){
            throw new RuntimeException("queue length exceeds the limit "+ queueMaxSize);
        }
        jedisCluster.rpush(queueName, JSON.toJSONString(data));
        if(expireSeconds!=null&&expireSeconds.length>0){
            jedisCluster.expire(queueName, expireSeconds[0]);
        }
        return true;
    }

    /**
     * @param jedisCluster
     * @return
     */
    public List<DRQueueDto> takeFirst(JedisCluster jedisCluster) {
        this.initSetPipelineCluster(jedisCluster);
        String d3Prefix = generateShardPrefix(prefix_d3);
        String d3Key = d3Prefix + "_" + machineId;
        Set<String> queueSet = null;
        try {
            queueSet = d3QmRelGuavaCache.get(d3Key, () -> jedisCluster.smembers(d3Key));
        } catch (ExecutionException e) {
            log.error("d3QmRelGuavaCache,get,error,{}", d3Key, e);
            queueSet = null;
        }
        if(queueSet!=null&&queueSet.size()>0){
            List<DRQueueDto> datas = new ArrayList<>();
            queueSet.forEach(queueName->{
                try {
                    String popData = jedisCluster.lpop(queueName);
                    if(StringUtils.isNotBlank(popData)){
                        DRQueueDto dObj = JSON.parseObject(popData, DRQueueDto.class);
                        datas.add(dObj);
                    }
                } catch (Exception e) {
                    log.error(this.getClass().getSimpleName()+",lpop_error,{}", queueName, e);
                }
            });
            return datas;
        }
        return null;
    }

    public Set<String> getRedisQueues(JedisCluster jedisCluster) {
        this.initSetPipelineCluster(jedisCluster);
        String d3Prefix = generateShardPrefix(prefix_d3);
        String d3Key = d3Prefix + "_" + machineId;
        Set<String> queueSet = jedisCluster.smembers(d3Key);
        return queueSet;
    }

    public void resetQueue(Integer newQueueNum) {
        if(newQueueNum==0||jedisCluster==null){
            log.info(this.getClass().getSimpleName()+",resetQueue,fail,{}", newQueueNum);
            return;
        }
        String key1 = generateShardPrefix(prefix_d1);
        String key2 = generateShardPrefix(prefix_d2);
        String key3 = generateShardPrefix(prefix_d3);
        String key4 = generateShardPrefix(prefix_d4);
        List<String> params = new ArrayList<>();
        params.add(key1);
        params.add(key2);
        params.add(key3);
        params.add(key4);
        params.add(generatePrefix(prefix_queue));
        params.add(String.valueOf(newQueueNum));
        String result = executeLua(jedisCluster, distributeRouteQueue_script_resetQueue, params, 4);
        log.info(this.getClass().getSimpleName()+",resetQueue,{},{}", newQueueNum, result);
    }

    private void runDaemonTread() {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if(jedisCluster!=null){
                        if(!initQueue){
                            initQueue();
                            initQueue = true;
                        }
                        if(!registerMachine){
                            registerMachine();
                            registerMachine = true;
                        }
                        managerQueue();
                    }else{
                        log.info("runDaemonTread_1 waiting queue jedisCluster");
                    }
                } catch (Exception e) {
                    log.error(this.getClass().getSimpleName()+",runDaemonTread_1_error", e);
                }
            }
        }, 3, 75, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if(jedisCluster!=null){
                        heartbeat();
                    }else{
                        log.info("runDaemonTread_2 waiting queue jedisCluster");
                    }
                } catch (Exception e) {
                    log.error(this.getClass().getSimpleName()+",runDaemonTread_2_error", e);
                }
            }
        }, 10, 45, TimeUnit.SECONDS);
    }

    private void initSetPipelineCluster(JedisCluster _jedisCluster) {
        if(daemonRunning){
            return;
        }
        synchronized (daemonRunningLock){
            if(!daemonRunning){
                jedisCluster = _jedisCluster;
                runDaemonTread();
                daemonRunning = true;
            }
        }
    }

    private String read(String file) throws IOException {
        // 获取当前类的类加载器
        ClassLoader classLoader = this.getClass().getClassLoader();
        // 获取指定文件的输入流
        InputStream inputStream = classLoader.getResourceAsStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String str = null;
        StringBuffer sb = new StringBuffer();
        while((str = br.readLine()) != null) {
            sb.append(str).append("\n\t");
        }
        br.close();
        inputStream.close();
        return sb.toString();
    }

    private void initLuaScript() {
        try {
            distributeRouteQueue_script_regMachine = this.read("lua/distributeRouteQueue_script_regMachine.lua").replaceAll("\n", " ");
            distributeRouteQueue_script_mgr = this.read("lua/distributeRouteQueue_script_mgr.lua").replaceAll("\n", " ");
            distributeRouteQueue_script_initQueue = this.read("lua/distributeRouteQueue_script_initQueue.lua").replaceAll("\n", " ");
            distributeRouteQueue_script_resetQueue = this.read("lua/distributeRouteQueue_script_resetQueue.lua").replaceAll("\n", " ");
            distributeRouteQueue_script_heartbeat = this.read("lua/distributeRouteQueue_script_heartbeat.lua").replaceAll("\n", " ");
            distributeRouteQueue_script_time = this.read("lua/distributeRouteQueue_script_time.lua").replaceAll("\n", " ");
            distributeRouteQueue_script_test = this.read("lua/distributeRouteQueue_script_test.lua").replaceAll("\n", " ");
        } catch (Exception e) {
            log.error("luaScript_load_from_classPath,error,{}", "distributeRouteQueue_script", e);
            throw new RuntimeException("!!!!!!!!!!!!!!!!!!,luaScript_load_error,!!!!!!!!!!!!!!!!!!", e);
        }
    }

    private void initQueue() {
        String key1 = generateShardPrefix(prefixInit);
        String key2 = generateShardPrefix(prefix_d1);
        List<String> params = new ArrayList<>();
        params.add(key1);
        params.add(key2);
        params.add(String.valueOf(queueNum));
        params.add(generatePrefix(prefix_queue));
        String initResult = executeLua(jedisCluster, distributeRouteQueue_script_initQueue, params, 2);
        log.info(this.getClass().getSimpleName()+",initQueue,{}", initResult);
    }

    /**
     * lua脚本中执行TIME命令后写入数据报错 Write commands not allowed after non deterministic commands 
     * @return
     */
    private String getCurrentRedisTime(){
        List<String> params = new ArrayList<>();
        params.add("time");
        String time = executeLua(jedisCluster, distributeRouteQueue_script_time, params, 1);
        if(time.contains(".")){
            time = StringUtils.substringBefore(time,".");
        }
        return time;
    }

    private void registerMachine() {
        String key1 = generateShardPrefix(prefix_d2);
        String key2 = generateShardPrefix(prefix_d4);
        List<String> params = new ArrayList<>();
        params.add(key1);
        params.add(key2);
        params.add(machineId);
        params.add(getCurrentRedisTime());
        String registerResult = executeLua(jedisCluster, distributeRouteQueue_script_regMachine, params, 2);
        log.info(this.getClass().getSimpleName()+",register,{}", registerResult);
    }

    private void heartbeat() {
        String key1 = generateShardPrefix(prefix_d1);
        String key2 = generateShardPrefix(prefix_d2);
        String key3 = generateShardPrefix(prefix_d3);
        String key4 = generateShardPrefix(prefix_d4);
        List<String> params = new ArrayList<>();
        params.add(key1);
        params.add(key2);
        params.add(key3);
        params.add(key4);
        params.add(machineId);
        params.add(getCurrentRedisTime());
        String initResult = executeLua(jedisCluster, distributeRouteQueue_script_heartbeat, params, 4);
        if(!"success".equalsIgnoreCase(initResult)){
            log.info("machine_rel_queue_heartbeat,error,{}", initResult);
        }
    }

    private void managerQueue() {
        String key1 = generateShardPrefix(prefixMgr);
        String key2 = generateShardPrefix(prefix_d1);
        String key3 = generateShardPrefix(prefix_d2);
        String key4 = generateShardPrefix(prefix_d3);
        String key5 = generateShardPrefix(prefix_d4);

        List<String> params = new ArrayList<>();
        params.add(key1);
        params.add(key2);
        params.add(key3);
        params.add(key4);
        params.add(key5);
        params.add(getCurrentRedisTime());
        params.add(String.valueOf(offLineMaxMillisecond));
        params.add(String.valueOf(queueIdleMillisecond));
        String result = executeLua(jedisCluster, distributeRouteQueue_script_mgr, params, 5);
        if(luaLogOnOff){
            log.info(this.getClass().getSimpleName()+",manager,{}", result);
        }
    }




    private String generateShardPrefix(String prefix){
        String key = prefix+"{"+queueAppId+"}";
        return key;
    }
    private String generatePrefix(String prefix){
        String key = prefix+queueAppId+"_";
        return key;
    }

    public String routeQueueName(String concurrentId, JedisCluster jedisCluster) {
        String keyD1 = generateShardPrefix(prefix_d1);
        Long queueNum = jedisCluster.zcard(keyD1);

        CRC32 crc32 = new CRC32();
        crc32.update(concurrentId.getBytes());
        Long queueIndex = null;
        try {
            queueIndex = (crc32.getValue() + queueNum.intValue()-1) % queueNum.intValue() + 1;
            if(queueIndex == null || queueIndex<=0L||queueIndex>queueNum){
                queueIndex = 1L;
            }
        } catch (Exception e) {
            log.error("crc32_mod_error,{}", concurrentId, e);
            queueIndex = 1L;
        }

        return generatePrefix(prefix_queue) + queueIndex;
    }

    private String executeLua(JedisCluster jedisCluster, String script, List<String> paramsList, Integer keyNum) {
        long start = System.currentTimeMillis();
        Object result = null;
        try {
            String[] params = new String[paramsList.size()];
            paramsList.toArray(params);
            result = jedisCluster.eval(script, keyNum, params);
            return (String) result;
        } finally {
            if(luaLogOnOff){
                log.info("executeLua,paramsList:{},cost:{},result:{}",
                        paramsList, (System.currentTimeMillis() - start), result);
            }
        }
    }
}
