package cn.xu.framework.mysql.common.exception;

/**
 * @Author xuguofei
 * @Date 2023/12/27
 * @Desc TODO
 **/
public class DynamicSqlFieldsDuplicateException extends RuntimeException{

    public DynamicSqlFieldsDuplicateException() {
        super();
    }

    public DynamicSqlFieldsDuplicateException(String message) {
        super(message);
    }
}
