package cn.xu.framework.common.constant;

/**
 * @Author xuguofei
 * @Date 2022/4/2
 * @Desc TODO
 **/
public enum OperateType {
    moreAnd,//且
    moreOr,//或
    lt,//小于
    lte,//小于等于
    gt,//大于
    gte,//大于等于
    like,//相似
    nu,//空 null或者""
    nnu,//非空 非null且非""
    eq,//等于
    ne,//不等于
    in,//在列表
    nin,//不在列表
    elemMatch;//内嵌查询
}
