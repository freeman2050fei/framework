package cn.xu.framework.mongo.common.dao.impl;

import cn.xu.framework.common.constant.DateConstant;
import cn.xu.framework.common.constant.DataType;
import cn.xu.framework.common.constant.OperateType;
import cn.xu.framework.common.pojo.ConditionVo;
import cn.xu.framework.common.pojo.Pagination;
import cn.xu.framework.mongo.common.dao.MongoBaseDao;
import cn.xu.framework.mongo.common.document.BaseDoc;
import com.alibaba.fastjson.JSON;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @Description: 实现MongoDB的CRUD操作通用基础接口方法实现
 * @author xuguofei
 * @date 2022/4/2
 */
@Slf4j
public class MongoBaseDaoImpl<T> implements MongoBaseDao<T> {

	@Autowired
	protected MongoTemplate mongoTemplate;

	@Value("${printQueryLog:true}")
	private Boolean printQueryLog;
	@Value("${printQueryLogMaxCost:500}")
	private Long printQueryLogMaxCost;

	private Class<T> entityClass;

	@SuppressWarnings("unchecked")
	public MongoBaseDaoImpl() {
		super();
		//反射获取对象类型
		this.entityClass = null;
		@SuppressWarnings("rawtypes")
        Class c = getClass();
		Type type = c.getGenericSuperclass();
		if (type instanceof ParameterizedType) {
			Type[] parameTypes = ((ParameterizedType) type)
					.getActualTypeArguments();
			this.entityClass = (Class<T>) parameTypes[0];
		}
	}
	
	//每页显示五条  
    protected static final int DEFAULT_PAGE_SIZE = 5;

    
    @Override
    public long count(Query query) {
    	return this.mongoTemplate.count(query, entityClass);  
    }
    
    @Override
    public long count(Query query, String collectionName) {
    	return this.mongoTemplate.count(query, entityClass, collectionName);  
    }
    
    @Override
    public Pagination<T> findPage(int skip, int limit, long totalCount, List<CriteriaDefinition> conditions, Sort sort, String[] ascs, String[] descs, String...fields) {
    	long st = System.currentTimeMillis();
		Query query = null;
		try {
			if(limit == 0){
				limit = DEFAULT_PAGE_SIZE;
			}
			query = createQuery(conditions, fields);
			//获取总条数
			if(totalCount<=0){
				totalCount = (int)(this.count(query));
			}
			//总页数
			int totalPage = (int) (totalCount/limit);

			int currentPage = skip / limit + 1;
			query.skip(skip);// skip相当于从那条记录开始
			query.limit(limit);// 从skip开始,取多少条记录

			if(ascs!=null&&ascs.length>0){
				Direction direction= Direction.ASC;
				if(sort!=null){
					sort = sort.and(new Sort(direction, ascs));
				}else{
					sort = new Sort(direction, ascs);
				}
			}
			if(descs!=null&&descs.length>0){
				Direction direction= Direction.DESC;
				if(sort!=null){
					sort = sort.and(new Sort(direction, descs));
				}else{
					sort = new Sort(direction, descs);
				}
			}
			if(sort!=null){
				query.with(sort);
			}
			List<T> datas = this.mongoTemplate.find(query, entityClass);
			Pagination<T> page = new Pagination<T>(entityClass, skip, limit, currentPage, totalPage, totalCount);
			page.build(datas);//获取数据
			return page;
		} finally {
			long cost = System.currentTimeMillis()-st;
			if(printQueryLog&&query!=null&&cost>printQueryLogMaxCost){
				log.info(this.getClass().getSimpleName() + ",printQueryLog,findPage : {}, cost : {}", query.toString(), cost);
			}
		}
	}
	@Override
	public long countByConditions(List<ConditionVo> conditionVos) {
		long st = System.currentTimeMillis();
		Query query = null;
		try {
			List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
			query = createQuery(conditions, null);
			return this.count(query);
		} finally {
			long cost = System.currentTimeMillis()-st;
			if(printQueryLog&&query!=null&&cost>printQueryLogMaxCost){
				log.info(this.getClass().getSimpleName() + ",printQueryLog,countByConditions : {}, cost : {}", query.toString(), cost);
			}
		}
	}

	@Override
	public Pagination<T> findPageConditions(int skip, int limit, long totalCount, List<ConditionVo> conditionVos, String[] ascs, String[] descs, String... fields) {
		long st = System.currentTimeMillis();
		Query query = null;
		try {
			if(limit == 0){
				limit = DEFAULT_PAGE_SIZE;
			}
			List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
			query = createQuery(conditions, fields);
			//获取总条数
			if(totalCount<=0){
				totalCount = this.count(query);
			}
			//总页数
			Integer totalPage = (int) ((totalCount+limit-1)/limit);

			int currentPage = skip / limit + 1;
			query.skip(skip);// skip相当于从那条记录开始
			query.limit(limit);// 从skip开始,取多少条记录

			Sort sort = null;
			if(ascs!=null&&ascs.length>0){
				Direction direction= Direction.ASC;
				if(sort!=null){
					sort = sort.and(new Sort(direction, ascs));
				}else{
					sort = new Sort(direction, ascs);
				}
			}
			if(descs!=null&&descs.length>0){
				Direction direction= Direction.DESC;
				if(sort!=null){
					sort = sort.and(new Sort(direction, descs));
				}else{
					sort = new Sort(direction, descs);
				}
			}
			if(sort!=null){
				query.with(sort);
			}
			List<T> datas = this.mongoTemplate.find(query, entityClass);
			Pagination<T> page = new Pagination<T>(entityClass, skip, limit, currentPage, totalPage, totalCount);
			page.build(datas);//获取数据
			return page;
		} finally {
			long cost = System.currentTimeMillis()-st;
			if(printQueryLog&&query!=null&&cost>printQueryLogMaxCost){
				log.info(this.getClass().getSimpleName() + ",printQueryLog,findPageConditions : {}, cost : {}", query.toString(), cost);
			}
		}
	}

