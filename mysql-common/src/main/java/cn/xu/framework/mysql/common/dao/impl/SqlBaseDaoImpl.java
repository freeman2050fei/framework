package cn.xu.framework.mysql.common.dao.impl;

import cn.xu.framework.common.pojo.ConditionVo;
import cn.xu.framework.common.pojo.Pagination;
import cn.xu.framework.mysql.common.anaotation.DBMapper;
import cn.xu.framework.mysql.common.constant.SQLConstant;
import cn.xu.framework.mysql.common.dao.DaoField;
import cn.xu.framework.mysql.common.dao.SqlBaseDao;
import cn.xu.framework.mysql.common.exception.DynamicSqlFieldNotExistsException;
import cn.xu.framework.mysql.common.exception.DynamicSqlFieldsDuplicateException;
import cn.xu.framework.mysql.common.utils.MapUtils;
import cn.xu.framework.mysql.common.utils.SQLGenerateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @Author xuguofei clark2021@qq.com || WX clark_2023
 * @Date 2023/12/27
 * @Desc TODO
 **/
@Slf4j
public abstract class SqlBaseDaoImpl<T> implements SqlBaseDao<T> {

    @Value("${printSlowSqlLog:true}")
    private Boolean printSlowSqlLog;
    @Value("${printSlowSqlLogMaxCost:0}")
    private Integer printSlowSqlLogMaxCost;

    private Class<T> entityClass;

    private Map<String, DaoField> fieldNamesMap;
    private Set<String> fieldTbNames;
    private String tbName;

