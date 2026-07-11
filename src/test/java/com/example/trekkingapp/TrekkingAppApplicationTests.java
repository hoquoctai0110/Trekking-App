package com.example.trekkingapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires an external datasource configuration that is not available in the test environment")
class TrekkingAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
