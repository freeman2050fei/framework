package cn.xu.framework.design.dynamicsql.script;

import cn.xu.framework.design.dynamicsql.Context;

import java.util.List;

public class MixedSqlFragment implements SqlFragment {

    private List<SqlFragment> contents;

    public MixedSqlFragment(List<SqlFragment> contents) {
        this.contents = contents;
    }

    @Override
    public boolean apply(Context context) {

        for (SqlFragment sf : contents) {
            sf.apply(context);
        }

        return true;
    }

}
