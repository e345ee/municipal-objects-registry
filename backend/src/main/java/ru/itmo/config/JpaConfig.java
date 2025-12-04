package ru.itmo.config;


import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "ru.itmo.repository",
        entityManagerFactoryRef = "emf",
        transactionManagerRef = "txManager"
)
public class JpaConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://pg:5432/studs");
        ds.setUsername("s368748");
        ds.setPassword("TtSXRLK86yz5ENVZ");
        return ds;
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
