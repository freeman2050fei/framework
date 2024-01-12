## 项目名称
### 通用的标准入参转mysql 标准DML执行公共模块
> 提供SqlBaseDao并实现公共方法
>>标准的select 单条、多条、分页查询实体方法
>>标准的实体insert、update、delete方法
>>指定字段的聚合查询方法



## 运行条件
###配置相关数据源，创建JdbcTemplate JdbcTemplateConfig 为示例配置
*使用dynamic-datasource-spring-boot-starter配置相关数据源
如下：
######数据源配置
spring.datasource.dynamic.primary=db1
######=============================================================db1配置===========================================================
spring.datasource.dynamic.datasource.db1.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.dynamic.datasource.db1.url=jdbc:mysql://127.0.0.1:3306/db1?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.dynamic.datasource.db1.username=test
spring.datasource.dynamic.datasource.db1.password=test@123
spring.datasource.dynamic.datasource.db1.druid.initial-size=5
spring.datasource.dynamic.datasource.db1.druid.max-active=30
spring.datasource.dynamic.datasource.db1.druid.min-idle=1
spring.datasource.dynamic.datasource.db1.druid.max-wait=6000
spring.datasource.dynamic.datasource.db1.druid.validation-query=SELECT 1
######spring.datasource.dynamic.datasource.db1.druid.test-on-borrow=true
######spring.datasource.dynamic.datasource.db1.druid.test-on-return=false
######spring.datasource.dynamic.datasource.db1.druid.test-while-idle=true
###### 配置一个连接在池中最小生存的时间，单位是毫秒
spring.datasource.dynamic.datasource.db1.druid.min-evictable-idle-time-millis=300000
spring.datasource.dynamic.datasource.db1.druid.max-evictable-idle-time-millis=300000
###### 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
spring.datasource.dynamic.datasource.db1.druid.time-between-eviction-runs-millis=600000
###### 打开PSCache，并且指定每个连接上PSCache的大小
spring.datasource.dynamic.datasource.db1.druid.pool-prepared-statements=false

######=============================================================db2配置===========================================================
spring.datasource.dynamic.datasource.db2.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.dynamic.datasource.db2.url=jdbc:mysql://127.0.0.1:3306/db1?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.dynamic.datasource.db2.username=test
spring.datasource.dynamic.datasource.db2.password=test@123
spring.datasource.dynamic.datasource.db2.druid.initial-size=5
spring.datasource.dynamic.datasource.db2.druid.max-active=30
spring.datasource.dynamic.datasource.db2.druid.min-idle=1
spring.datasource.dynamic.datasource.db2.druid.max-wait=6000
spring.datasource.dynamic.datasource.db2.druid.validation-query=SELECT 1
######spring.datasource.dynamic.datasource.db2.druid.test-on-borrow=true
######spring.datasource.dynamic.datasource.db2.druid.test-on-return=false
######spring.datasource.dynamic.datasource.db2.druid.test-while-idle=true
###### 配置一个连接在池中最小生存的时间，单位是毫秒
spring.datasource.dynamic.datasource.db2.druid.min-evictable-idle-time-millis=300000
spring.datasource.dynamic.datasource.db2.druid.max-evictable-idle-time-millis=300000
###### 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
spring.datasource.dynamic.datasource.db2.druid.time-between-eviction-runs-millis=600000
###### 打开PSCache，并且指定每个连接上PSCache的大小
spring.datasource.dynamic.datasource.db2.druid.pool-prepared-statements=false

mybatis.configuration.map-underscore-to-camel-case=true
mybatis.mapper-locations=classpath*:mapper/*.xml  

>具体的每个表对应的dao实现SqlBaseDao后，必须重写JdbcTemplate getJdbcTemplate();方法
如UserDao对应的实现UserDaoImpl
>需要依赖JdbcTemplateConfig
>@Repository("stationInfoDao")
>public class UserDaoImpl extends SqlBaseDaoImpl<User> implements UserDao {

>    @Resource(name = "db1JdbcTemplate")
>    private JdbcTemplate db1JdbcTemplate;

>    @Override
>    public JdbcTemplate getJdbcTemplate() {
>        return db1JdbcTemplate;
>    }
>}

>也可以通过DS注解, 这种配置不需要依赖JdbcTemplateConfig
>@Repository("stationInfoDao")
>@DS("db1")
>public class UserDaoImpl extends SqlBaseDaoImpl<User> implements UserDao {

>    @Resource
>    private JdbcTemplate jdbcTemplate;

>    @Override
>    public JdbcTemplate getJdbcTemplate() {
>        return jdbcTemplate;
>    }
>}

###表对应实体表名及字段映射使用@DBMapper注解
>实体字段为String类型的，配置了dateFormat日期转换格式的，并且查询结果为Timestamp或Date类型的，会转为对应格式字符串
>非String类型配置dateFormat无效

## 运行说明
> 说明如何运行和使用你的项目，建议给出具体的步骤说明
* 操作一
* 操作二
* 操作三  



## 测试说明
> 如果有测试相关内容需要说明，请填写在这里  



## 技术架构
> 使用的技术框架或系统架构图等相关说明，请填写在这里  


## 协作者
> 高效的协作会激发无尽的创造力，将他们的名字记录在这里吧