    public SqlBaseDaoImpl() {
        super();
        //反射获取对象类型
        this.entityClass = null;
        @SuppressWarnings("rawtypes")
        Class c = getClass();
        Type type = c.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type[] parameTypes = ((ParameterizedType) type)
                    .getActualTypeArguments();
            this.entityClass = (Class<T>) parameTypes[0];
            this.initMappingName();
        }
    }

    private void initMappingName() {
        tbName = entityClass.getSimpleName();
        DBMapper tbMapper = entityClass.getDeclaredAnnotation(DBMapper.class);
        boolean checkTbFieldDuplicate = false;
        if(tbMapper!=null){
            tbName = tbMapper.mapping();
            checkTbFieldDuplicate = tbMapper.checkTbFieldDuplicate();
        }
        Field[] fields = entityClass.getDeclaredFields();
        fieldNamesMap = new LinkedHashMap<>();
        fieldTbNames = new HashSet<>();
        for (Field field:fields) {
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }else if("serialVersionUID".equalsIgnoreCase(field.getName())){
                continue;
            }
            tbMapper = field.getDeclaredAnnotation(DBMapper.class);
            DaoField daoField = new DaoField();
            daoField.setField(field);
            if(tbMapper!=null){
                fieldTbNames.add(tbMapper.mapping());
                daoField.setFiledTbName(tbMapper.mapping());
                daoField.setDateFormat(tbMapper.dateFormat());
                fieldNamesMap.put(field.getName(), daoField);
            }else{
                fieldTbNames.add(field.getName());
                daoField.setFiledTbName(field.getName());
                fieldNamesMap.put(field.getName(), daoField);
            }
        }
        if(checkTbFieldDuplicate && fieldTbNames.size() < fieldNamesMap.size()){
            throw new DynamicSqlFieldsDuplicateException("table fields duplicate,"+fieldNamesMap);
        }
    }

    public static ThreadLocal<String> sqlContext = new ThreadLocal<>();

    private <V> V callBack(Callable<V> call) {
        long st = System.currentTimeMillis();
        try {
            return call.call();
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally {
            String sql = sqlContext.get();
            sqlContext.remove();
            long cost = System.currentTimeMillis()-st;
            if(printSlowSqlLog && !StringUtils.isEmpty(sql) &&cost> printSlowSqlLogMaxCost){
                log.info(this.getClass().getSimpleName() + ",printSlowSql : {}, cost : {}", sql, cost);
            }
        }
    }

    private <V> List<V> callBackList(Callable<List<V>> call) {
        long st = System.currentTimeMillis();
        try {
            return call.call();
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally {
            String sql = sqlContext.get();
            sqlContext.remove();
            long cost = System.currentTimeMillis()-st;
            if(printSlowSqlLog&& !StringUtils.isEmpty(sql) &&cost> printSlowSqlLogMaxCost){
                log.info(this.getClass().getSimpleName() + ",printSlowSql : {}, cost : {}", sql, cost);
            }
        }

    }

    @Override
    public T findOne(List<ConditionVo> conditionVos, String... fields) {
        return this.callBack(() -> {
            String sql = SqlBaseDaoImpl.this.generateQuerySql(conditionVos, fields);
            sqlContext.set(sql);
            List<Map<String, Object>> mapList = SqlBaseDaoImpl.this.getJdbcTemplate().queryForList(sql);
            if(mapList!=null&&mapList.size()>0){
                return MapUtils.mapToJavaBean(mapList.get(0), fieldNamesMap, entityClass);
            }
            return null;
        });
    }

    @Override
    public List<T> findList(List<ConditionVo> conditionVos, String... fields) {
        return this.callBackList(() -> {
            String sql = SqlBaseDaoImpl.this.generateQuerySql(conditionVos, fields);
            sqlContext.set(sql);
            List<Map<String, Object>> mapList = SqlBaseDaoImpl.this.getJdbcTemplate().queryForList(sql);
            if(mapList!=null&&mapList.size()>0){
                List<T> tList = mapList.stream()
                        .map(map -> MapUtils.mapToJavaBean(map, fieldNamesMap, entityClass))
                        .collect(Collectors.toList());
                return tList;
            }
            return null;
        });
    }

    @Override
    public long count(List<ConditionVo> conditionVos) {
        return this.callBack(() -> {
            String sql = SqlBaseDaoImpl.this.generateCountSql(conditionVos);
            sqlContext.set(sql);
            List<Long> counts = SqlBaseDaoImpl.this.getJdbcTemplate().query(sql, (rs, rowNum) -> rs.getLong(1));
            return CollectionUtils.isEmpty(counts) ? 0L : counts.get(0);
        });
    }

    @Override
    public Map<String, BigDecimal> sumQuery(String[] sumFields, List<ConditionVo> conditionVos) {
        if(sumFields==null||sumFields.length==0){
            throw new IllegalArgumentException("sumFields param not empty");
        }
        return this.callBack(() -> {
            String sql = SQLGenerateUtil.generateSumSql(tbName, sumFields, fieldNamesMap, conditionVos);
            sqlContext.set(sql);
            List<Map<String, Object>> mapList = SqlBaseDaoImpl.this.getJdbcTemplate().queryForList(sql);
            if(mapList!=null&&mapList.size()>0){
                Map<String,BigDecimal> resultMap = new HashMap<>();
                Map<String, Object> document = mapList.get(0);
                for(String sumField : sumFields){
                    if(document.get(sumField)==null){
                        resultMap.put(sumField, BigDecimal.ZERO);
                    }else if(document.get(sumField) instanceof Integer){
                        Integer sumFieldValue = (Integer) document.get(sumField);
                        resultMap.put(sumField, new BigDecimal(sumFieldValue));
                    }else if(document.get(sumField) instanceof Double
                            || document.get(sumField) instanceof Float){
                        Double sumFieldValue = (Double) document.get(sumField);
                        // 设置小数点后两位，并使用四舍五入的舍入模式
                        BigDecimal value =  new BigDecimal(sumFieldValue);
                        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
                        resultMap.put(sumField, rounded);
                    }else if(document.get(sumField) instanceof Long){
                        Long sumFieldValue = (Long) document.get(sumField);
                        resultMap.put(sumField, new BigDecimal(sumFieldValue));
                    }else if(document.get(sumField) instanceof String){
                        String sumFieldValue = (String) document.get(sumField);
                        resultMap.put(sumField, new BigDecimal(sumFieldValue));
                    }else{
                        resultMap.put(sumField, new BigDecimal(0));
                    }
                }
                return resultMap;
            }
            return null;
        });
    }

    @Override
    public Pagination<T> findPageConditions(int skip, int limit, long totalCount, List<ConditionVo> conditionVos, String[] ascs, String[] descs, String... fields) {
        long st = System.currentTimeMillis();
        try {
            if(limit == 0){
                limit = DEFAULT_PAGE_SIZE;
            }
            String sql = this.generateQuerySql(conditionVos, fields);

            StringBuilder sort = new StringBuilder();
            if(ascs!=null&&ascs.length>0){
                int i = 0;
                for(String asc:ascs){
                    if (!fieldNamesMap.containsKey(asc)) {
                        throw new DynamicSqlFieldNotExistsException("not find field name," + asc);
                    }
                    if(i!=0){
                        sort.append(",");
                    }
                    sort.append(asc);
                    i++;
                }
            }
            if(descs!=null&&descs.length>0){
                int i = 0;
                for(String desc:descs){
                    if (!fieldNamesMap.containsKey(desc)) {
                        throw new DynamicSqlFieldNotExistsException("not find field name," + desc);
                    }
                    if(i!=0){
                        sort.append(",");
                    }
                    sort.append(desc).append(SQLConstant.BLANK).append(SQLConstant.DESC);
                    i++;
                }
            }
            if(sort.length()>0){
                sql.concat(SQLConstant.ORDER_BY).concat(sort.toString());
            }

            //获取总条数
            if(totalCount<=0){
                totalCount = this.count(conditionVos);
            }
            //总页数
            Integer totalPage = (int) ((totalCount+limit-1)/limit);

            int currentPage = skip / limit + 1;
            // skip相当于从那条记录开始  limit从skip开始,取多少条记录
            sql.concat(SQLConstant.LIMIT).concat(String.valueOf(skip)).concat(String.valueOf(limit));

            sqlContext.set(sql);

            List<Map<String, Object>> mapList = this.getJdbcTemplate().queryForList(sql);
            List<T> tList = null;
            if(mapList!=null&&mapList.size()>0){
                tList = mapList.stream()
                        .map(map -> MapUtils.mapToJavaBean(map, fieldNamesMap, entityClass))
                        .collect(Collectors.toList());
            }
            Pagination<T> page = new Pagination<T>(entityClass, skip, limit, currentPage, totalPage, totalCount);
            page.build(tList);//获取数据
            return page;
        } finally {
            String sql = sqlContext.get();
            sqlContext.remove();
            long cost = System.currentTimeMillis()-st;
            if(printSlowSqlLog&& !StringUtils.isEmpty(sql) &&cost> printSlowSqlLogMaxCost){
                log.info(this.getClass().getSimpleName() + ",printSlowSql : {}, cost : {}", sql, cost);
            }
        }
    }

    @Override
    public int insert(T object) {
        return this.callBack(() -> {
            List<Object> params = new ArrayList<>();
            String sql = SQLGenerateUtil.generateInsertStatement(tbName, object, fieldNamesMap, params);
            sqlContext.set(sql);
            Object[] paramAry = params.toArray();
            int rowsAffected = SqlBaseDaoImpl.this.getJdbcTemplate().update(sql, paramAry);
            return rowsAffected;
        });
    }



    @Override
    public long update(Map<String, Object> whereParams, Map<String, Object> setParams) {
        if(whereParams==null||setParams==null||whereParams.size()==0||setParams.size()==0){
            throw new IllegalArgumentException("where and set param not empty");
        }
        return this.callBack(() -> {
            List<Object> params = new ArrayList<>();
            String sql = SQLGenerateUtil.generateUpdateStatement(tbName, whereParams, setParams, fieldNamesMap, params);
            sqlContext.set(sql);
            Object[] paramAry = params.toArray();
            int rowsAffected = SqlBaseDaoImpl.this.getJdbcTemplate().update(sql, paramAry);
            return rowsAffected;
        });
    }

    @Override
    public long updateConditionVo(List<ConditionVo> conditionVos, Map<String, Object> setParams) {
        if(conditionVos==null||setParams==null||conditionVos.size()==0||setParams.size()==0){
            throw new IllegalArgumentException("where and set param not empty");
        }
        return this.callBack(() -> {
            List<Object> params = new ArrayList<>();
            String sql = SQLGenerateUtil.generateUpdateStatement(tbName, conditionVos, setParams, fieldNamesMap, params);
            sqlContext.set(sql);
            Object[] paramAry = params.toArray();
            int rowsAffected = SqlBaseDaoImpl.this.getJdbcTemplate().update(sql, paramAry);
            return rowsAffected;
        });
    }

    @Override
    public long delete(List<ConditionVo> conditionVos) {
        if(conditionVos==null||conditionVos.size()==0){
            throw new IllegalArgumentException("where param not empty");
        }
        return this.callBack(() -> {
            String sql = SQLGenerateUtil.generateDelete(tbName, conditionVos, fieldNamesMap);
            sqlContext.set(sql);
            int rowsAffected = SqlBaseDaoImpl.this.getJdbcTemplate().update(sql);
            return rowsAffected;
        });
    }

    private String generateQuerySql(List<ConditionVo> conditionVos, String... fields) {
        StringBuilder statement = new StringBuilder();
        statement.append(SQLGenerateUtil.generateSelectFrom(tbName, fieldNamesMap, fields));
        SQLGenerateUtil.generateWhere(conditionVos, fieldNamesMap, statement);
        return statement.toString();
    }
    private String generateCountSql(List<ConditionVo> conditionVos) {
        StringBuilder statement = new StringBuilder();
        statement.append(SQLConstant.SELECT).append(SQLConstant.COUNT1).append(SQLConstant.FROM).append(tbName);
        SQLGenerateUtil.generateWhere(conditionVos, fieldNamesMap, statement);
        return statement.toString();
    }

}
