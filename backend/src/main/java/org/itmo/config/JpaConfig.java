package org.itmo.config;


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
        basePackages = "org.itmo.repository",
        entityManagerFactoryRef = "emf",
        transactionManagerRef   = "txManager"
)
public class JpaConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        ds.setUsername("postgres");
        ds.setPassword("AVVx@0589");
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean emf() {
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setPackagesToScan("org.itmo.domain");
        emf.setPersistenceProviderClass(org.eclipse.persistence.jpa.PersistenceProvider.class);

        Properties jpa = new Properties();
        jpa.put("eclipselink.weaving", "false");
        jpa.put("eclipselink.logging.level", "FINE");
        jpa.put("eclipselink.ddl-generation", "none"); // схему создали SQL'ом
        emf.setJpaProperties(jpa);

        return emf;
    }

    @Bean
    public PlatformTransactionManager txManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
