package com.example.adoptions;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.sql.init.mode=always")
public class DogTest {

    @Autowired
    private DogRepository dogRepository;

    @Autowired
    private VectorStore vectorStore;

    @Test
    void dogsDB() {
        var dogs = dogRepository.findAll();
        assertEquals(10, dogs.size());
    }

    @Test
    void dogsVector() {
        var doguments = vectorStore.similaritySearch("prancer");
        assertEquals(4, doguments.size());
    }

}