	@Override
	public Pagination<String> findPageDistinctFieldConditions(int skip, int limit, long totalCount, String distinctField, List<ConditionVo> conditionVos, String[] ascs, String[] descs, String... fields) {
		long st = System.currentTimeMillis();
		Query query = null;
		try {
			if(limit == 0){
				limit = DEFAULT_PAGE_SIZE;
			}
			List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
			query = createQuery(conditions, fields);
			//获取总条数
			if(totalCount<=0){
				totalCount = this.count(query);
			}
			//总页数
			Integer totalPage = (int) ((totalCount+limit-1)/limit);

			int currentPage = skip / limit + 1;
			query.skip(skip);// skip相当于从那条记录开始
			query.limit(limit);// 从skip开始,取多少条记录

			Sort sort = null;
			if(ascs!=null&&ascs.length>0){
				Direction direction= Direction.ASC;
				if(sort!=null){
					sort = sort.and(new Sort(direction, ascs));
				}else{
					sort = new Sort(direction, ascs);
				}
			}
			if(descs!=null&&descs.length>0){
				Direction direction= Direction.DESC;
				if(sort!=null){
					sort = sort.and(new Sort(direction, descs));
				}else{
					sort = new Sort(direction, descs);
				}
			}
			if(sort!=null){
				query.with(sort);
			}
			List<String> datas = this.mongoTemplate.findDistinct(query, distinctField, entityClass, String.class);
			Pagination<String> page = new Pagination<String>(String.class, skip, limit, currentPage, totalPage, totalCount);
			page.build(datas);//获取数据
			return page;
		} finally {
			long cost = System.currentTimeMillis()-st;
			if(printQueryLog&&query!=null&&cost>printQueryLogMaxCost){
				log.info(this.getClass().getSimpleName() + ",printQueryLog,findPageDistinctFieldConditions : {}, cost : {}", query.toString(), cost);
			}
		}
	}

	@Override
	public Pagination<T> findDistinctGroupPageQuery(long skip, long limit, long totalCount, String[] groupFields,
                                                    List<ConditionVo> conditionVos,
                                                    String[] sortDesc, String[] sortAsc,
                                                    Class<T> clazz) {

		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
		conditions.toArray(criteriaDefinitionsAry);
		Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
		List<AggregationOperation> aggregationOperations = new ArrayList<>();
		MatchOperation matchOperation = new MatchOperation(criteria);
		aggregationOperations.add(matchOperation);
		GroupOperation groupOperation = new GroupOperation(Fields.fields(groupFields));
		//获取总条数
		if(totalCount<=0){
			totalCount = this.groupTotal(matchOperation, groupOperation, clazz);
		}
		//总页数
		Integer totalPage = (int) ((totalCount+limit-1)/limit);
		long currentPage = skip / limit + 1;
		aggregationOperations.add(groupOperation);
		Sort sort = null;
		if(sortDesc!=null&&sortDesc.length>0){
			sort = new Sort(Direction.DESC, sortDesc);
		}
		if(sortAsc!=null&&sortAsc.length>0){
			if(sort!=null){
				sort = sort.and(new Sort(Direction.ASC, sortAsc));
			}else{
				sort = new Sort(Direction.ASC, sortAsc);
			}
		}
		if(sort!=null){
			aggregationOperations.add(new SortOperation(sort));
		}
		aggregationOperations.add(Aggregation.skip(skip));
		aggregationOperations.add(Aggregation.limit(limit));
		Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<T> output = mongoTemplate.aggregate(aggregation, entityClass, clazz);
		List<T> datas = output.getMappedResults();
		Pagination<T> page = new Pagination<T>(entityClass, (int)skip, (int)limit, (int)currentPage, totalPage, totalCount);
		page.build(datas);//获取数据
		return page;
	}

	@Override
	public Pagination<T> groupsSumsPageQuery(long skip, long limit, long totalCount, String[] groupFields,
                                             String[] sumFields, String[] sumAlias, List<ConditionVo> conditionVos,
                                             String[] sortDesc, String[] sortAsc,
                                             Class<T> clazz) {

		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
		conditions.toArray(criteriaDefinitionsAry);
		Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
		List<AggregationOperation> aggregationOperations = new ArrayList<>();
		MatchOperation matchOperation = new MatchOperation(criteria);
		aggregationOperations.add(matchOperation);
		GroupOperation groupOperation = new GroupOperation(Fields.fields(groupFields));
		if(sumFields!=null&&sumFields.length>0){
			for(int i = 0;i<sumFields.length;i++){
				String sf = sumFields[i];
				String sa = sumFields[i];
				if(sumAlias!=null&&sumAlias.length>i){
					sa = sumAlias[i];
				}
				groupOperation = groupOperation.sum(sf).as(sa);
			}
		}
		//获取总条数
		if(totalCount<=0){
			totalCount = this.groupTotal(matchOperation, groupOperation, clazz);
		}
		//总页数
		Integer totalPage = (int) ((totalCount+limit-1)/limit);
		long currentPage = skip / limit + 1;
		aggregationOperations.add(groupOperation);
		Sort sort = null;
		if(sortDesc!=null&&sortDesc.length>0){
			sort = new Sort(Direction.DESC, sortDesc);
		}
		if(sortAsc!=null&&sortAsc.length>0){
			if(sort!=null){
				sort = sort.and(new Sort(Direction.ASC, sortAsc));
			}else{
				sort = new Sort(Direction.ASC, sortAsc);
			}
		}
		if(sort!=null){
			aggregationOperations.add(new SortOperation(sort));
		}
		aggregationOperations.add(Aggregation.skip(skip));
		aggregationOperations.add(Aggregation.limit(limit));
		Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<T> output = mongoTemplate.aggregate(aggregation, entityClass, clazz);
		List<T> datas = output.getMappedResults();
		Pagination<T> page = new Pagination<T>(entityClass, (int)skip, (int)limit, (int)currentPage, totalPage, totalCount);
		page.build(datas);//获取数据
		return page;
	}

