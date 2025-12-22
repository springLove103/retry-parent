package org.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;


/**
 * 提供名为 easyRetryMybatisDataSource 的 DataSource bean（starter 要求此名称）
 */
@Configuration
public class EasyRetryDatasourceConfig {

//    @Bean(name = "easyRetryMybatisDataSource", initMethod = "start", destroyMethod = "close")
//    public DataSource easyRetryMybatisDataSource() {
//        HikariConfig cfg = new HikariConfig();
//        cfg.setJdbcUrl("jdbc:mysql://localhost:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC");
//        cfg.setUsername("root");
//        cfg.setPassword("111111");
//        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
//        // 可添加连接池其它配置...
//        return new HikariDataSource(cfg);
//    }


    @Value("${easy.datasource.url}")
    private String jdbcUrl;

    @Value("${easy.datasource.username:}")
    private String username;

    @Value("${easy.datasource.password:}")
    private String password;

    @Value("${easy.datasource.driver-class-name:org.h2.Driver}")
    private String driverClassName;

    @Bean(name = "easyRetryMybatisDataSource")
    public DataSource easyRetryMybatisDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(username);
        cfg.setPassword(password);
        
        // 显式加载驱动类以避免类加载问题
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load JDBC driver: " + driverClassName, e);
        }
        cfg.setDriverClassName(driverClassName);
        cfg.setMaximumPoolSize(10);
        return new HikariDataSource(cfg);
    }
    
}