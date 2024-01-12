package cn.xu.framework.design.dynamicsql;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author xuguofei
 * @Date 2021/1/5
 * @Desc TODO
 **/
public class Test {

    public static void main(String[] args) {
//        sql1();
//        sql2();
        sql3();
    }

    private static void sql1() {
        String sqlTemplate = "select index_code,count(1) as index_cnt from package_metrics.package_metrics \n"
                + "where create_time between #{createStartTime} and #{createEndTime} \n"
                + "and product_id=#{projectCode} and \n"
                + "index_code in\n"
                + "<foreach collection=\"indexCodeList\" index=\"index\" item=\"item\" open=\"(\" separator=\",\" "
                + "close=\")\">\n"
                + "    #{item}\n"
                + "</foreach>\n"
                + "group by index_code";
        Map<String, Object> pMap = new HashMap<>();
        pMap.put("tablename", "t_user_log");
        pMap.put("splitColumnName", "projectCode");
        pMap.put("splitColumnValue", "13");
        pMap.put("indexCodeList", new Integer[]{3,33});
        String sql = DynamicSqlTemplateUtil.getSql(sqlTemplate, pMap);
        System.out.println(sql);
    }

    private static void sql2() {
        String sqlTemplate =
                " delete from ${tablename} "
                + "where create_time between #{createStartTime} and #{createEndTime} \n"
                + "and ${splitColumnName}=#{splitColumnValue} and \n"
                + "index_code in\n"
                + "<foreach collection=\"indexCodeList\" index=\"index\" item=\"item\" open=\"(\" separator=\",\" "
                + "close=\")\">\n"
                + "    #{item}\n"
                + "</foreach>\n"
                + "group by index_code";
        Map<String, Object> pMap = new HashMap<>();
        pMap.put("tablename", "t_user_log");
        pMap.put("createStartTime", 1);
        pMap.put("createEndTime", 200);
        pMap.put("splitColumnName", "projectCode");
        pMap.put("splitColumnValue", "13");
        pMap.put("indexCodeList", new Integer[]{3,33});
        String sql = DynamicSqlTemplateUtil.getSql(sqlTemplate, pMap);

        System.out.println(sql);
    }
    private static void sql3() {
        String sqlTemplate =
                " delete from &$&{tablename} "
                        + " where create_time  &gt;= str_to_date(&#&{startTime}, '%Y-%m-%d %H:%i:%s') "
                        + " and create_time &lt;= str_to_date(&#&{endTime}, '%Y-%m-%d %H:%i:%s') \n"
                        + " <if test=\" splitColumnValue != null and splitColumnValue!='' \"> "
                        + "  and &$&{splitColumnName}=#{splitColumnValue} \n"
                        + " </if> "
                        + "and index_code in\n"
                        + "<foreach collection=\"indexCodeList\" index=\"index\" item=\"item\" open=\"(\" separator=\",\" "
                        + "close=\")\">\n"
                        + "    #{item}\n"
                        + "</foreach> ";
        sqlTemplate = StringUtils.replace(sqlTemplate, "&$&", "$");
        sqlTemplate = StringUtils.replace(sqlTemplate, "&#&", "#");
//        sqlTemplate = sqlTemplate.replaceAll("&$&", "$");
        System.out.println(sqlTemplate);
        Map<String, Object> pMap = new HashMap<>();
        pMap.put("tablename", "t_user_log");
        pMap.put("startTime", "2020-01-06 04:00:05");
        pMap.put("endTime", "2020-01-07 14:00:05");
        pMap.put("splitColumnName", "projectCode");
//        pMap.put("splitColumnValue", "00080659");
        pMap.put("indexCodeList", new Integer[]{3,33});

        String sql = DynamicSqlTemplateUtil.getSql(sqlTemplate, pMap);

        System.out.println(sql);
    }

}
