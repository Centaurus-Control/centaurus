package de.shadowsoft.centaurus.server.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayMigrationConfig {

    @Bean
    public Object flywayMigration(
        DataSource dataSource,
        @Value("${spring.flyway.enabled:true}") boolean enabled,
        @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
        @Value("${spring.flyway.baseline-version:1}") String baselineVersion,
        @Value("${spring.flyway.locations:classpath:db/migration}") String locations
    ) {
        if (enabled) {
            Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .locations(locations.split(","))
                .load()
                .migrate();
        }
        return new Object();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayBeforeJpa() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                    BeanDefinition beanDefinition = beanFactory.getBeanDefinition("entityManagerFactory");
                    beanDefinition.setDependsOn(appendDependsOn(beanDefinition.getDependsOn(), "flywayMigration"));
                }
            }
        };
    }

    private static String[] appendDependsOn(String[] currentDependsOn, String dependency) {
        if (currentDependsOn == null || currentDependsOn.length == 0) {
            return new String[] {dependency};
        }
        for (String currentDependency : currentDependsOn) {
            if (dependency.equals(currentDependency)) {
                return currentDependsOn;
            }
        }
        String[] updatedDependsOn = new String[currentDependsOn.length + 1];
        System.arraycopy(currentDependsOn, 0, updatedDependsOn, 0, currentDependsOn.length);
        updatedDependsOn[currentDependsOn.length] = dependency;
        return updatedDependsOn;
    }
}
