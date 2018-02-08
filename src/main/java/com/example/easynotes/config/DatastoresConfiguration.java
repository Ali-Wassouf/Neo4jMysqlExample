package com.example.easynotes.config;


import org.hibernate.jpa.HibernatePersistenceProvider;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.easynotes.repository.relational")
@EnableNeo4jRepositories(basePackages = "com.example.easynotes.repository.graph")
@EnableTransactionManagement
public class DatastoresConfiguration {

    @Bean
    public org.neo4j.ogm.config.Configuration configuration(){
        return new org.neo4j.ogm.config.Configuration.Builder().uri("bolt://127.0.0.1")
                .credentials("neo4j","123456").build();
    }

    @Bean
    public SessionFactory sessionFactory(){
        return new SessionFactory(configuration(), "com.example.easynotes.models.graph");
    }

    @Bean
    public Neo4jTransactionManager neo4jTransactionManager(){
        return new Neo4jTransactionManager(sessionFactory());
    }

    @Bean
    public Session getSession()  {
       return neo4jTransactionManager().getSessionFactory().openSession();
    }


    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder
                .create()
                .driverClassName("com.mysql.jdbc.Driver")
                .build();
    }


    @Primary
    @Bean
    @Autowired
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setPackagesToScan("com.example.easynotes.repository.relational");
        entityManagerFactory.setJpaDialect(new HibernateJpaDialect());
        Map<String, String> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.connection.charSet", "UTF-8");
        jpaProperties.put("spring.jpa.hibernate.ddl-auto", "none");
        jpaProperties.put("spring.jpa.hibernate.naming-strategy", "org.springframework.boot.orm.jpa.SpringNamingStrategy");
        jpaProperties.put("hibernate.bytecode.provider", "javassist");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        jpaProperties.put("hibernate.order_inserts", "true");
        jpaProperties.put("hibernate.jdbc.batch_size", "50");

        entityManagerFactory.setJpaPropertyMap(jpaProperties);
        entityManagerFactory.setPersistenceProvider(new HibernatePersistenceProvider());
        return entityManagerFactory;
    }

    @Autowired
    @Primary
    @Bean(name = "mysqlTransactionManager")
    public JpaTransactionManager mysqlTransactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory)
            throws Exception {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }


    @Autowired
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(Neo4jTransactionManager neo4jTransactionManager,
                                                         JpaTransactionManager mysqlTransactionManager) {
        return new ChainedTransactionManager(
                mysqlTransactionManager,
                neo4jTransactionManager
        );
    }
}