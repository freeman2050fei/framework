package cn.xu.framework.common.pojo;

import lombok.Data;

import java.util.List;

/**
 * @Author xuguofei
 * @Date 2023/12/13
 * @Desc sum查询对象
 **/
@Data
public class SumConditionVo {

    private List<ConditionVo> conditionVos;

    /**
     * 分片字段值
     */
    private String shardingFieldValue;

    private String[] sumFields;

    /**
     * 起始时间 yyyy-MM-dd HH:mm:ss
     */
    private String startTime;
    /**
     * 结束时间 yyyy-MM-dd HH:mm:ss
     */
    private String endTime;

}