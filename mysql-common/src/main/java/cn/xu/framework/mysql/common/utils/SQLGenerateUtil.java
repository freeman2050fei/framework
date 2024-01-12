package cn.xu.framework.mysql.common.utils;

import cn.xu.framework.common.constant.DataType;
import cn.xu.framework.common.constant.DateConstant;
import cn.xu.framework.common.constant.OperateType;
import cn.xu.framework.common.pojo.ConditionVo;
import cn.xu.framework.mysql.common.constant.SQLConstant;
import cn.xu.framework.mysql.common.dao.DaoField;
import cn.xu.framework.mysql.common.exception.DynamicSqlFieldNotExistsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author xuguofei clark2021@qq.com || WX clark_2023
 * @Date 2023/12/27
 * @Desc TODO
 **/
@Slf4j
public class SQLGenerateUtil {

    public static String generateSumSql(String tbName, String[] sumFields, Map<String, DaoField> fieldNamesMap, List<ConditionVo> conditionVos) {
        StringBuilder statement = new StringBuilder();
        statement.append(SQLConstant.SELECT);

        for(int i = 0;i<sumFields.length;i++){
            if(i!=0){
                statement.append(SQLConstant.COMMA);
            }
            DaoField daoField = fieldNamesMap.get(sumFields[i]);
            if(daoField==null){
                throw new DynamicSqlFieldNotExistsException(" not find field name," + sumFields[i]);
            }
            statement.append(String.format(SQLConstant.SUM, daoField.getFiledTbName()));
            statement.append(SQLConstant.AS).append(sumFields[i]);
        }
        statement.append(SQLConstant.FROM).append(tbName);
        generateWhere(conditionVos, fieldNamesMap, statement);
        return statement.toString();
    }

    public static String generateUpdateStatement(String tbName, Map<String, Object> whereParams,
                                                 Map<String, Object> setParams, Map<String, DaoField> fieldNamesMap,
                                                 List<Object> params) {
        StringBuilder statement = new StringBuilder();
        statement.append(SQLConstant.UPDATE).append(tbName).append(SQLConstant.SET);
        int i = 0;
        for(Map.Entry<String, Object> setEntry:setParams.entrySet()){
            if(i!=0){
                statement.append(SQLConstant.COMMA);
            }
            DaoField daoField = fieldNamesMap.get(setEntry.getKey());
            if(daoField==null){
                throw new DynamicSqlFieldNotExistsException(" not find field name," + setEntry.getKey());
            }
            statement.append(daoField.getFiledTbName()).append(SQLConstant.EQ).append(SQLConstant.QUESTION);
            params.add(setEntry.getValue());
            i++;
        }
        statement.append(SQLConstant.WHERE);
        int j = 0;
        for(Map.Entry<String, Object> whereEntry:whereParams.entrySet()){
            if(j!=0){
                statement.append(SQLConstant.AND);
            }
            DaoField daoField = fieldNamesMap.get(whereEntry.getKey());
            if(daoField==null){
                throw new DynamicSqlFieldNotExistsException(" not find field name," + whereEntry.getKey());
            }
            statement.append(daoField.getFiledTbName()).append(SQLConstant.EQ).append(SQLConstant.QUESTION);
            params.add(whereEntry.getValue());
            j++;
        }

        return statement.toString();
    }

    public static String generateUpdateStatement(String tbName, List<ConditionVo> whereConditions,
                                                 Map<String, Object> setParams, Map<String, DaoField> fieldNamesMap,
                                                 List<Object> params) {
        StringBuilder statement = new StringBuilder();
        statement.append(SQLConstant.UPDATE).append(tbName).append(SQLConstant.SET);
        int i = 0;
        for(Map.Entry<String, Object> setEntry:setParams.entrySet()){
            if(i!=0){
                statement.append(SQLConstant.COMMA);
            }
            DaoField daoField = fieldNamesMap.get(setEntry.getKey());
            if(daoField==null){
                throw new DynamicSqlFieldNotExistsException(" not find field name," + setEntry.getKey());
            }
            statement.append(daoField.getFiledTbName()).append(SQLConstant.EQ).append(SQLConstant.QUESTION);
            params.add(setEntry.getValue());
            i++;
        }
        generateWhere(whereConditions, fieldNamesMap, statement);
        return statement.toString();
    }

