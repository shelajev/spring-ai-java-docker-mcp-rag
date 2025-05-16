package com.example.adoptions;

import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.DockerModelRunnerContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

public class TestAdoptionsApplication {

    public static void main(String[] args) {
        System.setProperty("spring.sql.init.mode", "always");
        SpringApplication.run(AdoptionsApplication.class, args);
    }
}

@Configuration
@Testcontainers
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    @RestartScope
    PostgreSQLContainer<?> postgreSQLContainer() {
        var image = DockerImageName.parse("pgvector/pgvector:pg16")
                .asCompatibleSubstituteFor("postgres");
        return new PostgreSQLContainer<>(image);
    }

}

@Configuration
@ConditionalOnExpression("'${scheduling-service.url:}'.empty") // only start the scheduling container if the url isn't set
@Testcontainers
class SchedulingServiceConfiguration {

    @Bean
    @RestartScope
    GenericContainer<?> schedulingService() {
        return new GenericContainer<>(DockerImageName.parse("scheduling"))
                .withExposedPorts(8081)
                .waitingFor(Wait.forHttp("/sse"))
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(getClass())));
    }

    @Bean
    DynamicPropertyRegistrar adoptionServiceProperties(GenericContainer<?> schedulingService) {
        return (properties) ->
            properties.add("scheduling-service.url", () -> "http://localhost:" + schedulingService.getFirstMappedPort());
    }

}

@Configuration
@Testcontainers
class DockerModelRunnerConfiguration {

    @Bean
    @RestartScope
    DockerModelRunnerContainer dmr() {
        return new DockerModelRunnerContainer("alpine/socat:1.8.0.1");
//                .withModel("ai/gemma3:4B-Q4_0"); // In next tc-java release 1.21.1
    }

    @Bean
    DynamicPropertyRegistrar openAiProperties(DockerModelRunnerContainer dmr) {
        return (properties) ->
                properties.add("spring.ai.openai.base-url", dmr::getOpenAIEndpoint);
    }

}

@Configuration
class DogDataInitializerConfiguration {

    @Bean
    ApplicationRunner initializerRunner(VectorStore vectorStore,
                                        DogRepository dogRepository) {
        return _ -> {
            if (dogRepository.count() == 0) {
                System.out.println("initializing vector store");
                var map = Map.of(
                        "Jasper", "A grey Shih Tzu known for being protective.",
                        "Toby", "A grey Doberman known for being playful.",
                        "Nala", "A spotted German Shepherd known for being loyal.",
                        "Penny", "A white Great Dane known for being protective.",
                        "Bella", "A golden Poodle known for being calm.",
                        "Willow", "A brindle Great Dane known for being calm.",
                        "Daisy", "A spotted Poodle known for being affectionate.",
                        "Mia", "A grey Great Dane known for being loyal.",
                        "Molly", "A golden Chihuahua known for being curious.",
                        "Prancer", "A demonic, neurotic, man hating, animal hating, children hating dog that looks like a gremlin."
                );
                map.forEach((name, description) -> {
                    var dog = dogRepository.save(new Dog(0, name, null, description));
                    var dogument = new Document("id: %s, name: %s, description: %s".formatted(dog.id(), dog.name(), dog.description()));
                    vectorStore.add(List.of(dogument));
                });
                System.out.println("finished initializing vector store");
            }

        };
    }
}
