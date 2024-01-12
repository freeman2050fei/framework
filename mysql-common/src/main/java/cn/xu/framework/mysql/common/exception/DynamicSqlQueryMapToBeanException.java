package cn.xu.framework.mysql.common.exception;

/**
 * @Author xuguofei
 * @Date 2023/12/29
 * @Desc TODO
 **/
public class DynamicSqlQueryMapToBeanException extends RuntimeException{

    public DynamicSqlQueryMapToBeanException() {
        super();
    }

    public DynamicSqlQueryMapToBeanException(String message) {
        super(message);
    }

    public DynamicSqlQueryMapToBeanException(String message, Throwable e) {
        super(message, e);
    }

}
