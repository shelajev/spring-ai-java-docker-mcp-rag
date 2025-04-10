package com.example.adoptions;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootApplication
public class SchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulingApplication.class, args);
    }

    @Bean
    ToolCallbackProvider serviceToolCallbackProvider(
            DogAdoptionAppointmentScheduler scheduler) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(scheduler)
                .build();
    }

}


@Service
class DogAdoptionAppointmentScheduler {

    @Tool(description = "schedule an appointment to adopt a dog" +
            " at the Pooch Palace dog adoption agency")
    String scheduleDogAdoptionAppointment(
            @ToolParam(description = "the id of the dog") int id,
            @ToolParam(description = "the name of the dog") String name) {
        var instant = Instant.now().plus(3, ChronoUnit.DAYS);
        System.out.println("confirming the appointment: " + instant + " for dog " + id + " named " + name);
        return instant.toString();
    }
}