	@Override
	public <T> List<T> groupsSumsTotalQuery(String[] groupFields, String[] sumFields, String[] sumAlias, List<ConditionVo> conditionVos, Class<T> clazz) {
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
		conditions.toArray(criteriaDefinitionsAry);
		Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
		List<AggregationOperation> aggregationOperations = new ArrayList<>();
		MatchOperation matchOperation = new MatchOperation(criteria);
		aggregationOperations.add(matchOperation);
		GroupOperation groupOperation = new GroupOperation(Fields.fields(groupFields));
		if(sumFields!=null&&sumFields.length>0){
			for(int i = 0;i<sumFields.length;i++){
				String sf = sumFields[i];
				String sa = sumFields[i];
				if(sumAlias!=null&&sumAlias.length>i){
					sa = sumAlias[i];
				}
				groupOperation = groupOperation.sum(sf).as(sa);
			}
		}
		aggregationOperations.add(groupOperation);
		Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<T> output = mongoTemplate.aggregate(aggregation, entityClass, clazz);
		return output.getMappedResults();
	}

	private <T> long groupTotal(MatchOperation matchOperation, GroupOperation groupOperation, Class<T> clazz) {
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<T> output = mongoTemplate.aggregate(aggregation, entityClass, clazz);
		return output.getMappedResults().size();
	}

	@Override
	public List<T> findList(List<ConditionVo> conditionVos, String[] ascs, String[] descs){
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Query query = createQuery(conditions);
		Sort sort = null;
		if(ascs!=null&&ascs.length>0){
			Direction direction= Direction.ASC;
			if(sort!=null){
				sort = sort.and(new Sort(direction, ascs));
			}else{
				sort = new Sort(direction, ascs);
			}
		}
		if(descs!=null&&descs.length>0){
			Direction direction= Direction.DESC;
			if(sort!=null){
				sort = sort.and(new Sort(direction, descs));
			}else{
				sort = new Sort(direction, descs);
			}
		}
		if(sort!=null){
			query.with(sort);
		}
		return this.mongoTemplate.find(query, entityClass);
	}

	@Override
	public List<T> findListLimit(List<ConditionVo> conditionVos, String[] ascs, String[] descs, Integer limit){
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Query query = createQuery(conditions);
		Sort sort = null;
		if(ascs!=null&&ascs.length>0){
			Direction direction= Direction.ASC;
			if(sort!=null){
				sort = sort.and(new Sort(direction, ascs));
			}else{
				sort = new Sort(direction, ascs);
			}
		}
		if(descs!=null&&descs.length>0){
			Direction direction= Direction.DESC;
			if(sort!=null){
				sort = sort.and(new Sort(direction, descs));
			}else{
				sort = new Sort(direction, descs);
			}
		}
		if(sort!=null){
			query.with(sort);
		}
		if(limit!=null){
			query.limit(limit);
		}
		return this.mongoTemplate.find(query, entityClass);
	}

	@Override
	public List<T> findListLimit(List<ConditionVo> conditionVos, String[] ascs, String[] descs, Integer limit, String... fields) {
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Query query = createQuery(conditions, fields);
		Sort sort = null;
		if(ascs!=null&&ascs.length>0){
			Direction direction= Direction.ASC;
			if(sort!=null){
				sort = sort.and(new Sort(direction, ascs));
			}else{
				sort = new Sort(direction, ascs);
			}
		}
		if(descs!=null&&descs.length>0){
			Direction direction= Direction.DESC;
			if(sort!=null){
				sort = sort.and(new Sort(direction, descs));
			}else{
				sort = new Sort(direction, descs);
			}
		}
		if(sort!=null){
			query.with(sort);
		}
		if(limit!=null){
			query.limit(limit);
		}
		return this.mongoTemplate.find(query, entityClass);
	}

	@Override
	public Map<String,BigDecimal> sumQuery(String[] sumFields, String[] sumAlias, List<ConditionVo> conditionVos) {
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
		conditions.toArray(criteriaDefinitionsAry);
		Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
		List<AggregationOperation> aggregationOperations = new ArrayList<>();

		aggregationOperations.add(new MatchOperation(criteria));
		GroupOperation groupOperation = Aggregation.group(Fields.fields());
		if(sumFields!=null&&sumFields.length>0){
			for(int i = 0;i<sumFields.length;i++){
				String sf = sumFields[i];
				String sa = sumFields[i];
				if(sumAlias!=null&&sumAlias.length>i){
					sa = sumAlias[i];
				}
				groupOperation = groupOperation.sum(new AggregationExpression() {
					@Override
					public Document toDocument(AggregationOperationContext context) {
//						return new Document("$toDouble", "$" + sf);
						return new Document("$toDecimal", "$" + sf);
					}
					@Override
					public String toString() {
						return sf;
					}
				}).as(sa);
//				groupOperation = groupOperation.sum(sf).as(sa);
			}
		}
		aggregationOperations.add(groupOperation);

		Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);

		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<Document> output = mongoTemplate.aggregate(aggregation, entityClass, Document.class);

