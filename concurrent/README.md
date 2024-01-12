#new create 20231117
#init

#分布式队列，实现异步情况下的并发与顺序处理

##DistributeRouteRedisLuaQueue 基于redis+lua的实现

###配置
* distributeRouteRedisLuaQueue.queue.qId=应用队列标识，队列的命名通配符，使得各个数据结构映射到redis分片的同一槽位 必须配置！！！！命名尽量简短
* distributeRouteRedisLuaQueue.queue.num=队列数目，默认8
* distributeRouteRedisLuaQueue.queue.max.size=单个队列长度，默认5W
* distributeRouteRedisLuaQueue.queue.max.size.rejectStrategy=队列超过指定长度拒绝策略，Discard、DiscardOldest、Exception，默认Discard 丢弃
* distributeRouteRedisLuaQueue.queue.machine.offLineMillisecond=机器超过多少毫秒未操作判定为下线状态，最小150秒，150000，应大于【心跳检测】执行间隔-45秒
* distributeRouteRedisLuaQueue.queue.idle.Millisecond=队列的空闲时间，超过多少毫秒判定为空闲队列，最小300秒，300000,重新分配机器,应大于机器的下线时长，否则队列重复分配
* distributeRouteRedisLuaQueue.queue.lua.log=false 是否开启队列调度执行lua参数日志打印，默认false

###数据结构
* q1,q2,q3...系统启动指定固定数目的redis队列LIST

* d1表: ZSET
标识每个队列的最后操作时间(long)
例：
q1:"9:10"
q2:"9:10"
q3:0
Q4:"9:15"
队列q3待分配机器，根据配置判断超过指定时长没有更新的队列也为待分配队列

* d2表: ZSET
存储机器最后从队列take操作时间(long)
例:
C1: 1700817293347
C2: 1700817291325

* d3表:SET key为机器标识  ？？？？换zset试试
存储每个机器分配到的队列
例:
C1:q1,q2
C2:q4

* d4表:ZSET
存储每个机器分配的队列数
例:
C1:2
C2:1

###操作
* 初始化
>应用系统启动后初次调用take，按配置生成d1,设置初始score为0,待分配机器状态。 同一个应用只会执行一次

* 注册机器
>应用系统启动后初次调用take，同一个机器启动后执行一次。
>更新d2,当前时间。
>查询d4,当前机器不存在则添加，初始化score=0。

* 定时调度分配机器（最好只有一台机器触发执行，加标志位锁控制）
>淘汰下线机器
>>lua 查询d2
>>lua 非0且根据配置判断超过指定时长机器判定为机器下线，更新删除d2-更新删除d3-更新删除d4

>为队列分配机器
>>lua 查询d1
>>lua 为0或根据配置判断超过指定时长为待分配机器队列
>>>lua 查询有结果-轮询队列 查询d4-获取score从小到大排列机器,分配机器，更新d3-更新d4-更新d1

>均衡机器与队列的分配关系（注意写入命令不能在一个非确定性命令之后-non deterministic commands,spop在lua执行中被认定为非确定性命令）
>>lua 查询d4
>>lua 先查询d4长度，队列总数/d4长度取整+1=n,再查询sore>=n的成员机器Cn
>>lua 查询机器Cn在d3表中的队列数，取sore-n个m个队列q1~qm出来，循环，查找d4表sore最小的元素，分配队列,更新d4、d3、d1

* 心跳检测
>java 定时执行
>lua 根据机器id查询d3,遍历查询结果，更新机器对应的d1中队列的lastTime
>lua 更新d2,对应机器id的lastTime

* 添加队列元素-获取目的队列
>java 根据队列元素的并发标识与队列数量计算获得目的队列

* 添加队列元素-向目的队列添加
>java 判断队列长度
>RPUSH 向目的队列添加元素到队列尾部

* 获取队列元素-获取目的队列
>JAVA 根据机器id,获得d3Key
>SMEMBERS 查询d3

* 获取队列元素-从目的队列获取元素
>LPOP 从队列头部获取元素

* 队列数变更重置-大队列数缩小后，虽然调度脚没问题，但队列里的数据需要迁移，比较费时间，所以不支持大改小!!!!
>清除d1,按配置生成d1,设置初始score为0,待分配机器状态
>查询d2,遍历清除d3
>查询d4,遍历重置score为0,待分配队列

###AbstractDistributeRouteQueuePopHandle 消费DistributeRouteRedisLuaQueue队列的抽象父类


