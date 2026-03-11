# kafka + rest-assured + spring-boot test automation demo

This repository is a small demo showing how to write integration tests for a Spring Boot application that produces Kafka events and how to verify those events using Testcontainers, Apache Kafka clients, and RestAssured.

What this project contains

- A tiny Spring Boot application (`demo.DemoApplication`) with a REST endpoint that produces an `order-created` Kafka event.
- Test code under `src/test/java/com/test` that:
  - Starts a Kafka Testcontainer (so tests run without a standalone Kafka broker).
  - Starts the Spring Boot application programmatically in the same JVM.
  - Posts to the `/orders` endpoint using RestAssured.
  - Consumes from the `order-created` topic and asserts the event payload.

Prerequisites

- Java (JDK 11+ or JDK 17+, matching your project's pom.xml settings)
- Maven 3.6+
- Docker Engine (Testcontainers requires Docker to run the Kafka container)
- Internet access to pull the Testcontainers and Confluent Kafka images the first time

Quick start — run the tests

1. Start Docker on your machine.
2. From the project root run:

   ```bash
   mvn test
   ```

   This will start a Kafka container, start the Spring Boot application (inside the test JVM), run the test which posts an order and then consumes the message from Kafka.

Why the tests use a random port (server.port=0)

The test config sets `System.setProperty("server.port","0")` before starting Spring. `server.port=0` tells Spring Boot to bind to an ephemeral (random) port allocated by the OS. This is the recommended pattern for integration tests because:

- It avoids port collisions between concurrent test runs or other processes that might be listening on the same fixed port.
- It removes brittle timing/race conditions that happen when you try to pick and reserve a free port yourself.

If you change the test to use a fixed port (for example, `server.port=8080`) the test may fail with a bind error (Address already in use) when that port is already taken by another process or another test run. Prefer `0` (or the Spring Test annotation `@SpringBootTest(webEnvironment = RANDOM_PORT)`) for robust tests.

Where the tests live

- `src/test/java/com/test/OrderKafkaTest.java` — uses Testcontainers' `KafkaContainer`, programmatically starts Spring Boot, configures RestAssured with the chosen port, posts an order and asserts the consumed Kafka message.

Notes and troubleshooting

- Testcontainers requires Docker. If tests fail with image pull errors or container startup errors, ensure Docker is running and you can pull images from Docker Hub.
- If a test fails with a bind/port error, check for other processes using the port. On macOS you can inspect a port with:

  ```bash
  lsof -iTCP:8080 -sTCP:LISTEN
  ```

- If Kafka consumer polls return empty results, ensure the container started successfully and that the application produced the event. Look at the Surefire reports (target/surefire-reports) for the full test output.
- When running tests in parallel (TestNG or CI), prefer ephemeral/random ports or use isolated environments per worker to avoid collisions.

Extending this demo

- Replace the hand-rolled Spring boot startup with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` and `@LocalServerPort` if you prefer JUnit/TestNG-managed application lifecycle.
- Add more assertions to validate the exact JSON schema of the Kafka event or wire up a schema registry in a test container if needed.

Files of interest

- `src/main/java/demo/DemoApplication.java`
- `src/main/java/demo/controller/OrderController.java` (application REST endpoint)
- `src/main/java/demo/producer/OrderProducer.java` (produces Kafka events)
- `src/test/java/com/test/OrderKafkaTest.java` (integration test using Testcontainers + RestAssured)

License & contact

This demo is provided as-is for educational purposes — modify and reuse as you like.

If you want, I can update the test to use `@SpringBootTest` + `@LocalServerPort`, or run the test suite locally and help triage any failures you see.
