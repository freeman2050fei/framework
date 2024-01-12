package cn.xu.framework.mongo.common.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

/**
 * @Author xuguofei
 * @Date 2022/4/2
 * @Desc MongoDB连接及配置信息
 **/
@Configuration
@ConditionalOnProperty({"spring.data.mongodb.hostAndPort"})
@Slf4j
public class MongoAutoConfiguration {

    @Value("${spring.data.mongodb.hostAndPort}")
    private String hostPort;
    @Value("${spring.data.mongodb.database}")
    private String database;
    @Value("${spring.data.mongodb.username}")
    private String username;
    @Value("${spring.data.mongodb.password}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database}")
    private String authenticationDatabase;

    @Value("${spring.application.name}")
    private String appName;

//    private final static String DB_URL_TEMPLATE = "mongodb://%s:%s@%s/%s?authSource=%s";
    private final static String DB_URL_TEMPLATE = "mongodb://%s:%s@%s/%s?authSource=%s&appName=%s&ssl=false";

    @Bean
    @Primary
    public MongoTemplate template() {
        return new MongoTemplate(factory());
    }

    @Bean("mongoDbFactory")
    public MongoDbFactory factory() {
        return new SimpleMongoDbFactory(client(), database);
    }

    @Bean("mongoClient")
    public MongoClient client() {
        // 生产集群模式连接
        // mongodb://ytsexp:B#gsxg#gb6F1s@mongo66.prd.db:22051,mongo64.prd.db:22051,mongo65.prd.db:22051/sexp?authSource=sexp&appName=xxx&ssl=false
        String dbUrl = String.format(DB_URL_TEMPLATE, username, password, hostPort, database, authenticationDatabase, appName);
        log.info("mongoClient.db.url,{}", dbUrl);
        MongoClientURI mongoClientURI = new MongoClientURI(dbUrl);
        MongoClient mongoClient = new MongoClient(mongoClientURI);
        return mongoClient;
    }
}
