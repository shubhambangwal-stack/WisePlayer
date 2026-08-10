    package com.iptv.wiseplayer.service;

    import com.iptv.wiseplayer.domain.entity.Admin;
    import com.iptv.wiseplayer.domain.entity.Device;
    import com.iptv.wiseplayer.domain.entity.ActivationRequest;
    import com.iptv.wiseplayer.domain.enums.AdminRole;
    import com.iptv.wiseplayer.domain.enums.DeviceStatus;
    import com.iptv.wiseplayer.domain.enums.SubscriptionType;
    import com.iptv.wiseplayer.dto.request.ResellerActivationRequestDto;
    import com.iptv.wiseplayer.dto.request.ResellerLoginRequest;
    import com.iptv.wiseplayer.exception.AuthenticationException;
    import com.iptv.wiseplayer.exception.BadRequestException;
    import com.iptv.wiseplayer.exception.ResourceNotFoundException;
    import com.iptv.wiseplayer.repository.AdminRepository;
    import com.iptv.wiseplayer.repository.DeviceRepository;
    import com.iptv.wiseplayer.repository.ActivationRequestRepository;
    import com.iptv.wiseplayer.repository.SubscriptionRepository;
    import com.iptv.wiseplayer.repository.ResellerCustomerRepository;
    import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
    import com.iptv.wiseplayer.domain.entity.ResellerCustomer;
    import com.iptv.wiseplayer.exception.ResourceAlreadyExistsException;
    import com.iptv.wiseplayer.security.AdminTokenUtil;
    import com.iptv.wiseplayer.security.DeviceTokenUtil;
    import com.iptv.wiseplayer.service.impl.ResellerServiceImpl;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;
    import org.mockito.InjectMocks;
    import org.mockito.Mock;
    import org.mockito.MockitoAnnotations;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.security.crypto.password.PasswordEncoder;

    import java.math.BigDecimal;
    import java.util.Optional;
    import java.util.UUID;

    import static org.junit.jupiter.api.Assertions.*;
    import static org.mockito.ArgumentMatchers.any;
    import static org.mockito.Mockito.*;

    class ResellerServiceImplTest {

        @Mock
        private DeviceRepository deviceRepository;

        @Mock
        private AdminRepository adminRepository;

        @Mock
        private ActivationRequestRepository activationRequestRepository;

        @Mock
        private DeviceTokenUtil tokenUtil;

        @Mock
        private AdminTokenUtil adminTokenUtil;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private CreditService creditService;

        @Mock
        private SubscriptionRepository subscriptionRepository;

        @Mock
        private ResellerCustomerRepository resellerCustomerRepository;

        @Mock
        private com.iptv.wiseplayer.service.impl.SubscriptionServiceImpl subscriptionService;

        @InjectMocks
        private ResellerServiceImpl resellerService;

        private final UUID resellerId = UUID.randomUUID();
        private final UUID deviceId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            MockitoAnnotations.openMocks(this);
        }

        @Test
        void login_ShouldReturnToken_WhenCredentialsAreValid() {
            // Arrange
            ResellerLoginRequest request = new ResellerLoginRequest();
            request.setUsername("reseller");
            request.setPassword("password");

            Admin admin = new Admin();
            admin.setUsername("reseller");
            admin.setPasswordHash("hashed_password");
            admin.setRole(AdminRole.RESELLER);
            admin.setActive(true);

            when(adminRepository.findByUsername("reseller")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);
            when(adminTokenUtil.generateToken(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn("mock_token");

            // Act
            var response = resellerService.login(request);

            // Assert
            assertNotNull(response);
            assertEquals("mock_token", response.getToken());
        }

        @Test
        void login_ShouldThrowException_WhenPasswordInvalid() {
            // Arrange
            ResellerLoginRequest request = new ResellerLoginRequest();
            request.setUsername("reseller");
            request.setPassword("wrong");

            Admin admin = new Admin();
            admin.setUsername("reseller");
            admin.setPasswordHash("hashed_password");
            admin.setRole(AdminRole.RESELLER);
            admin.setActive(true);

            when(adminRepository.findByUsername("reseller")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("wrong", "hashed_password")).thenReturn(false);

            // Act & Assert
            assertThrows(AuthenticationException.class, () -> resellerService.login(request));
        }

        @Test
        void submitActivationRequest_ShouldSucceed_WhenValid() {
            // Arrange
            ResellerActivationRequestDto requestDto = new ResellerActivationRequestDto();
            requestDto.setDeviceId(deviceId);
            requestDto.setPlanName("ANNUAL");

            Device device = new Device();
            device.setDeviceId(deviceId);
            device.setResellerId(resellerId);
            when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(device));

            when(subscriptionRepository.findByDeviceIdAndStatus(any(), any())).thenReturn(Optional.empty());
            when(activationRequestRepository.findTopByDeviceIdOrderByCreatedAtDesc(deviceId)).thenReturn(Optional.empty());

            when(creditService.getActivationCost("ANNUAL")).thenReturn(BigDecimal.TEN);

            ActivationRequest savedRequest = new ActivationRequest();
            savedRequest.setId(UUID.randomUUID());
            when(activationRequestRepository.save(any(ActivationRequest.class))).thenReturn(savedRequest);
            when(subscriptionService.activateSubscription(any())).thenReturn(null);

            // Act
            var result = resellerService.submitActivationRequest(resellerId, requestDto);

            // Assert
            assertNotNull(result);
            verify(creditService).deductCredits(eq(resellerId), eq("ANNUAL"), any());
            verify(activationRequestRepository, atLeastOnce()).save(any(ActivationRequest.class));
        }

        @Test
        void submitActivationRequest_ShouldThrowException_WhenDuplicatePending() {
            // Arrange
            ResellerActivationRequestDto requestDto = new ResellerActivationRequestDto();
            requestDto.setDeviceId(deviceId);
            requestDto.setPlanName("ANNUAL");

            Device device = new Device();
            device.setDeviceId(deviceId);
            device.setResellerId(resellerId);
            when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(device));

            ActivationRequest existing = new ActivationRequest();
            existing.setStatus("APPROVED");
            existing.setPlanName("ANNUAL");
            when(activationRequestRepository.findTopByDeviceIdOrderByCreatedAtDesc(deviceId)).thenReturn(Optional.of(existing));
            when(creditService.getActivationCost("ANNUAL")).thenReturn(BigDecimal.TEN);

            // Act & Assert
            assertThrows(BadRequestException.class, () -> resellerService.submitActivationRequest(resellerId, requestDto));
        }

        @Test
        void getResellerUsers_ShouldInvokeSearchResellerUsers() {

            Pageable pageable = mock(Pageable.class);
            Page<Device> expectedPage = mock(Page.class);

            when(deviceRepository.searchResellerUsers(
                    eq(resellerId),
                    eq("Samsung"),
                    eq(DeviceStatus.ACTIVE),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    eq(pageable)
            )).thenReturn(expectedPage);

            Page<Device> result =
                    resellerService.getResellerUsers(
                            resellerId,
                            "Samsung",
                            DeviceStatus.ACTIVE,
                            null,
                            null,
                            null,
                            null,
                            null,
                            pageable
                    );

            assertSame(expectedPage, result);

            verify(deviceRepository).searchResellerUsers(
                    eq(resellerId),
                    eq("Samsung"),
                    eq(DeviceStatus.ACTIVE),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    eq(pageable)
            );
        }

        @Test
        void createEndUser_ShouldSucceed_WhenDeviceRegisteredAndUnclaimed() {
            // Arrange
            String macAddress = "00:11:22:33:44:55";
            DeviceRegistrationRequest request = new DeviceRegistrationRequest(macAddress, "Samsung Smart TV", "1.0", "Tizen");
            String fingerprintHash = "mocked_hash";

            when(tokenUtil.hashFingerprint(macAddress)).thenReturn(fingerprintHash);

            Device device = new Device();
            device.setMacAddress(macAddress);
            device.setFingerprintHash(fingerprintHash);
            when(deviceRepository.findByFingerprintHash(fingerprintHash)).thenReturn(Optional.of(device));

            when(resellerCustomerRepository.findByResellerIdAndMacAddress(resellerId, macAddress)).thenReturn(Optional.empty());
            when(resellerCustomerRepository.findByMacAddress(macAddress)).thenReturn(Optional.empty());

            // Act
            var result = resellerService.createEndUser(resellerId, request);

            // Assert
            assertNotNull(result);
            assertTrue((Boolean) result.get("success"));
            assertEquals(macAddress, result.get("macAddress"));
            assertEquals(resellerId, device.getResellerId());

            verify(resellerCustomerRepository).save(any(ResellerCustomer.class));
            verify(deviceRepository).save(device);
        }

        @Test
        void createEndUser_ShouldThrowResourceNotFoundException_WhenDeviceNotRegistered() {
            // Arrange
            String macAddress = "00:11:22:33:44:55";
            DeviceRegistrationRequest request = new DeviceRegistrationRequest(macAddress, "Samsung Smart TV", "1.0", "Tizen");
            String fingerprintHash = "mocked_hash";

            when(tokenUtil.hashFingerprint(macAddress)).thenReturn(fingerprintHash);
            when(deviceRepository.findByFingerprintHash(fingerprintHash)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> resellerService.createEndUser(resellerId, request));
            verify(resellerCustomerRepository, never()).save(any());
            verify(deviceRepository, never()).save(any());
        }

        @Test
        void createEndUser_ShouldThrowResourceAlreadyExistsException_WhenDeviceAlreadyClaimedBySameReseller() {
            // Arrange
            String macAddress = "00:11:22:33:44:55";
            DeviceRegistrationRequest request = new DeviceRegistrationRequest(macAddress, "Samsung Smart TV", "1.0", "Tizen");
            String fingerprintHash = "mocked_hash";

            when(tokenUtil.hashFingerprint(macAddress)).thenReturn(fingerprintHash);

            Device device = new Device();
            device.setMacAddress(macAddress);
            device.setFingerprintHash(fingerprintHash);
            when(deviceRepository.findByFingerprintHash(fingerprintHash)).thenReturn(Optional.of(device));

            ResellerCustomer existingClaim = new ResellerCustomer(resellerId, macAddress, "Name");
            when(resellerCustomerRepository.findByResellerIdAndMacAddress(resellerId, macAddress)).thenReturn(Optional.of(existingClaim));

            // Act & Assert
            assertThrows(ResourceAlreadyExistsException.class, () -> resellerService.createEndUser(resellerId, request));
            verify(resellerCustomerRepository, never()).save(any());
            verify(deviceRepository, never()).save(any());
        }

        @Test
        void createEndUser_ShouldThrowResourceAlreadyExistsException_WhenDeviceAlreadyClaimedByOtherReseller() {
            // Arrange
            String macAddress = "00:11:22:33:44:55";
            DeviceRegistrationRequest request = new DeviceRegistrationRequest(macAddress, "Samsung Smart TV", "1.0", "Tizen");
            String fingerprintHash = "mocked_hash";

            when(tokenUtil.hashFingerprint(macAddress)).thenReturn(fingerprintHash);

            Device device = new Device();
            device.setMacAddress(macAddress);
            device.setFingerprintHash(fingerprintHash);
            when(deviceRepository.findByFingerprintHash(fingerprintHash)).thenReturn(Optional.of(device));

            UUID otherResellerId = UUID.randomUUID();
            ResellerCustomer existingClaim = new ResellerCustomer(otherResellerId, macAddress, "Name");
            when(resellerCustomerRepository.findByResellerIdAndMacAddress(resellerId, macAddress)).thenReturn(Optional.empty());
            when(resellerCustomerRepository.findByMacAddress(macAddress)).thenReturn(Optional.of(existingClaim));

            // Act & Assert
            assertThrows(ResourceAlreadyExistsException.class, () -> resellerService.createEndUser(resellerId, request));
            verify(resellerCustomerRepository, never()).save(any());
            verify(deviceRepository, never()).save(any());
        }
    }
