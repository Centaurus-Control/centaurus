package de.shadowsoft.centaurus.agent;

import de.shadowsoft.centaurus.agent.config.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AgentProperties.class)
@EnableScheduling
public class CentaurusAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CentaurusAgentApplication.class, args);
    }
}
