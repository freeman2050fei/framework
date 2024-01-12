package cn.xu.framework.mysql.common.dao;

import cn.xu.framework.common.pojo.ConditionVo;
import cn.xu.framework.common.pojo.Pagination;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @Author xuguofei clark2021@qq.com || WX clark_2023
 * @Date 2023/12/27
 * @Desc TODO
 **/
public interface SqlBaseDao<T> {

    int DEFAULT_PAGE_SIZE = 5;

    T findOne(List<ConditionVo> conditionVos, String... fields);

    List<T> findList(List<ConditionVo> conditionVos, String...fields);

    long count(List<ConditionVo> conditionVos);

    Map<String, BigDecimal> sumQuery(String[] sumFields, List<ConditionVo> conditionVos);

    Pagination<T> findPageConditions(int skip, int limit, long totalCount, List<ConditionVo> conditionVos, String[] ascs, String[] descs, String...fields);

    int insert(T object);

    long update(Map<String, Object> whereParams, Map<String, Object> setParams);

    long updateConditionVo(List<ConditionVo> conditionVos, Map<String, Object> setParams);

    long delete(List<ConditionVo> conditionVos);

    JdbcTemplate getJdbcTemplate();
}
