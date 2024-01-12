package cn.xu.framework.mysql.common.dao;

import lombok.Data;

import java.lang.reflect.Field;

/**
 * @Author xuguofei clark2021@qq.com || WX clark_2023
 * @Date 2023/12/29
 * @Desc TODO
 **/
@Data
public class DaoField {
    private String filedTbName;
    private Field field;
    private String dateFormat;
}