    public static String generateDelete(String tbName, List<ConditionVo> whereConditions,
                                                 Map<String, DaoField> fieldNamesMap) {
        StringBuilder statement = new StringBuilder();
        statement.append(SQLConstant.DELETE).append(tbName);
        generateWhere(whereConditions, fieldNamesMap, statement);
        return statement.toString();
    }

    public static String generateInsertStatement(String tbName, Object object,
                                                 Map<String, DaoField> fieldNamesMap,
                                                 List<Object> params) {
        String fBeanName = null;
        DaoField fTbField = null;
        int i = 0;
        StringBuilder statement = new StringBuilder();
        statement.append(SQLConstant.INSERT).append(tbName).append(SQLConstant.LEFT_);
        for(Map.Entry<String, DaoField> fieldsEntry:fieldNamesMap.entrySet()) {
            fBeanName = fieldsEntry.getKey();
            fTbField = fieldsEntry.getValue();
            Field field = fTbField.getField();
            field.setAccessible(true); // 如果字段是私有的，需要设置为可访问
            try {
                Object value = field.get(object);
                if(value!=null){
                    if(i!=0){
                        statement.append(SQLConstant.COMMA);
                    }
                    statement.append(fTbField.getFiledTbName());
                    params.add(value);
                    i++;
                }
            } catch (IllegalAccessException e) {
                throw new DynamicSqlFieldNotExistsException(object.getClass().getSimpleName()+" get field value error," + fBeanName);
            }
        }
        statement.append(SQLConstant.RIGHT_);
        statement.append(SQLConstant.VALUES).append(SQLConstant.LEFT_);
        for(int j = 0;j<params.size();j++){
            if(j!=0){
                statement.append(SQLConstant.COMMA);
            }
            statement.append(SQLConstant.QUESTION);
        }
        statement.append(SQLConstant.RIGHT_);
        return statement.toString();
    }

    public static String generateSelectFrom(String tbName, Map<String, DaoField> fieldNamesMap, String... fields) {
        StringBuilder statement = new StringBuilder();
        statement.append(SQLConstant.SELECT);
        String fBeanName = null;
        DaoField fTbField = null;
        Set<String> cusBeanFields = null;
        if(fields!=null&&fields.length>0){
            cusBeanFields = Arrays.stream(fields).collect(Collectors.toCollection(HashSet::new));
        }
        int i = 0;
        for(Map.Entry<String, DaoField> fieldsEntry:fieldNamesMap.entrySet()){
            fBeanName = fieldsEntry.getKey();
            fTbField = fieldsEntry.getValue();
            if(cusBeanFields!=null&&cusBeanFields.size()>0){
                if(cusBeanFields.contains(fBeanName)){
                    if(i!=0){
                        statement.append(SQLConstant.COMMA);
                    }
                    statement.append(fTbField.getFiledTbName()).append(SQLConstant.BLANK).append(fBeanName);
                    i++;
                }
            }else{
                if(i!=0){
                    statement.append(SQLConstant.COMMA);
                }
                statement.append(fTbField.getFiledTbName()).append(SQLConstant.BLANK).append(fBeanName);
                i++;
            }
        }
        statement.append(SQLConstant.FROM).append(tbName);
        return statement.toString();
    }

    public static void generateWhere(List<ConditionVo> conditionVos, Map<String, DaoField> fieldNamesMap, StringBuilder statement) {
        statement.append(SQLConstant.WHERE);
        if (conditionVos.size() == 1) {
            statement.append(generateConditionVo(conditionVos.get(0), fieldNamesMap));
        } else {
            int i = 0;
            for (ConditionVo conditionVo : conditionVos) {
                if (!OperateType.moreAnd.name().equals(conditionVo.getOperate()) && !OperateType.moreOr.name().equals(conditionVo.getOperate())) {
                    if (!fieldNamesMap.containsKey(conditionVo.getField())) {
                        throw new DynamicSqlFieldNotExistsException("not find field name," + conditionVo.getField());
                    }
                }
                if (i != 0) {
                    statement.append(SQLConstant.AND);
                }
                statement.append(SQLConstant.LEFT_).append(generateConditionVo(conditionVo, fieldNamesMap)).append(SQLConstant.RIGHT_);
                i++;
            }
        }
    }

