package cn.xu.framework.common.pojo;

import lombok.Data;
import java.util.List;

/**
 * @Title: Pagination.java
 * @Description: TODO(用一句话描述该文件做什么)
 * @author xuguofei
 * @date 2022年4月2日
 */
@Data
public class Pagination<T> {

	private Class<T> clazz;

    /** 每页显示条数 */
    private Integer pageSize = 10;
  
    /** 当前页 */  
    private Integer currentPage = 1;
  
    /** 总页数 */  
    private Integer totalPage = 1;
    
    /** 开始索引*/
    private Integer start;
  
    /** 查询到的总数据量 */  
    private Long totalNumber = 0L;
  
    /** 数据集 */  
    private List<T> items;

	private Boolean success = true;
	private String errorCode;
	private String errorMsg;
  
    private Class getClazz(){
    	return clazz;
	}
    
    private Pagination() {
		super();
	}

	public Pagination(Class<T> clazz){
		this.clazz = clazz;
	}

	public Pagination(Class<T> clazz, Boolean success, String errorMsg){
		this.clazz = clazz;
		this.success = success;
		this.errorMsg = errorMsg;
	}

	public Pagination(Class<T> clazz, List<T> items){
		this.clazz = clazz;
		this.items = items;
	}

	public Pagination(Class<T> clazz, Integer pageSize, Integer currentPage, Integer totalPage, Long totalNumber) {
		super();
		this.clazz = clazz;
		this.pageSize = pageSize;
		this.currentPage = currentPage;
		this.totalPage = totalPage;
		this.totalNumber = totalNumber;
	}
	
	public Pagination(Class<T> clazz, Integer skip, Integer pageSize, Integer currentPage, Integer totalPage, Long totalNumber) {
		super();
		this.clazz = clazz;
		this.start = skip;
		this.pageSize = pageSize;
		this.currentPage = currentPage;
		this.totalPage = totalPage;
		this.totalNumber = totalNumber;
	}

	/** 
     * 处理查询后的结果数据 
     *  
     * @param items 
     *            查询结果集 
     */
    public void build(List<T> items) {
        this.setItems(items);
		long count =  this.getTotalNumber();
        int divisor = (int) (count / this.getPageSize());
        int remainder = (int) (count % this.getPageSize());
        this.setTotalPage(remainder == 0 ? divisor == 0 ? 1 : divisor : divisor + 1);  
    }

}
