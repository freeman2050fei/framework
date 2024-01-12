package cn.xu.framework.design.dynamicsql.script;


import cn.xu.framework.design.dynamicsql.Context;

public class IfFragment implements SqlFragment {

    private String test;

    private SqlFragment contents;

    private ExpressionEvaluator expression;

    public IfFragment(SqlFragment contents, String test) {

        this.expression = new ExpressionEvaluator();
        this.contents = contents;
        this.test = test;
    }

    @Override
    public boolean apply(Context context) {
        if (expression.evaluateBoolean(test, context.getBinding())) {

            this.contents.apply(context);

            return true;
        }
        return false;
    }

}