    private static String generateConditionVo(ConditionVo conditionVo, Map<String, DaoField> fieldNamesMap) {
        String doOperate = conditionVo.getOperate();
        if(StringUtils.isBlank(doOperate)){
            return null;
        }
        // more类型的处理
        if(OperateType.moreOr.name().equalsIgnoreCase(doOperate)||OperateType.moreAnd.name().equalsIgnoreCase(doOperate)){
            if(conditionVo.getDetails()!=null && conditionVo.getDetails().size()>0){
                StringBuilder criteriaSb = new StringBuilder();
                if(conditionVo.getDetails().size()==1){
                    criteriaSb.append(crateCriteria(conditionVo.getDetails().get(0), fieldNamesMap));
                }else{
                    int i=0;
                    for (ConditionVo subContConditionVo : conditionVo.getDetails()) {
                        if(i!=0){
                            if(OperateType.moreOr.name().equalsIgnoreCase(doOperate)){
                                criteriaSb.append(SQLConstant.OR);
                            }else if(OperateType.moreAnd.name().equalsIgnoreCase(doOperate)){
                                criteriaSb.append(SQLConstant.AND);
                            }
                        }
                        criteriaSb.append(SQLConstant.LEFT_).append(generateConditionVo(subContConditionVo, fieldNamesMap)).append(SQLConstant.RIGHT_);
                        i++;
                    }
                }
                return criteriaSb.toString();
            }
            return null;
        }else{
            return crateCriteria(conditionVo, fieldNamesMap);
        }
    }

