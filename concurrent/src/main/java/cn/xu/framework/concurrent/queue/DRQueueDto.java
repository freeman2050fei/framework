package cn.xu.framework.concurrent.queue;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author xuguofei
 * @Date 2023/12/1
 * @Desc TODO
 **/
@Data
public class DRQueueDto implements Serializable {

    private String dataType;

    private String jsonData;

    private Integer retryCount = 0;
}
