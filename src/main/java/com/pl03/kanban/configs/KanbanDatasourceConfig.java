package com.pl03.kanban.configs;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.pl03.kanban.kanban_entities",
        entityManagerFactoryRef = "kanbanEntityManager",
        transactionManagerRef = "kanbanTransactionManager"
)
public class KanbanDatasourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.kanban")
    public DataSourceProperties kanbanDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.kanban.configuration")
    public DataSource kanbanDataSource() {
        return kanbanDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "kanbanEntityManager")
    @Primary
    public LocalContainerEntityManagerFactoryBean kanbanEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(kanbanDataSource())
                .packages("com.pl03.kanban.kanban_entities")
                .build();
    }

    @Bean(name = "kanbanTransactionManager")
    public PlatformTransactionManager kanbanTransactionManager(
            final @Qualifier("kanbanEntityManager") LocalContainerEntityManagerFactoryBean kanbanEntityManager) {
        return new JpaTransactionManager(
                Objects.requireNonNull(
                        kanbanEntityManager.getObject()
                )
        );
    }
}