    private static String crateCriteria(ConditionVo conditionVo, Map<String, DaoField> fieldNamesMap) {
        Object dataValue = conditionVo.getValue();
        if(DataType.number.name().equalsIgnoreCase(conditionVo.getDataType())){
            dataValue = Double.parseDouble(conditionVo.getValue());
        }
        if(DataType.date.name().equalsIgnoreCase(conditionVo.getDataType())){
            if(conditionVo.getValue().length()<=10){
                //日期型补充后几位时间 yyyy-MM-dd
                if(OperateType.lt.name().equalsIgnoreCase(conditionVo.getOperate())||OperateType.lte.name().equalsIgnoreCase(conditionVo.getOperate())){
                    dataValue = dataValue + " 23:59:59";
                }
                if(OperateType.gt.name().equalsIgnoreCase(conditionVo.getOperate())||OperateType.gte.name().equalsIgnoreCase(conditionVo.getOperate())){
                    dataValue = dataValue + " 00:00:00";
                }
            }
        }
        if(DataType.ISODate.name().equalsIgnoreCase(conditionVo.getDataType())&&dataValue!=null){
            try {
                dataValue = DateUtils.parseDate(dataValue.toString(), new String[]{DateConstant.NORM_DATETIME_PATTERN, DateConstant.NORM_DATETIME_MS_PATTERN});
            } catch (ParseException e) {
                log.error("isoDate_parse_error,{}", dataValue, e);
            }
        }
        StringBuilder criteriaSb = new StringBuilder();
        if (OperateType.nu.name().equalsIgnoreCase(conditionVo.getOperate())) {
            //为null或为""
            criteriaSb.append(SQLConstant.LEFT_).append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.IS_NULL).append(SQLConstant.RIGHT_);
            criteriaSb.append(SQLConstant.OR);
            criteriaSb.append(SQLConstant.LEFT_).append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.EQ).append("\"\"").append(SQLConstant.RIGHT_);
            return criteriaSb.toString();
        }
        if (OperateType.nnu.name().equalsIgnoreCase(conditionVo.getOperate())) {
            //不为null且不为""
            criteriaSb.append(SQLConstant.LEFT_).append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.IS_NOT_NULL).append(SQLConstant.RIGHT_);
            criteriaSb.append(SQLConstant.OR);
            criteriaSb.append(SQLConstant.LEFT_).append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.NE).append("\"\"").append(SQLConstant.RIGHT_);
            return criteriaSb.toString();
        }
        if(OperateType.like.name().equalsIgnoreCase(conditionVo.getOperate())){
            //字符串或者原类型
            criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.LIKE).append(SQLConstant.QUOTATION)
                    .append(SQLConstant.PERCENTAGE).append(conditionVo.getValue()).append(SQLConstant.PERCENTAGE).append(SQLConstant.QUOTATION);
            return criteriaSb.toString();
        }
        if(OperateType.eq.name().equalsIgnoreCase(conditionVo.getOperate())){
            //字符串或者原类型
            criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.EQ).append(getCriteriaValue(conditionVo));
            return criteriaSb.toString();
        }
        if(OperateType.ne.name().equalsIgnoreCase(conditionVo.getOperate())){
            //字符串或者原类型
            criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.NE).append(getCriteriaValue(conditionVo));
            return criteriaSb.toString();
        }
        if(OperateType.gte.name().equalsIgnoreCase(conditionVo.getOperate())){
            //字符串或者原类型
            criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.GTE).append(getCriteriaValue(conditionVo));
            return criteriaSb.toString();
        }
        if(OperateType.lte.name().equalsIgnoreCase(conditionVo.getOperate())){
            //字符串或者原类型
            criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.LTE).append(getCriteriaValue(conditionVo));
            return criteriaSb.toString();
        }
        if(OperateType.gt.name().equalsIgnoreCase(conditionVo.getOperate())){
            //字符串或者原类型
            criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.GT).append(getCriteriaValue(conditionVo));
            return criteriaSb.toString();
        }
        if(OperateType.lt.name().equalsIgnoreCase(conditionVo.getOperate())){
            //字符串或者原类型
            criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.LT).append(getCriteriaValue(conditionVo));
            return criteriaSb.toString();
        }
        if(OperateType.in.name().equalsIgnoreCase(conditionVo.getOperate())){
            if(DataType.String.equals(conditionVo.getDataType())){
                criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.IN).
                        append(SQLConstant.LEFT_).append(toUniqueArrayStr(String.valueOf(conditionVo.getValue()),",")).append(SQLConstant.RIGHT_);
            }else{
                criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.IN).
                        append(SQLConstant.LEFT_).append(toUniqueArray(String.valueOf(conditionVo.getValue()),",")).append(SQLConstant.RIGHT_);
            }
            return criteriaSb.toString();
        }
        if(OperateType.nin.name().equalsIgnoreCase(conditionVo.getOperate())){
            if(DataType.String.name().equals(conditionVo.getDataType())){
                criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.NOT_IN).
                        append(SQLConstant.LEFT_).append(toUniqueArrayStr(String.valueOf(conditionVo.getValue()),",")).append(SQLConstant.RIGHT_);
            }else{
                criteriaSb.append(fieldNamesMap.get(conditionVo.getField()).getFiledTbName()).append(SQLConstant.NOT_IN).
                        append(SQLConstant.LEFT_).append(toUniqueArray(String.valueOf(conditionVo.getValue()),",")).append(SQLConstant.RIGHT_);
            }
            return criteriaSb.toString();
        }
        return null;
    }

    private static String getCriteriaValue(ConditionVo conditionVo) {
        if(DataType.String.name().equals(conditionVo.getDataType())||DataType.date.name().equals(conditionVo.getDataType())){
            return SQLConstant.QUOTATION+conditionVo.getValue()+SQLConstant.QUOTATION;
        }else{
            return conditionVo.getValue();
        }
    }

    private static List<String> toUniqueArray(String values, String split) {
        List<String> valueArray = new ArrayList<String>();
        if (StringUtils.isBlank(values) || StringUtils.isEmpty(split)) {
            return valueArray;
        } else {
            //敏感数据查询条件加密
            String[] valueList = StringUtils.split(values, split);
            for (String v : valueList) {
                if (StringUtils.isNotBlank(v)) {
                    if (!valueArray.contains(v)) {
                        valueArray.add(v);
                    }
                }
            }
            return valueArray;
        }
    }
    private static List<String> toUniqueArrayStr(String values, String split) {
        List<String> valueArray = new ArrayList<>();
        if (StringUtils.isBlank(values) || StringUtils.isEmpty(split)) {
            return valueArray;
        } else {
            //敏感数据查询条件加密
            String[] valueList = StringUtils.split(values, split);
            for (String v : valueList) {
                if (StringUtils.isNotBlank(v)) {
                    if (!valueArray.contains(v)) {
                        valueArray.add(SQLConstant.QUOTATION+v+SQLConstant.QUOTATION);
                    }
                }
            }
            return valueArray;
        }
    }

}
