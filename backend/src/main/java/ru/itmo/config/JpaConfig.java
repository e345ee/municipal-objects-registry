package ru.itmo.config;

import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.net.URL;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "ru.itmo.repository",
        entityManagerFactoryRef = "emf",
        transactionManagerRef = "txManager"
)
@PropertySource("classpath:db.properties")
public class JpaConfig {

    private final Environment env;

    public JpaConfig(Environment env) {
        this.env = env;
    }

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(required("db.url"));
        ds.setUsername(required("db.username"));
        ds.setPassword(required("db.password"));
        ds.setDriverClassName(env.getProperty("db.driver", "org.postgresql.Driver"));

        // Apache Commons DBCP2 pool configuration
        ds.setInitialSize(intProp("db.pool.initialSize", 5));
        ds.setMaxTotal(intProp("db.pool.maxTotal", 20));
        ds.setMaxIdle(intProp("db.pool.maxIdle", 10));
        ds.setMinIdle(intProp("db.pool.minIdle", 5));
        ds.setMaxWaitMillis(longProp("db.pool.maxWaitMillis", 10000L));

        ds.setValidationQuery(env.getProperty("db.pool.validationQuery", "SELECT 1"));
        ds.setValidationQueryTimeout(intProp("db.pool.validationQueryTimeout", 3));
        ds.setTestOnBorrow(boolProp("db.pool.testOnBorrow", true));
        ds.setTestWhileIdle(boolProp("db.pool.testWhileIdle", true));

        ds.setTimeBetweenEvictionRunsMillis(longProp("db.pool.timeBetweenEvictionRunsMillis", 30000L));
        ds.setMinEvictableIdleTimeMillis(longProp("db.pool.minEvictableIdleTimeMillis", 300000L));

        ds.setRemoveAbandonedOnBorrow(boolProp("db.pool.removeAbandonedOnBorrow", false));
        ds.setRemoveAbandonedTimeout(intProp("db.pool.removeAbandonedTimeout", 60));
        ds.setLogAbandoned(boolProp("db.pool.logAbandoned", false));

        return ds;
    }

    private String required(String key) {
        String v = env.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return v;
    }

    private int intProp(String key, int def) {
        String v = env.getProperty(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        return Integer.parseInt(v.trim());
    }

    private long longProp(String key, long def) {
        String v = env.getProperty(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        return Long.parseLong(v.trim());
    }

    private boolean boolProp(String key, boolean def) {
        String v = env.getProperty(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        return Boolean.parseBoolean(v.trim());
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean emf() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setPackagesToScan("ru.itmo.domain");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(false);
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties jpa = new Properties();
        jpa.put("hibernate.hbm2ddl.auto", "none");
        jpa.put("hibernate.format_sql", "true");
        jpa.put("hibernate.jdbc.lob.non_contextual_creation", "true");

        // Hibernate L2 cache via JCache (Ehcache)
        jpa.put("hibernate.cache.use_second_level_cache", "true");
        jpa.put("hibernate.cache.region.factory_class", "jcache");
        jpa.put("hibernate.javax.cache.provider", "org.ehcache.jsr107.EhcacheCachingProvider");
        jpa.put("hibernate.javax.cache.missing_cache_strategy", "create");
        jpa.put("hibernate.generate_statistics", "false");

        URL ehcacheUrl = JpaConfig.class.getResource("/ehcache.xml");
        if (ehcacheUrl == null) {
            throw new IllegalStateException("ehcache.xml not found in classpath");
        }
        jpa.put("hibernate.javax.cache.uri", ehcacheUrl.toExternalForm());

        emf.setJpaProperties(jpa);
        return emf;
    }

    @Bean
    public PlatformTransactionManager txManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
