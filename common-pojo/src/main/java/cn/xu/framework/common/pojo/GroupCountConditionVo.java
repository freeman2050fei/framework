package cn.xu.framework.common.pojo;

import lombok.Data;

import java.util.List;

/**
 * @Author xuguofei
 * @Date 2023/2/15
 * @Desc group查询对象
 **/
@Data
public class GroupCountConditionVo {

    private List<ConditionVo> conditionVos;

    /**
     * 分片字段值
     */
    private String shardingFieldValue;

    private String[] groupFields;

    /**
     * 起始时间 yyyy-MM-dd HH:mm:ss
     */
    private String startTime;
    /**
     * 结束时间 yyyy-MM-dd HH:mm:ss
     */
    private String endTime;

    //调用方请求来源-预分配
    private String fromAppCode;
}