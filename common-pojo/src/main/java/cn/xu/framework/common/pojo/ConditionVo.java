package cn.xu.framework.common.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 条件
 * @author xuguofei
 */
@Data
public class ConditionVo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String field;
	private String operate;
	private String dataType;
	private String value;
	private String fieldCn;
	private List<ConditionVo> details = null;
	/**
	 * 储存需要做in筛选的数据字段(用于在缓存原始数据时是否需要将原有map中逗号分割数据转为list类型)
	 */
	private List<String> inOperateFields;
	public ConditionVo(){
		super();
	}
	public ConditionVo(String field, String operate, String dataType, String value) {
		this.field = field;
		this.operate = operate;
		this.dataType = dataType;
		this.value = value;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("字段编号：");
		buf.append(this.field);
		buf.append("字段名：");
		buf.append(this.fieldCn);
		buf.append("比较方式：");
		buf.append(this.operate);
		buf.append("比较对象：");
		buf.append(this.value);
		return buf.toString();
	}
	
//	public static void main(String[] args) {
//		String test = "[{field:col1,operate:'bigthen',value:'1000',fieldCn:'金额'},{fieldCn:'支付方式',field:col1,operate:'in',value:'支票,现金,转账'}]";
//		List<ConditionVo> tjs = GsonUtil.parseList(test, ConditionVo.class);
//
//		for (int i = 0; i < tjs.size(); i++) {
//			ConditionVo vo = tjs.get(i);
//			System.out.println(vo);
//		}
//	}


}
