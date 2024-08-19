package com.pl03.kanban.configs;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.pl03.kanban.user_entities",
        entityManagerFactoryRef = "userEntityManager",
        transactionManagerRef = "userTransactionManager"
)
public class UserDatasourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.user")
    public DataSourceProperties userDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.user.configuration")
    public DataSource userDataSource() {
        return userDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public EntityManagerFactoryBuilder userEntityManagerFactoryBuilder() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        // Disable automatic generation of tables
        vendorAdapter.setGenerateDdl(false);
        return new EntityManagerFactoryBuilder(vendorAdapter, new HashMap<>(), null);
    }

    @Bean(name = "userEntityManager")
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(userDataSource())
                .packages("com.pl03.kanban.user_entities")
                .build();
    }

    @Bean(name = "userTransactionManager")
    public PlatformTransactionManager userTransactionManager(
            final @Qualifier("userEntityManager")
            LocalContainerEntityManagerFactoryBean userEntityManager) {
        return new JpaTransactionManager(
                // Use to throw NullPointerException if userAccountEntityManagerFactory.getObject() is null
                Objects.requireNonNull(
                        userEntityManager.getObject()
                )
        );
    }
}