package cn.xu.framework.mongo.common.dao;

import cn.xu.framework.common.pojo.ConditionVo;
import cn.xu.framework.common.pojo.Pagination;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**  
 * @Description: 实现MongoDB的CRUD操作通用基础接口
 * @author xuguofei
 * @date 2022年4月2日
 */
public interface MongoBaseDao<T> {

    //添加
    // 若新增数据的主键已经存在，则会抛 org.springframework.dao.DuplicateKeyException 异常提示主键重复，不保存当前数据。
    // 对于批操作  可以一次性插入一整个列表，而不用进行遍历操作，效率相对较高
    boolean insert(T object);

    boolean insert(T object, String collectionName);

    //添加  若新增数据的主键已经存在，则会对当前已经存在的数据进行修改操作。
    // 对于批操作  需要遍历列表，进行一个个的插入 -- 带分片的不支持？ com.mongodb.MongoWriteException: Failed to target upsert by query :: could not extract exact shard key
    @Deprecated
    void save(T object);
    @Deprecated
    void save(T object, String collectionName);

    long count(Query query);

    long count(Query query, String collectionName);

    T findOne(Map<String, Object> params);

    T findCollectionOne(Map<String, Object> params, String collectionName);

    T findOne(List<ConditionVo> conditionVos);

    List<T> findList(List<ConditionVo> conditionVos, String...fields);

    List<String> findDistinct(String distinctField, List<ConditionVo> conditionVos, String... fields);

    List<T> findList(Map<String, Object> params);

    List<T> findAll(Query query, String...fields);

    List<T> findCollectionAll(Query query, String collectionName, String...fields);

    List<T> findCritia(List<CriteriaDefinition> conditions, String...fields);

    List<T> findCollectionCritia(List<CriteriaDefinition> conditions, String collectionName, String...fields);
    
    List<T> findPage(int start, int pageSize, String...fields);

    Pagination<T> findPage(int skip, int limit, long totalCount, List<CriteriaDefinition> conditions, Sort sort, String[] ascs, String[] descs, String...fields);

    Pagination<T> findPageConditions(int skip, int limit, long totalCount, List<ConditionVo> conditionVos, String[] ascs, String[] descs, String...fields);

    Pagination<String> findPageDistinctFieldConditions(int skip, int limit, long totalCount, String distinctField, List<ConditionVo> conditionVos, String[] ascs, String[] descs, String...fields);

    /**
     * db.getCollection('ImageOrgUploadedDeclare').aggregate([
     *     {$match:{"$and":[
     *         {"opOrgCode" : {"$eq":"010017"}},
     *         {"operateTime" : {"$lte":"2023-11-19 23:59:59"}},
     *         {"operateTime" : {"$gte":"2023-11-17 00:00:00"}}]}},
     *     {$group:{"_id": {
     * 				"opOrgCode": "$opOrgCode",
     * 				"opOrgName": "$opOrgName",
     * 				"opOrgType": "$opOrgType"
     *                        }}}
     * ])
     * @param skip
     * @param limit
     * @param totalCount
     * @param groupFields
     * @param conditionVos
     * @param sortDesc
     * @param sortAsc
     * @param clazz
     * @return
     */
    Pagination<T> findDistinctGroupPageQuery(long skip, long limit, long totalCount, String[] groupFields,
                                             List<ConditionVo> conditionVos,
                                             String[] sortDesc, String[] sortAsc,
                                             Class<T> clazz);

    Pagination<T> groupsSumsPageQuery(long skip, long limit, long totalCount, String[] groupFields,
                                      String[] sumFields, String[] sumAlias, List<ConditionVo> conditionVos,
                                      String[] sortDesc, String[] sortAsc,
                                      Class<T> clazz);

    <T> List<T> groupsSumsTotalQuery(String[] groupFields, String[] sumFields, String[] sumAlias, List<ConditionVo> conditionVos, Class<T> clazz);

    List<T> findList(List<ConditionVo> conditionVos, String[] ascs, String[] descs);

    List<T> findListLimit(List<ConditionVo> conditionVos, String[] ascs, String[] descs, Integer limit);

    List<T> findListLimit(List<ConditionVo> conditionVos, String[] ascs, String[] descs, Integer limit, String...fields);

    /**
     * 无group sum query
     * 处理了当sum的字段是BigDecimal时无法sum的问题，故统一返回的都是转为BigDecimal
     * @param sumFields
     * @param sumAlias
     * @param conditionVos
     * @return
     */
    Map<String, BigDecimal> sumQuery(String[] sumFields, String[] sumAlias, List<ConditionVo> conditionVos);

    <T> List<T> groupSumQuery(String groupField, String sumField, String sumAlias, List<ConditionVo> conditionVos, Class<T> clazz);

    <T> List<T> groupsSumQuery(String[] groupFields, String sumField, String sumAlias, List<ConditionVo> conditionVos, Class<T> clazz);

    <T> List<T> groupCountQuery(String[] groupFields, String countAlias, List<ConditionVo> conditionVos, Class<T> clazz);

    long countUniqueValues(List<ConditionVo> conditionVos, String field);

    Pagination<T> findCollectionPage(int skip, int limit, long totalCount, List<CriteriaDefinition> conditions, Sort sort, String[] ascs, String[] descs, String collectionName, String...fields);

    //修改
    long update(Map<String, Object> whereParams, Map<String, Object> setParams);
    long update(Map<String, Object> whereParams, Map<String, Object> setParams, String collectionName);

    void updateConditionVo(List<ConditionVo> conditionVos, Map<String, Object> setParams);

    void incrConditionVo(List<ConditionVo> conditionVos, Map<String, Integer> incrParams);

    UpdateResult updateCritia(List<CriteriaDefinition> conditions, Map<String, Object> setParams);

    void updateCollectionCritia(List<CriteriaDefinition> conditions, Map<String, Object> setParams, String collectionName);
    //创建集合  
    void createCollection(String collectionName);
    //根据条件删除  
    void deleteById(String id);
    void deleteById(String id, String collectionName);
    //根据条件删除  
    void remove(Map<String, Object> params);
    void remove(Map<String, Object> params, String collectionName);
    
    Query createQuery(List<CriteriaDefinition> conditions, String... fields);

    List<CriteriaDefinition> getFilterCriteriaDefinitions(List<ConditionVo> conditionVos);

    long countByConditions(List<ConditionVo> conditionVos);

    long deleteByConditionVoList(List<ConditionVo> conditionVoList);
}
