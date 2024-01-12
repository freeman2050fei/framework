package cn.xu.framework.design.dynamicsql.script;


import cn.xu.framework.design.dynamicsql.Context;

public interface SqlFragment {
    boolean apply(Context context);

}
