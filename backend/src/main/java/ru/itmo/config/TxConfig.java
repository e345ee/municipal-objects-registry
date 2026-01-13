package ru.itmo.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TxConfig {

    @Bean
    public JpaDialect jpaDialect() {
        return new EclipseLinkJpaDialect();
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf, JpaDialect jpaDialect) {
        JpaTransactionManager tm = new JpaTransactionManager(emf);
        tm.setJpaDialect(jpaDialect);
        return tm;
    }
}
