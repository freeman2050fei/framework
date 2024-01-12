package cn.xu.framework.mysql.common.utils;

import cn.xu.framework.mysql.common.dao.DaoField;
import cn.xu.framework.mysql.common.exception.DynamicSqlQueryMapToBeanException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * @Author xuguofei clark2021@qq.com || WX clark_2023
 * @Date 2023/12/29
 * @Desc TODO
 **/
public class MapUtils {

    public static <T> T mapToJavaBean(Map<String, Object> map, Map<String, DaoField> fieldsMap, Class<T> clazz) throws DynamicSqlQueryMapToBeanException {
        try {
            T obj = clazz.newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if(fieldsMap.containsKey(key)&&value!=null){
                    Field field = fieldsMap.get(key).getField();
                    field.setAccessible(true);
                    if(StringUtils.isNotBlank(fieldsMap.get(key).getDateFormat()) && value instanceof Timestamp
                            && field.getType()==String.class){
                        field.set(obj, DateFormatUtils.format((Timestamp)value, fieldsMap.get(key).getDateFormat()));
                    }else if(StringUtils.isNotBlank(fieldsMap.get(key).getDateFormat()) && value instanceof Date
                            && field.getType()==String.class){
                        field.set(obj, DateFormatUtils.format((Date)value, fieldsMap.get(key).getDateFormat()));
                    }else {
                        field.set(obj, value);
                    }
                }
            }
            return obj;
        } catch (Exception e) {
            throw new DynamicSqlQueryMapToBeanException(clazz.getSimpleName()+" map转换类型异常", e);
        }
    }
}
