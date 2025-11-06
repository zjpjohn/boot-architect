package com.cloud.arch.repository;

import com.cloud.arch.core.LogPageQuery;
import com.cloud.arch.core.LogRecord;
import com.cloud.arch.page.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Objects;

public class MongoLogRepository implements ILogRepository {

    private static final String COLLECTION_NAME = "log_record";

    private final MongoTemplate mongoTemplate;

    public MongoLogRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(List<LogRecord> records) {
        mongoTemplate.insert(records, COLLECTION_NAME);
    }

    @Override
    public List<LogRecord> ofBizNo(String bizNo) {
        Query query = Query.query(Criteria.where("bizNo").is(bizNo)).with(Sort.by(Sort.Order.desc("gmtCreate")));
        return mongoTemplate.find(query, LogRecord.class, COLLECTION_NAME);
    }

    @Override
    public Page<LogRecord> queryPage(LogPageQuery condition) {
        Query           query = buildQuery(condition);
        long            total = mongoTemplate.count(query, COLLECTION_NAME);
        Page<LogRecord> page  = new Page<>();
        page.setTotal((int) total);
        page.setCurrent(condition.getPage());
        page.setPageSize(condition.getLimit());
        if (total > 0) {
            query.with(Sort.by(Sort.Order.desc("gmtCreate")));
            query.skip((long) (condition.getPage() - 1) * condition.getLimit()).limit(condition.getLimit());
            List<LogRecord> records = mongoTemplate.find(query, LogRecord.class, COLLECTION_NAME);
            page.setRecords(records);
            page.setSize(records.size());
        }
        return page;
    }

    private Query buildQuery(LogPageQuery condition) {
        Query query = new Query();
        if (StringUtils.isNotBlank(condition.getApp())) {
            query.addCriteria(Criteria.where("app").is(condition.getApp()));
        }
        if (StringUtils.isNotBlank(condition.getGroup())) {
            query.addCriteria(Criteria.where("group").is(condition.getGroup()));
        }
        if (StringUtils.isNotBlank(condition.getBizNo())) {
            query.addCriteria(Criteria.where("bizNo").is(condition.getBizNo()));
        }
        if (StringUtils.isNotBlank(condition.getTenant())) {
            query.addCriteria(Criteria.where("tenant").is(condition.getTenant()));
        }
        if (Objects.nonNull(condition.getOperatorId())) {
            query.addCriteria(Criteria.where("operatorId").is(condition.getOperatorId()));
        }
        return query;
    }

}
