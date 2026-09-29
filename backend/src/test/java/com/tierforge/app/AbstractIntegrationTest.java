package com.tierforge.app;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

// Deliberately NOT using @Testcontainers/@Container: that JUnit5 extension manages container
// lifecycle per test class, which — even for a field inherited from this shared base class —
// started a separate Postgres container per subclass. Three containers starting within a few
// seconds of each other under Docker Desktop's resource limits caused earlier containers'
// connections to silently die mid-suite. The documented fix for sharing one container across
// multiple test classes is this "singleton container" pattern: start it once, manually, here.
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }
}
