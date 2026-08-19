package com.aedmp.platform;

import com.aedmp.platform.job.JobStatus;
import com.aedmp.platform.job.ProcessingJob;
import com.aedmp.platform.job.ProcessingJobRepository;
import com.aedmp.platform.dataset.DatasetVersion;
import com.aedmp.platform.dataset.DatasetVersionRepository;
import com.aedmp.platform.dataset.DatasetVersionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end coverage of the dataset lifecycle (upload -> profile -> validate -> process ->
 * report -> audit) plus the RBAC and failure scenarios called out in docs/state-machine.md and
 * the master spec's "critical scenarios" list, driven entirely through the real HTTP API against
 * a real Postgres (Testcontainers) so Flyway migrations, JSONB columns, and constraints are all
 * exercised exactly as they would be in production.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatasetVersionRepository datasetVersionRepository;

    @Autowired
    private ProcessingJobRepository processingJobRepository;

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void loginWithValidSeededCredentialsSucceeds() throws Exception {
        String token = loginAndGetToken("admin@demo.aedmp.local", "DemoAdmin!2026");
        org.assertj.core.api.Assertions.assertThat(token).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"admin@demo.aedmp.local\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/datasets")).andExpect(status().isUnauthorized());
    }

    @Test
    void operatorCannotManageUsers() throws Exception {
        String token = loginAndGetToken("operator@demo.aedmp.local", "DemoOperator!2026");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void healthEndpointIsPubliclyReachable() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Transactional
    void fullLifecycle_uploadValidateRejectFixProcessReport() throws Exception {
        String analystToken = loginAndGetToken("analyst@demo.aedmp.local", "DemoAnalyst!2026");
        String unique = String.valueOf(Instant.now().toEpochMilli());

        String createDatasetBody = """
                {"name":"Lifecycle Test Dataset %s","description":"integration test"}
                """.formatted(unique);
        String datasetJson = mockMvc.perform(post("/api/datasets")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType("application/json").content(createDatasetBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long datasetId = objectMapper.readTree(datasetJson).get("id").asLong();

        // Attach a couple of rules scoped to this dataset.
        createRule(analystToken, datasetId, "order_id required", "COMPLETENESS", "BLOCKER", "order_id", "NOT_NULL", "");
        createRule(analystToken, datasetId, "amount non-negative", "VALIDITY", "BLOCKER", "amount", "RANGE_MIN", "0");

        // v1: one row is missing order_id -> should be rejected.
        String badCsv = "order_id,amount\n,10.00\nORD-1,5.00\n";
        MockMultipartFile badFile = new MockMultipartFile("file", "orders.csv", "text/csv",
                badCsv.getBytes(StandardCharsets.UTF_8));
        String v1Json = mockMvc.perform(multipart("/api/datasets/{id}/versions", datasetId)
                        .file(badFile).param("sourceSystem", "integration-test")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rowCount").value(2))
                .andReturn().getResponse().getContentAsString();
        long v1Id = objectMapper.readTree(v1Json).get("id").asLong();

        String v1RunJson = mockMvc.perform(post("/api/validation/runs")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType("application/json").content("{\"datasetVersionId\":" + v1Id + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(v1RunJson).get("rejectedRecords").asLong())
                .isEqualTo(1);

        DatasetVersion v1AfterRun = datasetVersionRepository.findById(v1Id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(v1AfterRun.getStatus()).isEqualTo(DatasetVersionStatus.REJECTED);

        // v2: clean data -> should pass and become READY.
        String goodCsv = "order_id,amount\nORD-1,5.00\nORD-2,7.50\n";
        MockMultipartFile goodFile = new MockMultipartFile("file", "orders.csv", "text/csv",
                goodCsv.getBytes(StandardCharsets.UTF_8));
        String v2Json = mockMvc.perform(multipart("/api/datasets/{id}/versions", datasetId)
                        .file(goodFile).param("sourceSystem", "integration-test")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andReturn().getResponse().getContentAsString();
        long v2Id = objectMapper.readTree(v2Json).get("id").asLong();

        mockMvc.perform(post("/api/validation/runs")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType("application/json").content("{\"datasetVersionId\":" + v2Id + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.qualityScore").value(100.0));

        // Re-uploading the exact same bytes as a "new" version is rejected (checksum reuse).
        MockMultipartFile duplicateFile = new MockMultipartFile("file", "orders.csv", "text/csv",
                goodCsv.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/datasets/{id}/versions", datasetId)
                        .file(duplicateFile).param("sourceSystem", "integration-test")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isConflict());

        // Process (transform + load) the now-READY v2.
        String jobJson = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType("application/json")
                        .content("{\"datasetVersionId\":" + v2Id + ",\"jobType\":\"TRANSFORMATION\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        long jobId = objectMapper.readTree(jobJson).get("id").asLong();

        // Idempotent resubmission returns the same job, no duplicate work.
        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType("application/json")
                        .content("{\"datasetVersionId\":" + v2Id + ",\"jobType\":\"TRANSFORMATION\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(jobId));

        DatasetVersion v2AfterJob = datasetVersionRepository.findById(v2Id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(v2AfterJob.getStatus()).isEqualTo(DatasetVersionStatus.COMPLETED);

        String reportJson = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType("application/json")
                        .content("{\"datasetVersionId\":" + v2Id + ",\"reportType\":\"DATASET_QUALITY\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode summary = objectMapper.readTree(reportJson).get("summaryMetrics");
        org.assertj.core.api.Assertions.assertThat(summary.get("qualityScore").asDouble()).isEqualTo(100.0);

        mockMvc.perform(get("/api/reports/kpis/overview").header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetsRegistered").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        String adminToken = loginAndGetToken("admin@demo.aedmp.local", "DemoAdmin!2026");
        mockMvc.perform(get("/api/audit").param("targetType", "DATASET_VERSION").param("targetId", String.valueOf(v2Id))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    @Test
    @Transactional
    void missingRequiredColumnIsRejectedAsSchemaFailure() throws Exception {
        String token = loginAndGetToken("analyst@demo.aedmp.local", "DemoAnalyst!2026");
        String unique = String.valueOf(Instant.now().toEpochMilli());
        long datasetId = createDataset(token, "Missing Column Dataset " + unique);
        createRule(token, datasetId, "email required", "COMPLETENESS", "BLOCKER", "customer_email", "NOT_NULL", "");

        String csvMissingColumn = "order_id,amount\nORD-1,5.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv",
                csvMissingColumn.getBytes(StandardCharsets.UTF_8));
        String versionJson = mockMvc.perform(multipart("/api/datasets/{id}/versions", datasetId)
                        .file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long versionId = objectMapper.readTree(versionJson).get("id").asLong();

        mockMvc.perform(post("/api/validation/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"datasetVersionId\":" + versionId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.rejectedRecords").value(1))
                .andExpect(jsonPath("$.qualityScore").value(0.0));
    }

    @Test
    @Transactional
    void emptyDatasetValidatesWithoutError() throws Exception {
        String token = loginAndGetToken("analyst@demo.aedmp.local", "DemoAnalyst!2026");
        String unique = String.valueOf(Instant.now().toEpochMilli());
        long datasetId = createDataset(token, "Empty Dataset " + unique);

        String emptyCsv = "order_id,amount\n";
        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv",
                emptyCsv.getBytes(StandardCharsets.UTF_8));
        String versionJson = mockMvc.perform(multipart("/api/datasets/{id}/versions", datasetId)
                        .file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rowCount").value(0))
                .andReturn().getResponse().getContentAsString();
        long versionId = objectMapper.readTree(versionJson).get("id").asLong();

        mockMvc.perform(post("/api/validation/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"datasetVersionId\":" + versionId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRecords").value(0))
                .andExpect(jsonPath("$.qualityScore").value(0.0));
    }

    @Test
    @Transactional
    void jobCannotStartBeforeVersionIsReady() throws Exception {
        String token = loginAndGetToken("analyst@demo.aedmp.local", "DemoAnalyst!2026");
        String unique = String.valueOf(Instant.now().toEpochMilli());
        long datasetId = createDataset(token, "Not Ready Dataset " + unique);

        String csv = "order_id,amount\nORD-1,5.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        String versionJson = mockMvc.perform(multipart("/api/datasets/{id}/versions", datasetId)
                        .file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long versionId = objectMapper.readTree(versionJson).get("id").asLong();

        // Never validated, so still UPLOADED - not eligible for processing yet.
        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"datasetVersionId\":" + versionId + ",\"jobType\":\"TRANSFORMATION\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void failedJobCanBeRetriedAndSucceeds() throws Exception {
        String token = loginAndGetToken("analyst@demo.aedmp.local", "DemoAnalyst!2026");
        String unique = String.valueOf(Instant.now().toEpochMilli());
        long datasetId = createDataset(token, "Retry Dataset " + unique);

        String csv = "order_id,amount\nORD-1,5.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "orders.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        String versionJson = mockMvc.perform(multipart("/api/datasets/{id}/versions", datasetId)
                        .file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long versionId = objectMapper.readTree(versionJson).get("id").asLong();

        mockMvc.perform(post("/api/validation/runs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"datasetVersionId\":" + versionId + "}"))
                .andExpect(status().isCreated());

        // Simulate a prior failed attempt (e.g. a transient storage error) directly via the
        // repository, since forcing a real IO failure through the HTTP layer isn't practical here.
        DatasetVersion version = datasetVersionRepository.findById(versionId).orElseThrow();
        version.setStatus(DatasetVersionStatus.FAILED);
        datasetVersionRepository.save(version);

        ProcessingJob failedJob = ProcessingJob.builder()
                .datasetVersion(version)
                .jobType(com.aedmp.platform.job.JobType.TRANSFORMATION)
                .status(JobStatus.FAILED)
                .idempotencyKey(versionId + ":TRANSFORMATION")
                .retryCount(0)
                .maxRetries(3)
                .errorMessage("simulated storage timeout")
                .createdBy(version.getUploadedBy())
                .build();
        failedJob = processingJobRepository.save(failedJob);

        mockMvc.perform(post("/api/jobs/{id}/retry", failedJob.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.retryCount").value(1));

        DatasetVersion afterRetry = datasetVersionRepository.findById(versionId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(afterRetry.getStatus()).isEqualTo(DatasetVersionStatus.COMPLETED);
    }

    private long createDataset(String token, String name) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"description\":\"integration test\"}";
        String json = mockMvc.perform(post("/api/datasets")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void createRule(String token, long datasetId, String name, String family, String severity,
                             String column, String expressionType, String expression) throws Exception {
        String body = objectMapper.createObjectNode()
                .put("name", name).put("ruleFamily", family).put("severity", severity)
                .put("datasetId", datasetId).put("columnName", column)
                .put("expressionType", expressionType).put("expression", expression)
                .toString();
        mockMvc.perform(post("/api/validation/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());
    }
}
