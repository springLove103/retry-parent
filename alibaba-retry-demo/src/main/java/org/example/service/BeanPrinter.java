package org.example.service;


import com.alibaba.easyretry.common.RetryContainer;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

@Component
public class BeanPrinter  implements CommandLineRunner {

    private final ApplicationContext ctx;

    public BeanPrinter(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run(String... args) throws Exception {
/*        String[] ds = ctx.getBeanNamesForType(DataSource.class);
        System.out.println("DataSource beans: " + Arrays.toString(ds));
        System.out.println("Has easyRetryMybatisDataSource: " + ctx.containsBean("easyRetryMybatisDataSource"));

        String[] retryContainers = ctx.getBeanNamesForType(RetryContainer.class);
        System.out.println("RetryContainer bean names: " + Arrays.toString(retryContainers));
        System.out.println("Found RetryContainer bean count: " + retryContainers.length);*/
        System.out.println(">>> Resource check:");
        URL configUrl = Thread.currentThread().getContextClassLoader().getResource("dal/easyretry/easy-mybatis-config.xml");
        URL mapperUrl = Thread.currentThread().getContextClassLoader().getResource("dal/easyretry/easy-retry-task-mapper.xml");
        System.out.println("easy-mybatis-config.xml => " + configUrl);
        System.out.println("easy-retry-task-mapper.xml => " + mapperUrl);

        System.out.println(">>> Beans check:");
        String[] sqlSessionFactoryBeans = ctx.getBeanNamesForType(SqlSessionFactory.class);
        System.out.println("SqlSessionFactory beans: " + String.join(", ", sqlSessionFactoryBeans));

        if (ctx.containsBean("easyRetrySqlSessionFactory")) {
            SqlSessionFactory factory = (SqlSessionFactory) ctx.getBean("easyRetrySqlSessionFactory");
            System.out.println("Found bean easyRetrySqlSessionFactory");
            try {
                Collection<String> names = factory.getConfiguration().getMappedStatementNames();
//                Set<String> names = factory.getConfiguration().getMappedStatementNames();
                System.out.println("Mapped statements count: " + names.size());
                // print some or all names
                names.stream().sorted().forEach(n -> System.out.println("  MAPPED: " + n));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            System.out.println("No bean named easyRetrySqlSessionFactory found");
            // If there are other SqlSessionFactory beans, inspect first
            if (sqlSessionFactoryBeans.length > 0) {
                SqlSessionFactory factory = (SqlSessionFactory) ctx.getBean(sqlSessionFactoryBeans[0]);
                System.out.println("Inspecting first SqlSessionFactory bean: " + sqlSessionFactoryBeans[0]);
                //Set<String> names = factory.getConfiguration().getMappedStatementNames();
                Collection<String> names = factory.getConfiguration().getMappedStatementNames();
                System.out.println("Mapped statements count: " + names.size());
                names.stream().sorted().forEach(n -> System.out.println("  MAPPED: " + n));
            }
        }
    }
}
