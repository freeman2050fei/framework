package cn.xu.framework.design.dynamicsql;

import cn.hutool.db.sql.SqlFormatter;
import org.apache.commons.collections.MapUtils;

import java.util.List;
import java.util.Map;

public class DynamicSqlTemplateUtil {

    public static String getSql(String template, Map<String, Object> params) {
        Configuration configuration = new Configuration();
        SqlTemplate sqlTemplate = configuration
            .getTemplate(template);
        Bindings bind = new Bindings();

        if (MapUtils.isNotEmpty(params)) {
            params.forEach((k, v) -> bind.bind(k, v));
        }

        SqlMeta sqlMeta = sqlTemplate.process(bind);
        return buildSql(sqlMeta);
    }

    private static String buildSql(SqlMeta sqlMeta) {
        String template = sqlMeta.getSql();
        List<Object> parameter = sqlMeta.getParameter();
        for (Object param : parameter) {
            String replacement = "'" + param.toString() + "'";
            if (!(param instanceof String)) {
                replacement = param.toString();
            }
            template = template.replaceFirst("\\?", replacement);
        }
        return SqlFormatter.format(template);
    }
}
