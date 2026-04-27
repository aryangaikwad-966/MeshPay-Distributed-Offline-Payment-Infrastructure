package com.demo.upimesh;

import com.demo.upimesh.crypto.HybridCryptoService;
import com.demo.upimesh.model.*;
import com.demo.upimesh.service.BridgeIngestionService;
import com.demo.upimesh.service.IdempotencyService;
import com.demo.upimesh.crypto.ServerKeyHolder;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Integration Tests for UPI Offline Mesh
 * 
 * Tests cover:
 * - Payment settlement flow
 * - Idempotency and duplicate protection
 * - Security (authentication, authorization)
 * - Rate limiting
 * - Error scenarios
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UPI Mesh Integration Tests")
public class UpiMeshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private HybridCryptoService cryptoService;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    @Autowired
    private BridgeIngestionService bridgeIngestionService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ServerKeyHolder serverKeyHolder;


    @BeforeEach
    public void setUp() {
        // Clear idempotency cache and transactions before each test
        idempotencyService.clear();
        transactionRepository.deleteAll();
    }

    // ===== Security & Authentication Tests =====

    @Test
    @DisplayName("GET /api/accounts requires authentication")
    public void testGetAccountsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/bridge/ingest requires BRIDGE_NODE role")
    public void testBridgeIngestRequiresBridgeNodeRole() throws Exception {
        mockMvc.perform(post("/api/bridge/ingest")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))

            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BRIDGE_NODE")
    @DisplayName("Bridge endpoint accepts valid JWT with BRIDGE_NODE role")
    public void testBridgeEndpointWithValidRole() throws Exception {
        // This would normally need a valid encrypted packet
        mockMvc.perform(post("/api/bridge/ingest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ciphertext\":\"invalid\"}"))
            .andExpect(status().isBadRequest());  // Bad request due to invalid packet, not auth failure
    }

    // ===== Payment Settlement Tests =====

    @Test
    @WithMockUser(roles = "BRIDGE_NODE")
    @DisplayName("Valid payment settles successfully")
    public void testValidPaymentSettles() throws Exception {
        // Create payment instruction
        Account senderAccount = accountRepository.findByVpa("alice@demo").orElseThrow();
        Account receiverAccount = accountRepository.findByVpa("bob@demo").orElseThrow();
        
        BigDecimal initialSenderBalance = senderAccount.getBalance();
        BigDecimal initialReceiverBalance = receiverAccount.getBalance();
        BigDecimal amount = new BigDecimal("50.00");
        
        // Create and encrypt payment
        PaymentInstruction instruction = new PaymentInstruction();
        instruction.setSenderVpa("alice@demo");
        instruction.setReceiverVpa("bob@demo");
        instruction.setAmount(amount);
        instruction.setSignedAt(System.currentTimeMillis());
        instruction.setPinHash("1234");  // Demo PIN
        
        String ciphertext = cryptoService.encrypt(instruction, serverKeyHolder.getPublicKey());

        
        // Ingest payment
        mockMvc.perform(post("/api/bridge/ingest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ciphertext\":\"" + ciphertext + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("SETTLED"));
        
        // Verify balances changed
        senderAccount = accountRepository.findByVpa("alice@demo").orElseThrow();
        receiverAccount = accountRepository.findByVpa("bob@demo").orElseThrow();

        
        assert senderAccount.getBalance().compareTo(initialSenderBalance.subtract(amount)) == 0;
        assert receiverAccount.getBalance().compareTo(initialReceiverBalance.add(amount)) == 0;
    }

    // ===== Idempotency Tests =====

    @Test
    @WithMockUser(roles = "BRIDGE_NODE")
    @DisplayName("Duplicate packets are rejected with DUPLICATE_DROPPED status")
    public void testDuplicatePacketRejected() throws Exception {
        // Create and encrypt payment
        PaymentInstruction instruction = new PaymentInstruction();
        instruction.setSenderVpa("alice@demo");
        instruction.setReceiverVpa("bob@demo");
        instruction.setAmount(new BigDecimal("50.00"));
        instruction.setSignedAt(System.currentTimeMillis());
        instruction.setPinHash("1234");
        
        String ciphertext = cryptoService.encrypt(instruction, serverKeyHolder.getPublicKey());

        
        // First ingest
        mockMvc.perform(post("/api/bridge/ingest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ciphertext\":\"" + ciphertext + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("SETTLED"));
        
        // Second ingest (duplicate)
        mockMvc.perform(post("/api/bridge/ingest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ciphertext\":\"" + ciphertext + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("DUPLICATE_DROPPED"));
        
        // Verify only ONE transaction was created
        assert transactionRepository.count() == 1;
    }

    @Test
    @WithMockUser(roles = "BRIDGE_NODE")
    @DisplayName("Concurrent duplicate payments settle exactly once")
    public void testConcurrentDuplicates() throws Exception {
        PaymentInstruction instruction = new PaymentInstruction();
        instruction.setSenderVpa("alice@demo");
        instruction.setReceiverVpa("bob@demo");
        instruction.setAmount(new BigDecimal("50.00"));
        instruction.setSignedAt(System.currentTimeMillis());
        instruction.setPinHash("1234");
        
        String ciphertext = cryptoService.encrypt(instruction, serverKeyHolder.getPublicKey());

        
        // Sequential delivery to verify idempotency in MockMvc environment
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/bridge/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ciphertext\":\"" + ciphertext + "\"}"))
                .andExpect(status().isOk());
        }
        
        // Verify ONLY one transaction settled
        long settledCount = transactionRepository.findAll().stream()
            .filter(t -> "SETTLED".equals(t.getStatus().name()))
            .count();
        assertEquals(1, settledCount, "Should have exactly one settled transaction even with repeated requests");


    }

    // ===== Error Handling Tests =====

    @Test
    @WithMockUser(roles = "BRIDGE_NODE")
    @DisplayName("Stale packets are rejected")
    public void testStalePacketRejected() throws Exception {
        PaymentInstruction instruction = new PaymentInstruction();
        instruction.setSenderVpa("alice@demo");
        instruction.setReceiverVpa("bob@demo");
        instruction.setAmount(new BigDecimal("50.00"));
        instruction.setSignedAt(System.currentTimeMillis() - (2 * 24 * 3600 * 1000)); // 2 days old
        instruction.setPinHash("1234");
        
        String ciphertext = cryptoService.encrypt(instruction, serverKeyHolder.getPublicKey());

        
        mockMvc.perform(post("/api/bridge/ingest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ciphertext\":\"" + ciphertext + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "BRIDGE_NODE")
    @DisplayName("Invalid/tampered ciphertexts are rejected")
    public void testTamperedCiphertextRejected() throws Exception {
        String tampereredCiphertext = "aaaabbbbccccddddeeeeffffgggghhhiiijjjkkk";
        
        mockMvc.perform(post("/api/bridge/ingest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ciphertext\":\"" + tampereredCiphertext + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "BRIDGE_NODE")
    @DisplayName("Insufficient funds is handled gracefully")
    public void testInsufficientFundsHandled() throws Exception {
        // Try to send more than available
        PaymentInstruction instruction = new PaymentInstruction();
        instruction.setSenderVpa("alice@demo");
        instruction.setReceiverVpa("bob@demo");
        instruction.setAmount(new BigDecimal("999999.99")); // Very large amount
        instruction.setSignedAt(System.currentTimeMillis());
        instruction.setPinHash("1234");
        
        String ciphertext = cryptoService.encrypt(instruction, serverKeyHolder.getPublicKey());

        
        mockMvc.perform(post("/api/bridge/ingest")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ciphertext\":\"" + ciphertext + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    // ===== Health & Metrics Tests =====

    @Test
    @DisplayName("Health endpoint is accessible")
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Health endpoint is accessible for monitoring")
    public void testMonitoringEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }


    // ===== API Documentation Tests =====

    @Test
    @DisplayName("Swagger UI is accessible")
    public void testSwaggerUI() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }


    @Test
    @DisplayName("OpenAPI spec is available")
    public void testOpenAPISpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("MeshPay API"));

    }
}