		List<Document> resultDocuments = output.getMappedResults();
		if(resultDocuments!=null&&resultDocuments.size()>0){
			Map<String,BigDecimal> resultMap = new HashMap<>();
			Document document = resultDocuments.get(0);
			for(String sumField : sumFields){

				if(document.get(sumField) instanceof Integer){
					Integer sumFieldValue = document.getInteger(sumField, 0);
					resultMap.put(sumField, new BigDecimal(sumFieldValue));
				}else if(document.get(sumField) instanceof Decimal128
						|| document.get(sumField) instanceof Double
						|| document.get(sumField) instanceof Float){
					Decimal128 decimal128Value = document.get(sumField, Decimal128.class);
					BigDecimal bigDecimalValue = decimal128Value.bigDecimalValue();
					// 设置小数点后两位，并使用四舍五入的舍入模式
					BigDecimal rounded = bigDecimalValue.setScale(2, RoundingMode.HALF_UP);
					resultMap.put(sumField, rounded);
				}else if(document.get(sumField) instanceof Long){
					Long sumFieldValue = document.getLong(sumField);
					resultMap.put(sumField, new BigDecimal(sumFieldValue));
				}else if(document.get(sumField) instanceof String){
					resultMap.put(sumField, new BigDecimal(document.getString(sumField)));
				}else{
					resultMap.put(sumField, new BigDecimal(-1));
				}
			}
			return resultMap;
		}
		return null;
	}

	public <T> List<T> groupSumQuery(String groupField, String sumField, String sumAlias, List<ConditionVo> conditionVos, Class<T> clazz){
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
		conditions.toArray(criteriaDefinitionsAry);
		Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
		List<AggregationOperation> aggregationOperations = new ArrayList<>();
		aggregationOperations.add(new MatchOperation(criteria));
		aggregationOperations.add(new GroupOperation(Fields.fields(groupField)).sum(sumField).as(sumAlias));
		aggregationOperations.add(new SortOperation(new Sort(Direction.DESC, sumAlias)));
		Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<T> output = mongoTemplate.aggregate(aggregation, entityClass, clazz);
		return output.getMappedResults();
	}

	public <T> List<T> groupsSumQuery(String[] groupFields, String sumField, String sumAlias, List<ConditionVo> conditionVos, Class<T> clazz){
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
		conditions.toArray(criteriaDefinitionsAry);
		Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
		List<AggregationOperation> aggregationOperations = new ArrayList<>();
		aggregationOperations.add(new MatchOperation(criteria));
		aggregationOperations.add(new GroupOperation(Fields.fields(groupFields)).sum(sumField).as(sumAlias));
		aggregationOperations.add(new SortOperation(new Sort(Direction.DESC, sumAlias)));
		Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<T> output = mongoTemplate.aggregate(aggregation, entityClass, clazz);
		return output.getMappedResults();
	}

	public <T> List<T> groupCountQuery(String[] groupFields, String countAlias, List<ConditionVo> conditionVos, Class<T> clazz){
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
		conditions.toArray(criteriaDefinitionsAry);
		Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
		List<AggregationOperation> aggregationOperations = new ArrayList<>();
		aggregationOperations.add(new MatchOperation(criteria));
		aggregationOperations.add(new GroupOperation(Fields.fields(groupFields)).count().as(countAlias));
		aggregationOperations.add(new SortOperation(new Sort(Direction.DESC, countAlias)));
		Aggregation aggregation = Aggregation.newAggregation(aggregationOperations);
		AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
		aggregation = aggregation.withOptions(options);
		AggregationResults<T> output = mongoTemplate.aggregate(aggregation, entityClass, clazz);
		return output.getMappedResults();
	}

	public long countUniqueValues(List<ConditionVo> conditionVos, String uniqueField) {
		long st = System.currentTimeMillis();
		Aggregation aggregation = null;
		try {
			List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
			Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
			conditions.toArray(criteriaDefinitionsAry);
			Criteria criteria = new Criteria().andOperator(criteriaDefinitionsAry);
			List<AggregationOperation> aggregationOperations = new ArrayList<>();
			aggregationOperations.add(new MatchOperation(criteria));
			aggregationOperations.add(new ProjectionOperation(Fields.fields(uniqueField)).and(uniqueField));
			aggregationOperations.add(new GroupOperation(Fields.fields(uniqueField)).count().as("count"));
			aggregationOperations.add(new GroupOperation(Fields.fields()).count().as("count"));

			Query query = new Query();
			query.fields().include("count");
			aggregation = Aggregation.newAggregation(aggregationOperations);

			AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
			aggregation = aggregation.withOptions(options);

			AggregationResults<Query> aQr = mongoTemplate.aggregate(aggregation, entityClass, Query.class);
			if(aQr!=null&&aQr.getRawResults()!=null&&aQr.getRawResults().get("results")!=null){
				List<Document> results = (List<Document>)aQr.getRawResults().get("results");
				if(results!=null&&results.size()>0){
					return results.get(0).getInteger("count");
				}
			}
			return 0;
		} finally {
			long cost = System.currentTimeMillis()-st;
			if(printQueryLog&&aggregation!=null&&cost>printQueryLogMaxCost){
				log.info(this.getClass().getSimpleName() + ",printQueryLog,countUniqueValues : {}, cost : {}", aggregation.toString(), cost);
			}
		}
	}


	public long deleteByConditionVoList(List<ConditionVo> conditionVos){
		long st = System.currentTimeMillis();
		Query query = null;
		try {
			List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
			query = createQuery(conditions, null);
			DeleteResult deleteResult = mongoTemplate.remove(query, entityClass);
			return deleteResult!=null?deleteResult.getDeletedCount():0;
		} finally {
			long cost = System.currentTimeMillis()-st;
			if(printQueryLog&&query!=null&&cost>printQueryLogMaxCost){
				log.info(this.getClass().getSimpleName() + ",printQueryLog,deleteByConditionVoList : {}, cost : {}", query.toString(), cost);
			}
		}
	}


	@Override
	public List<CriteriaDefinition> getFilterCriteriaDefinitions(List<ConditionVo> conditionVos) {
		if (conditionVos!=null&&conditionVos.size()>0) {
			List<CriteriaDefinition> criteriaDefinitions = new ArrayList<>();
			for (ConditionVo conditionVo : conditionVos) {
				CriteriaDefinition cd = this.getCriteriaDefinition(conditionVo);
				if(cd!=null){
					criteriaDefinitions.add(cd);
				}
			}
			return criteriaDefinitions;
		}else{
			return null;
		}
	}

	private CriteriaDefinition getCriteriaDefinition(ConditionVo conditionVo) {
		String doOperate = conditionVo.getOperate();
		if(StringUtils.isBlank(doOperate)){
			return null;
		}
		// more类型的处理
		if(OperateType.moreOr.name().equalsIgnoreCase(doOperate)||OperateType.moreAnd.name().equalsIgnoreCase(doOperate)||OperateType.elemMatch.name().equalsIgnoreCase(doOperate)){
			if(conditionVo.getDetails()!=null&&conditionVo.getDetails().size()>0){
				List<CriteriaDefinition> criteriaDefinitions = new ArrayList<>();
				for (ConditionVo subContConditionVo : conditionVo.getDetails()) {
					CriteriaDefinition tmp = this.getCriteriaDefinition(subContConditionVo);
					if(tmp!=null){
						criteriaDefinitions.add(tmp);
					}
				}
				if(criteriaDefinitions.size()>0){
					Criteria moreCriteria = new Criteria();
					Criteria[] cs = new Criteria[criteriaDefinitions.size()];
					criteriaDefinitions.toArray(cs);
					if(OperateType.moreAnd.name().equalsIgnoreCase(doOperate)){
						moreCriteria.andOperator(cs);
					}else if(OperateType.moreOr.name().equalsIgnoreCase(doOperate)){
						moreCriteria.orOperator(cs);
					}else{
						//嵌套字段查询
						//子属性都当成and查询
						moreCriteria.andOperator(cs);
						return Criteria.where(conditionVo.getField()).elemMatch(moreCriteria);
					}
					return moreCriteria;
				}
			}
		}
		return this.crateDetailCriteria(conditionVo);
	}



	private CriteriaDefinition crateDetailCriteria(ConditionVo conditionVo) {
		Object dataValue = conditionVo.getValue();
		if(DataType.number.name().equalsIgnoreCase(conditionVo.getDataType())){
			dataValue = Double.parseDouble(conditionVo.getValue());
		}
		if(DataType.date.name().equalsIgnoreCase(conditionVo.getDataType())){
			if(conditionVo.getValue().length()<=10){
				//日期型补充后几位时间 yyyy-MM-dd
				if(OperateType.lt.name().equalsIgnoreCase(conditionVo.getOperate())||OperateType.lte.name().equalsIgnoreCase(conditionVo.getOperate())){
					dataValue = dataValue + " 23:59:59";
				}
				if(OperateType.gt.name().equalsIgnoreCase(conditionVo.getOperate())||OperateType.gte.name().equalsIgnoreCase(conditionVo.getOperate())){
					dataValue = dataValue + " 00:00:00";
				}
			}
		}
		if(DataType.ISODate.name().equalsIgnoreCase(conditionVo.getDataType())&&dataValue!=null){
			try {
				dataValue = DateUtils.parseDate(dataValue.toString(), new String[]{DateConstant.NORM_DATETIME_PATTERN, DateConstant.NORM_DATETIME_MS_PATTERN});
			} catch (ParseException e) {
				log.error("isoDate_parse_error,{}", dataValue, e);
			}
		}
		if (OperateType.nu.name().equalsIgnoreCase(conditionVo.getOperate())) {
			//为null或为""
			Criteria tc = new Criteria().orOperator(Criteria.where(conditionVo.getField()).is(null),Criteria.where(conditionVo.getField()).is(""));
			return tc;
		}
		if (OperateType.nnu.name().equalsIgnoreCase(conditionVo.getOperate())) {
			//不为null且不为""
			Criteria tc = new Criteria().norOperator(Criteria.where(conditionVo.getField()).is(null),Criteria.where(conditionVo.getField()).is(""));
			return tc;
		}
		if(OperateType.like.name().equalsIgnoreCase(conditionVo.getOperate())){
			//字符串或者原类型
			return Criteria.where(conditionVo.getField()).regex(conditionVo.getValue());
		}
		if(OperateType.eq.name().equalsIgnoreCase(conditionVo.getOperate())){
			//字符串或者原类型
//			Criteria tc = new Criteria().orOperator(Criteria.where(conditionVo.getField()).is(dataValue),Criteria.where(conditionVo.getField()).is(String.valueOf(dataValue)));
			Criteria tc = Criteria.where(conditionVo.getField()).is(dataValue);
			return tc;
		}
		if(OperateType.ne.name().equalsIgnoreCase(conditionVo.getOperate())){
			//字符串或者原类型
//			Criteria tc = new Criteria().orOperator(Criteria.where(conditionVo.getField()).ne(dataValue),Criteria.where(conditionVo.getField()).ne(String.valueOf(dataValue)));
			Criteria tc = Criteria.where(conditionVo.getField()).ne(dataValue);
			return tc;
		}
		if(OperateType.gte.name().equalsIgnoreCase(conditionVo.getOperate())){
			//字符串或者原类型
//			Criteria tc = new Criteria().orOperator(Criteria.where(conditionVo.getField()).gte(dataValue),Criteria.where(conditionVo.getField()).gte(String.valueOf(dataValue)));
			Criteria tc = Criteria.where(conditionVo.getField()).gte(dataValue);
			return tc;
		}
		if(OperateType.lte.name().equalsIgnoreCase(conditionVo.getOperate())){
			//字符串或者原类型
//			Criteria tc = new Criteria().orOperator(Criteria.where(conditionVo.getField()).lte(dataValue),Criteria.where(conditionVo.getField()).lte(String.valueOf(dataValue)));
			Criteria tc = Criteria.where(conditionVo.getField()).lte(dataValue);
			return tc;
		}
		if(OperateType.gt.name().equalsIgnoreCase(conditionVo.getOperate())){
			//字符串或者原类型
//			Criteria tc = new Criteria().orOperator(Criteria.where(conditionVo.getField()).gt(dataValue),Criteria.where(conditionVo.getField()).gt(String.valueOf(dataValue)));
			Criteria tc = Criteria.where(conditionVo.getField()).gt(dataValue);
			return tc;
		}
		if(OperateType.lt.name().equalsIgnoreCase(conditionVo.getOperate())){
			//字符串或者原类型
//			Criteria tc = new Criteria().orOperator(Criteria.where(conditionVo.getField()).lt(dataValue),Criteria.where(conditionVo.getField()).lt(String.valueOf(dataValue)));
			Criteria tc = Criteria.where(conditionVo.getField()).lt(dataValue);
			return tc;
		}
		if(OperateType.in.name().equalsIgnoreCase(conditionVo.getOperate())){
			return Criteria.where(conditionVo.getField()).in(this.toUniqueArray(conditionVo.getField(), String.valueOf(conditionVo.getValue()),","));
		}
		if(OperateType.nin.name().equalsIgnoreCase(conditionVo.getOperate())){
			return Criteria.where(conditionVo.getField()).nin(this.toUniqueArray(conditionVo.getField(), String.valueOf(conditionVo.getValue()),","));
		}
		return null;
	}

	private List<String> toUniqueArray(String field, String values, String split) {
		List<String> valueArray = new ArrayList<String>();
		if (StringUtils.isBlank(values) || StringUtils.isEmpty(split)) {
			return valueArray;
		} else {
			//敏感数据查询条件加密
			String[] valueList = StringUtils.split(values, split);
			for (String v : valueList) {
				if (StringUtils.isNotBlank(v)) {
					if (!valueArray.contains(v)) {
						valueArray.add(v);
					}
				}
			}
			return valueArray;
		}
	}

	public Query createQuery(List<CriteriaDefinition> conditions, String... fields) {
//		DBObject whereObject = new BasicDBObject();
//    	DBObject fieldObject = new BasicDBObject();
		Document whereObject = new Document();
		Document fieldObject = new Document();
    	if(fields!=null){
    		for (String filedNm : fields) {
    			fieldObject.put(filedNm, true);
			}
    	}
    	Query query = new BasicQuery(whereObject, fieldObject);
    	if(conditions!=null&&conditions.size()>0){
    		Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
    		conditions.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
    	}
		return query;
	}
    
    @Override
    public Pagination<T> findCollectionPage(int skip, int limit, long totalCount, List<CriteriaDefinition> conditions, Sort sort, String[] ascs, String[] descs, String collectionName, String...fields) {
    	Query query = createQuery(conditions, fields);
        //获取总条数  
        if(totalCount<=0){
        	totalCount = (int)(this.count(query, collectionName));
        }
		if(limit == 0){
			limit = DEFAULT_PAGE_SIZE;
		}
        //总页数  
        int totalPage = (int) (totalCount/limit);  
          
        int currentPage = skip / limit + 1;  
        query.skip(skip);// skip相当于从那条记录开始  
        query.limit(limit);// 从skip开始,取多少条记录  
        
        if(ascs!=null&&ascs.length>0){
        	Direction direction= Direction.ASC;
        	if(sort!=null){
        		sort = sort.and(new Sort(direction, ascs));
        	}else{
        		sort = new Sort(direction, ascs);
        	}
        }
        if(descs!=null&&descs.length>0){
        	Direction direction= Direction.DESC;
        	if(sort!=null){
        		sort = sort.and(new Sort(direction, descs));
        	}else{
        		sort = new Sort(direction, descs);
        	}
        }
        if(sort!=null){
        	query.with(sort);
        }
        List<T> datas = this.mongoTemplate.find(query, entityClass, collectionName);
        Pagination<T> page = new Pagination<T>(entityClass, skip, limit, currentPage, totalPage, totalCount);
        page.build(datas);//获取数据      
        return page;  
    }
    
    
	@Override
	public boolean insert(T object) {
		BaseDoc o = (BaseDoc) object;
		Date now = new Date();
		o.setUpdateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
		if(StringUtils.isBlank(o.getCreateTime())){
			o.setCreateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
		}
		if(o.getCreateDate()==null){
			o.setCreateDate(new Date());
		}
		mongoTemplate.insert(o);
		return true;
	}
	
	@Override
	public boolean insert(T object, String collectionName) {
		try {
			BaseDoc o = (BaseDoc) object;
			Date now = new Date();
			o.setUpdateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
			if(StringUtils.isBlank(o.getCreateTime())){
				o.setCreateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
			}
			if(o.getCreateDate()==null){
				o.setCreateDate(new Date());
			}
			mongoTemplate.insert(o, collectionName);
			return true;
		} catch (DuplicateKeyException e) {
			log.error("insert_duplicate,{}", JSON.toJSONString(object), e);
			return false;
		}
	}
	
	@Override
	public void save(T object) {
		BaseDoc o = (BaseDoc) object;
		Date now = new Date();
		o.setUpdateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
		if(StringUtils.isBlank(o.getCreateTime())){
			o.setCreateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
		}
		if(o.getCreateDate()==null){
			o.setCreateDate(new Date());
		}
		mongoTemplate.save(o);
	}
	
	@Override
	public void save(T object, String collectionName) {
		BaseDoc o = (BaseDoc) object;
		Date now = new Date();
		o.setUpdateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
		if(StringUtils.isBlank(o.getCreateTime())){
			o.setCreateTime(DateFormatUtils.format(now, DateConstant.NORM_DATETIME_PATTERN));
		}
		if(o.getCreateDate()==null){
			o.setCreateDate(new Date());
		}
		mongoTemplate.save(o, collectionName);
	}


	@Override
	public T findOne(Map<String, Object> params) {
		if(params!=null&&params.size()>0){
			Query query = new Query();  
    		List<Criteria> criterias = new ArrayList<>();
			for (Entry<String, Object> paramEntry : params.entrySet()) {
				criterias.add(Criteria.where(paramEntry.getKey()).is(paramEntry.getValue()));  
			}
			Criteria[] criteriaDefinitionsAry = new Criteria[criterias.size()];
			criterias.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
			return mongoTemplate.findOne(query, entityClass);   
		}
		return null;
	}

	@Override
	public List<T> findList(Map<String, Object> params) {
		if(params!=null&&params.size()>0){
			Query query = new Query();
			List<Criteria> criterias = new ArrayList<>();
			for (Entry<String, Object> paramEntry : params.entrySet()) {
				criterias.add(Criteria.where(paramEntry.getKey()).is(paramEntry.getValue()));
			}
			Criteria[] criteriaDefinitionsAry = new Criteria[criterias.size()];
			criterias.toArray(criteriaDefinitionsAry);
			query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
			return mongoTemplate.find(query, entityClass);
		}
		return null;
	}

	@Override
	public T findCollectionOne(Map<String, Object> params, String collectionName) {
		if(params!=null&&params.size()>0){
			Query query = new Query();  
    		List<Criteria> criterias = new ArrayList<>();
			for (Entry<String, Object> paramEntry : params.entrySet()) {
				criterias.add(Criteria.where(paramEntry.getKey()).is(paramEntry.getValue()));  
			}
			Criteria[] criteriaDefinitionsAry = new Criteria[criterias.size()];
			criterias.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
			return mongoTemplate.findOne(query, entityClass, collectionName);   
		}
		return null;
	}

	@Override
	public T findOne(List<ConditionVo> conditionVos) {
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Query query = createQuery(conditions);
		T data = this.mongoTemplate.findOne(query, entityClass);
		return data;
	}

	@Override
	public List<T> findList(List<ConditionVo> conditionVos, String...fields) {
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Query query = createQuery(conditions);
		if(fields!=null&&fields.length>0){
			Field queryFields = query.fields();
			for(String fieldName : fields){
				queryFields.include(fieldName);
			}
		}
		return this.mongoTemplate.find(query, entityClass);
	}

	public List<String> findDistinct(String distinctField, List<ConditionVo> conditionVos, String... fields) {
		List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
		Query query = createQuery(conditions);
		if(fields!=null&&fields.length>0){
			Field queryFields = query.fields();
			for(String fieldName : fields){
				queryFields.include(fieldName);
			}
		}
		return this.mongoTemplate.findDistinct(query, distinctField, entityClass, String.class);
	}

	@Override
	public List<T> findAll(Query query, String...fields) {
		List<T> result = mongoTemplate.find(query, entityClass);
		return result; 
	}

	@Override
	public List<T> findCollectionAll(Query query, String collectionName, String...fields) {
		List<T> result = mongoTemplate.find(query, entityClass, collectionName);
		return result;  
	}
	
	@Override
	public List<T> findCritia(List<CriteriaDefinition> conditions, String... fields) {
		Query query = createQuery(conditions, fields);
        List<T> datas = this.mongoTemplate.find(query, entityClass);
        return datas;
	}

	@Override
	public List<T> findCollectionCritia(List<CriteriaDefinition> conditions, String collectionName, String... fields) {
		Query query = createQuery(conditions, fields);
        List<T> datas = this.mongoTemplate.find(query, entityClass, collectionName);
        return datas;
	}
	
	@Override
	public List<T> findPage(int start, int pageSize, String...fields) {
		Query query = new Query();  
        query.skip(start);// skip相当于从那条记录开始  
        query.limit(pageSize);// 从skip开始,取多少条记录  
        List<T> datas = this.findAll(query);
		return datas;
	}

	@Override
	public long update(Map<String, Object> whereParams, Map<String, Object> setParams) {
		if(setParams!=null&&whereParams!=null&&setParams.size()>0&&whereParams.size()>0){
			Query query = new Query();  
    		List<Criteria> criterias = new ArrayList<>();
			for (Entry<String, Object> paramEntry : whereParams.entrySet()) {
				criterias.add(Criteria.where(paramEntry.getKey()).is(paramEntry.getValue()));  
			}
			Criteria[] criteriaDefinitionsAry = new Criteria[criterias.size()];
			criterias.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
    		
			Update update = new Update();
			for (Entry<String, Object> paramEntry : setParams.entrySet()) {
				update.set(paramEntry.getKey(), paramEntry.getValue());
			}
			update.set("updateTime", DateFormatUtils.format(new Date(), DateConstant.NORM_DATETIME_PATTERN));
			UpdateResult updateResult = mongoTemplate.updateMulti(query, update, entityClass);
			return updateResult.getModifiedCount();
		}
		return 0L;
	}

	@Override
	public void updateConditionVo(List<ConditionVo> conditionVos, Map<String, Object> setParams) {
		if(conditionVos!=null&&conditionVos!=null&&setParams.size()>0&&conditionVos.size()>0){
			List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
			Query query = createQuery(conditions);
			Update update = new Update();
			for (Entry<String, Object> paramEntry : setParams.entrySet()) {
				update.set(paramEntry.getKey(), paramEntry.getValue());
			}
			update.set("updateTime", DateFormatUtils.format(new Date(), DateConstant.NORM_DATETIME_PATTERN));
			mongoTemplate.updateMulti(query, update, entityClass);
		}
	}

	@Override
	public void incrConditionVo(List<ConditionVo> conditionVos, Map<String, Integer> incrParams){
		if(conditionVos!=null&&conditionVos!=null&&incrParams.size()>0&&conditionVos.size()>0){
			List<CriteriaDefinition> conditions = this.getFilterCriteriaDefinitions(conditionVos);
			Query query = createQuery(conditions);
			Update update = new Update();
			for (Entry<String, Integer> paramEntry : incrParams.entrySet()) {
				update.inc(paramEntry.getKey(), paramEntry.getValue());
			}
			update.set("updateTime", DateFormatUtils.format(new Date(), DateConstant.NORM_DATETIME_PATTERN));
			mongoTemplate.updateMulti(query, update, entityClass);
		}
	}

	@Override
	public long update(Map<String, Object> whereParams, Map<String, Object> setParams, String collectionName) {
		if(setParams!=null&&whereParams!=null&&setParams.size()>0&&whereParams.size()>0){
			Query query = new Query();  
    		List<Criteria> criterias = new ArrayList<>();
			for (Entry<String, Object> paramEntry : whereParams.entrySet()) {
				criterias.add(Criteria.where(paramEntry.getKey()).is(paramEntry.getValue()));  
			}
			Criteria[] criteriaDefinitionsAry = new Criteria[criterias.size()];
			criterias.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
    		
			Update update = new Update();
			for (Entry<String, Object> paramEntry : setParams.entrySet()) {
				update.set(paramEntry.getKey(), paramEntry.getValue());
			}
			update.set("updateTime", DateFormatUtils.format(new Date(), DateConstant.NORM_DATETIME_PATTERN));
			UpdateResult updateResult = mongoTemplate.updateMulti(query, update, entityClass, collectionName);
			return updateResult.getModifiedCount();
		}
		return 0L;
	}
	
	@Override
	public UpdateResult updateCritia(List<CriteriaDefinition> conditions, Map<String, Object> setParams) {
		long st = System.currentTimeMillis();
		Query query = null;
		try {
			if(setParams!=null&&conditions!=null&&setParams.size()>0&&conditions.size()>0){
				query = new Query();
				Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
				conditions.toArray(criteriaDefinitionsAry);
				query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));

				Update update = new Update();
				for (Entry<String, Object> paramEntry : setParams.entrySet()) {
					update.set(paramEntry.getKey(), paramEntry.getValue());
				}
				update.set("updateTime", DateFormatUtils.format(new Date(), DateConstant.NORM_DATETIME_PATTERN));
				return mongoTemplate.updateMulti(query, update, entityClass);
			}
			return null;
		} finally {
			long cost = System.currentTimeMillis()-st;
			if(printQueryLog&&query!=null&&cost>printQueryLogMaxCost){
				log.info(this.getClass().getSimpleName() + ",printQueryLog,query : {}, cost : {}", query.toString(), cost);
			}
		}
	}
	
	@Override
	public void updateCollectionCritia(List<CriteriaDefinition> conditions, Map<String, Object> setParams, String collectionName) {
		if(setParams!=null&&conditions!=null&&setParams.size()>0&&conditions.size()>0){
			Query query = new Query();  
			Criteria[] criteriaDefinitionsAry = new Criteria[conditions.size()];
			conditions.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
    		
			Update update = new Update();
			for (Entry<String, Object> paramEntry : setParams.entrySet()) {
				update.set(paramEntry.getKey(), paramEntry.getValue());
			}
			update.set("updateTime", DateFormatUtils.format(new Date(), DateConstant.NORM_DATETIME_PATTERN));
			mongoTemplate.updateMulti(query, update, entityClass, collectionName);
		}
	}

	@Override
	public void createCollection(String collectionName) {
		mongoTemplate.createCollection(collectionName);
	}

	@Override
	public void deleteById(String id) {
		mongoTemplate.remove(new Query(Criteria.where("_id").is(new ObjectId(id))), entityClass);
	}

	@Override
	public void deleteById(String id, String collectionName) {
		mongoTemplate.remove(new Query(Criteria.where("_id").is(new ObjectId(id))), entityClass, collectionName);
	}



	@Override
	public void remove(Map<String, Object> params) {
		if(params!=null&&params.size()>0){
			Query query = new Query();  
    		List<Criteria> criterias = new ArrayList<>();
			for (Entry<String, Object> paramEntry : params.entrySet()) {
				criterias.add(Criteria.where(paramEntry.getKey()).is(paramEntry.getValue()));  
			}
			Criteria[] criteriaDefinitionsAry = new Criteria[criterias.size()];
			criterias.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
    		
			mongoTemplate.remove(query,entityClass);
		}
	}

	@Override
	public void remove(Map<String, Object> params, String collectionName) {
		if(params!=null&&params.size()>0){
			Query query = new Query();  
    		List<Criteria> criterias = new ArrayList<>();
			for (Entry<String, Object> paramEntry : params.entrySet()) {
				criterias.add(Criteria.where(paramEntry.getKey()).is(paramEntry.getValue()));  
			}
			Criteria[] criteriaDefinitionsAry = new Criteria[criterias.size()];
			criterias.toArray(criteriaDefinitionsAry);
    		query.addCriteria(new Criteria().andOperator(criteriaDefinitionsAry));
    		
			mongoTemplate.remove(query, collectionName);
		}
	}
	
	public Criteria crateDetailCriteria(String doOperate, String doData, String field) {
		if ("nu".equals(doOperate)) {
			//为null或者""
			return new Criteria().orOperator(Criteria.where(field).is(null),Criteria.where(field).is(""));
		} 
		if ("nnu".equals(doOperate)) {
			//不为null且不为""
			return new Criteria().norOperator(Criteria.where(field).is(null),Criteria.where(field).is(""));
		}
		if("like".equals(doOperate)){
			return Criteria.where(field).regex(doData);
		}
		if("eq".equals(doOperate)){
			return Criteria.where(field).is(doData);
		}
		if("ne".equals(doOperate)){
			return Criteria.where(field).ne(doData);
		}
		if("gt".equals(doOperate)){
			return Criteria.where(field).gte(doData);
		}
		if("lt".equals(doOperate)){
			return Criteria.where(field).lte(doData);
		}
		if("gs".equals(doOperate)){
			return Criteria.where(field).gt(doData);
		}
		if("ls".equals(doOperate)){
			return Criteria.where(field).lt(doData);
		}
		return null;
	}


}
