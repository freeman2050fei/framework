package cn.xu.framework.common.pojo;

import lombok.Data;

import java.util.List;

/**
 * @Author xuguofei
 * @Date 2022/4/3
 * @Desc 分页查询对象
 **/
@Data
public class PagerConditionVo {

    private int start=0;
    private int limit=15;
    private long totalCount=0;
    private String[] fields;
    private List<ConditionVo> conditionVos;
    private String[] ascs;
    private String[] descs;

    /**
     * 分片字段值
     */
    private String shardingFieldValue;

    //调用方请求来源-预分配
    private String fromAppCode;

    /**
     * 起始时间 yyyy-MM-dd HH:mm:ss
     */
    private String startTime;
    /**
     * 结束时间 yyyy-MM-dd HH:mm:ss
     */
    private String endTime;
}