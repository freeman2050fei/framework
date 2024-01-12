package cn.xu.framework.design.dynamicsql;

import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;
import org.apache.commons.lang3.time.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Context {

    public static final String BINDING_DATA = "_data";

    static {
        OgnlRuntime.setPropertyAccessor(HashMap.class, new ContextAccessor());
    }

    private Configuration cfg;

    private Map<String, Object> binding;

    private StringBuilder sql = new StringBuilder();

    private List<Object> parameter;

    private int uniqueNumber = 0;

    public Context(Configuration cfg, Object data) {
        this.cfg = cfg;
        binding = new HashMap<String, Object>();
        binding.put("util", new DateUtils());
        parameter = new ArrayList<Object>();
        binding.put(BINDING_DATA, data);
    }

    public void bind(String key, Object value) {
        binding.put(key, value);
    }

    public void appendSql(String sqlFragement) {
        sql.append(sqlFragement).append(" ");
    }

    public Map<String, Object> getBinding() {
        return this.binding;
    }

    public List<Object> getParameter() {
        return this.parameter;
    }

    public void addParameter(Object parameter) {
        this.parameter.add(parameter);
    }

    public String getSql() {
        return sql.toString();
    }

    public void setSql(String sql) {
        this.sql = new StringBuilder(sql);
    }

    public int getUniqueNumber() {
        return ++uniqueNumber;
    }

    public Configuration getConfiguration() {
        return this.cfg;
    }

    static class ContextAccessor implements PropertyAccessor {

        @Override
        public Object getProperty(Map context, Object target, Object name)
                throws OgnlException {
            Map map = (Map)target;

            Object result = map.get(name);
            if (result != null) {
                return result;
            }

            Object parameterObject = map.get(BINDING_DATA);
            if (parameterObject instanceof Map) {
                return ((Map)parameterObject).get(name);
            }

            return null;
        }

        @Override
        public void setProperty(Map context, Object target, Object name,
                                Object value) throws OgnlException {
            Map map = (Map)target;
            map.put(name, value);
        }

        @Override
        public String getSourceAccessor(OgnlContext arg0, Object arg1,
                                        Object arg2) {
            return null;
        }

        @Override
        public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
            return null;
        }

    }

}
