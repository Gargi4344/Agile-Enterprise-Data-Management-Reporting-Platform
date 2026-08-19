package com.aedmp.platform.dataset.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * V3__seed_demo_dataset.sql registers a dataset_versions row pointing at
 * local://seed-data/customer_orders_seed_v1.csv (a path relative to the process working
 * directory, matching how docker-compose/README run the app). This copies the bundled classpath
 * copy of that same file to that path on first startup so the seeded demo version is actually
 * readable (schema profiling, validation runs) rather than only existing as metadata.
 */
@Component
@ConditionalOnProperty(name = "aedmp.storage.mode", havingValue = "local", matchIfMissing = true)
@Slf4j
public class DemoSeedDataInitializer implements ApplicationRunner {

    private static final String RELATIVE_PATH = "seed-data/customer_orders_seed_v1.csv";
    private static final String CLASSPATH_RESOURCE = "seed-data/customer_orders_seed_v1.csv";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path target = Path.of(RELATIVE_PATH);
        if (Files.exists(target)) {
            return;
        }
        ClassPathResource resource = new ClassPathResource(CLASSPATH_RESOURCE);
        if (!resource.exists()) {
            log.warn("Demo seed CSV not found on classpath at {}", CLASSPATH_RESOURCE);
            return;
        }
        Files.createDirectories(target.getParent());
        try (var in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Materialized demo seed dataset file at {}", target.toAbsolutePath());
    }
}
