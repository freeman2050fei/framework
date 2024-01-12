package cn.xu.framework.mongo.common.document;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * @Description: mongo文档父类型
 * @author xuguofei
 * @date 2022年4月2日
 * @version V1.0  
 */
@Data
public abstract class BaseDoc {

	@Field("_id")
	private String id;

	//插入时间和修改时间每个doc都要有
	private String createTime;
	private String updateTime;

	//用于TTL过期
	private Date createDate;

	private String docDesc;
}
