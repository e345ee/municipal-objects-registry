package ru.itmo.config;


import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;
import java.util.Properties;

import com.zaxxer.hikari.HikariDataSource;

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

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(required("db.url"));
        ds.setUsername(required("db.username"));
        ds.setPassword(required("db.password"));
        return ds;
    }

    private String required(String key) {
        String v = env.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return v;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean emf() {
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setPackagesToScan("ru.itmo.domain");
        emf.setPersistenceProviderClass(org.eclipse.persistence.jpa.PersistenceProvider.class);

        Properties jpa = new Properties();
        jpa.put("eclipselink.weaving", "false");
        jpa.put("eclipselink.logging.level", "FINE");
        jpa.put("eclipselink.ddl-generation", "none");
        emf.setJpaProperties(jpa);

        return emf;
    }

    @Bean
    public PlatformTransactionManager txManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
