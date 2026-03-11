package com.test;

import io.restassured.RestAssured;
import org.testcontainers.containers.KafkaContainer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.utility.DockerImageName;
import org.apache.kafka.clients.consumer.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import demo.DemoApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static io.restassured.RestAssured.*;

public class OrderKafkaTest {
    // start the container first (static ensures it can be started before Spring context)
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.7.arm64"));

    static Consumer<String,String> consumer;
    static ConfigurableApplicationContext context;

    @BeforeClass
    public static void setUp(){
        // start kafka container and expose its bootstrap address to Spring via system property
        kafka.start();
        System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());

        Properties props=new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,"qa-test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        consumer=new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("order-created"));

        // start the Spring Boot application so the /orders endpoint is available
        // run on a random port to avoid collisions during test runs
        System.setProperty("server.port","0");
        context = SpringApplication.run(DemoApplication.class);

        // retrieve the actual port and configure RestAssured
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    public void testOrderEvent(){
        given().contentType("application/json").body("""
                {
                  "orderId":1,
                  "userId":99,
                   "amount":100
                }
                """).when().post("/orders").then().statusCode(200);

        ConsumerRecords<String,String> records=consumer.poll(Duration.ofSeconds(10));

        Assert.assertFalse(records.isEmpty());

        boolean found=false;

        for(ConsumerRecord<String,String> r:records){
            // fixed typo: userdId -> userId
            if (r.value() != null && r.value().contains("userId")){
                found=true;
            }
        }
        Assert.assertTrue(found);
    }

    @AfterClass
    public static void tearDown(){
        if (consumer != null) consumer.close();
        if (context != null) context.close();
        kafka.stop();
    }
}
