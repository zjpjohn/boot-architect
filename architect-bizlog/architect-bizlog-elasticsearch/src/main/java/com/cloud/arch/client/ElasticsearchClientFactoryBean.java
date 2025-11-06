package com.cloud.arch.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.google.common.base.Splitter;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ElasticsearchClientFactoryBean implements FactoryBean<ElasticsearchClient>, InitializingBean, DisposableBean {

    private final ElasticSearchProperties properties;
    private       ElasticsearchClient     client;
    private       RestClientTransport     transport;

    public ElasticsearchClientFactoryBean(ElasticSearchProperties properties) {
        this.properties = properties;
    }

    private void initialize() {
        HttpHost[] hosts = Splitter.on(";").splitToList(properties.getServer())
                .stream()
                .map(v -> v.split(":"))
                .map(v -> new HttpHost(v[0], Integer.parseInt(v[1]), "http"))
                .toArray(HttpHost[]::new);
        RestClientBuilder                      builder = RestClient.builder(hosts);
        ElasticSearchProperties.Authentication auth    = properties.getAuth();
        if (auth != null) {
            BasicCredentialsProvider    provider    = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(auth.getUser(), auth.getPassword());
            provider.setCredentials(AuthScope.ANY, credentials);
            builder.setRequestConfigCallback(config -> {
                config.setConnectTimeout(properties.getConnectTimeout());
                config.setSocketTimeout(properties.getSocketTimeout());
                config.setConnectionRequestTimeout(properties.getConnectionRequestTimeout());
                return config;
            }).setHttpClientConfigCallback(config -> {
                config.setDefaultCredentialsProvider(provider);
                config.setMaxConnTotal(properties.getMaxConnTotal());
                config.setMaxConnPerRoute(properties.getMaxConnPerRoute());
                config.setKeepAliveStrategy((response, context) -> properties.getKeepAlive());
                return config;
            });
        }
        this.transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());
        this.client    = new ElasticsearchClient(transport);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }


    @Override
    public ElasticsearchClient getObject() throws Exception {
        return this.client;
    }

    @Override
    public Class<?> getObjectType() {
        return ElasticsearchClient.class;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        this.initialize();
    }

    @Override
    public void destroy() throws Exception {
        this.transport.close();
    }

}
