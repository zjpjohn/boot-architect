package com.cloud.arch.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.cloud.arch.client.ElasticSearchProperties;
import com.cloud.arch.core.LogPageQuery;
import com.cloud.arch.core.LogRecord;
import com.cloud.arch.page.Page;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class ElasticLogRepository implements ILogRepository {

    private final ElasticSearchProperties properties;
    private final ElasticsearchClient     client;

    public ElasticLogRepository(ElasticSearchProperties properties, ElasticsearchClient client) {
        this.properties = properties;
        this.client     = client;
    }

    @Override
    public void save(List<LogRecord> records) {
        try {
            List<BulkOperation> operations = records.stream()
                                                    .map(r -> new BulkOperation.Builder().create(d -> d.document(r)
                                                                                                       .id(r.getId()))
                                                                                         .build())
                                                    .collect(Collectors.toList());
            client.bulk(e -> e.index(properties.getIndex()).operations(operations));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<LogRecord> ofBizNo(String bizNo) {
        try {
            SearchResponse<LogRecord> response = client.search(r -> r.index(properties.getIndex())
                                                                     .query(q -> q.term(e -> e.field("bizNo")
                                                                                              .value(bizNo)))
                                                                     .sort(s -> s.field(f -> f.field("gmtCreate")
                                                                                              .order(SortOrder.Desc))), LogRecord.class);
            return response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<LogRecord> queryPage(LogPageQuery query) {
        try {
            List<Query>     queries = buildQuery(query);
            long            total   = client.count(c -> c.query(q -> q.bool(b -> b.must(queries)))).count();
            Page<LogRecord> page    = new Page<>();
            page.setTotal((int) total);
            page.setCurrent(query.getPage());
            page.setPageSize(query.getLimit());
            if (total > 0) {
                SearchResponse<LogRecord> response = client.search(r -> r.index(properties.getIndex())
                                                                         .query(q -> q.bool(b -> b.must(queries)))
                                                                         .sort(s -> s.field(f -> f.field("gmtCreate")
                                                                                                  .order(SortOrder.Desc)))
                                                                         .from((query.getPage() - 1) * query.getLimit())
                                                                         .size(query.getLimit()), LogRecord.class);
                List<LogRecord> records = response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
                page.setRecords(records);
                page.setSize(records.size());
            }
            return page;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Query> buildQuery(LogPageQuery query) {
        List<Query> queries = Lists.newArrayList();
        if (StringUtils.isNotBlank(query.getApp())) {
            Query appQuery = Query.of(b -> b.term(e -> e.field("app").value(query.getApp())));
            queries.add(appQuery);
        }
        if (StringUtils.isNotBlank(query.getGroup())) {
            Query groupQuery = Query.of(b -> b.term(e -> e.field("group").value(query.getGroup())));
            queries.add(groupQuery);
        }
        if (StringUtils.isNotBlank(query.getBizNo())) {
            Query bizNoQuery = Query.of(b -> b.term(e -> e.field("bizNo").value(query.getBizNo())));
            queries.add(bizNoQuery);
        }
        if (StringUtils.isNotBlank(query.getTenant())) {
            Query tenantQuery = Query.of(b -> b.term(e -> e.field("tenant").value(query.getTenant())));
            queries.add(tenantQuery);
        }
        if (query.getOperatorId() != null) {
            Query operatorQuery = Query.of(b -> b.term(e -> e.field("operatorId").value(query.getOperatorId())));
            queries.add(operatorQuery);
        }
        return queries;
    }

}
