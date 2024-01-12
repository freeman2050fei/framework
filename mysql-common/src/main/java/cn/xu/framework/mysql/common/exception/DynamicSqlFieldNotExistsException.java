package cn.xu.framework.mysql.common.exception;

/**
 * @Author xuguofei
 * @Date 2023/12/27
 * @Desc TODO
 **/
public class DynamicSqlFieldNotExistsException extends RuntimeException{

    public DynamicSqlFieldNotExistsException() {
        super();
    }

    public DynamicSqlFieldNotExistsException(String message) {
        super(message);
    }
}
