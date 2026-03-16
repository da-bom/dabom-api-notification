package com.project.domain.webpush.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.webpush.controller.dto.PushSubscriptionRequest;
import com.project.domain.webpush.entity.PushSubscription;
import com.project.domain.webpush.repository.PushSubscriptionRepository;
import com.project.global.exception.ApplicationException;

import nl.martijndwars.webpush.PushService;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebPushServiceImpl 단위 테스트")
class WebPushServiceImplTest {

    @Mock private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private PushService pushService;
    @Mock private CloseableHttpClient pushHttpClient;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private WebPushServiceImpl webPushService;

    private static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/test-token";
    private static final String OTHER_ENDPOINT = "https://fcm.googleapis.com/fcm/send/other-token";
    private static final String P256DH = "test-p256dh-key";
    private static final String AUTH = "test-auth-key";
    private static final Long CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;

    private PushSubscriptionRequest createRequest(String endpoint) {
        return new PushSubscriptionRequest(endpoint, Map.of("p256dh", P256DH, "auth", AUTH));
    }

    private PushSubscription createSubscription(String endpoint, Long customerId) {
        return PushSubscription.builder()
                .endpoint(endpoint)
                .p256dh(P256DH)
                .auth(AUTH)
                .customerId(customerId)
                .build();
    }

    @Nested
    @DisplayName("subscribe - 4-case 분기 로직")
    class Subscribe {

        @Test
        @DisplayName("Case 1: 기존 endpoint + 같은 customer → updateSubscription 호출")
        void sameEndpointSameCustomer_updatesSubscription() {
            PushSubscription existing = createSubscription(ENDPOINT, CUSTOMER_ID);
            when(pushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .thenReturn(Optional.of(existing));
            when(pushSubscriptionRepository.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Optional.of(existing));

            webPushService.subscribe(createRequest(ENDPOINT), CUSTOMER_ID);

            verify(pushSubscriptionRepository, never()).delete(any());
            verify(pushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Case 2: 기존 endpoint + 다른 customer (기존 고객 구독 있음) → 삭제 후 reassign")
        void sameEndpointDifferentCustomer_withExistingSubscription_deletesAndReassigns() {
            PushSubscription endpointSub = createSubscription(ENDPOINT, OTHER_CUSTOMER_ID);
            PushSubscription customerSub = createSubscription(OTHER_ENDPOINT, CUSTOMER_ID);
            when(pushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .thenReturn(Optional.of(endpointSub));
            when(pushSubscriptionRepository.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Optional.of(customerSub));

            webPushService.subscribe(createRequest(ENDPOINT), CUSTOMER_ID);

            verify(pushSubscriptionRepository).delete(customerSub);
            verify(pushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Case 2b: 기존 endpoint + 다른 customer (기존 고객 구독 없음) → reassign만")
        void sameEndpointDifferentCustomer_withoutExistingSubscription_reassignsOnly() {
            PushSubscription endpointSub = createSubscription(ENDPOINT, OTHER_CUSTOMER_ID);
            when(pushSubscriptionRepository.findByEndpoint(ENDPOINT))
                    .thenReturn(Optional.of(endpointSub));
            when(pushSubscriptionRepository.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Optional.empty());

            webPushService.subscribe(createRequest(ENDPOINT), CUSTOMER_ID);

            verify(pushSubscriptionRepository, never()).delete(any());
            verify(pushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Case 3: 새 endpoint + 기존 customer → updateSubscription 호출")
        void newEndpointExistingCustomer_updatesSubscription() {
            PushSubscription customerSub = createSubscription(OTHER_ENDPOINT, CUSTOMER_ID);
            when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty());
            when(pushSubscriptionRepository.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Optional.of(customerSub));

            webPushService.subscribe(createRequest(ENDPOINT), CUSTOMER_ID);

            verify(pushSubscriptionRepository, never()).delete(any());
            verify(pushSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Case 4: 새 endpoint + 새 customer → save 호출")
        void newEndpointNewCustomer_savesNewSubscription() {
            when(pushSubscriptionRepository.findByEndpoint(ENDPOINT)).thenReturn(Optional.empty());
            when(pushSubscriptionRepository.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Optional.empty());

            webPushService.subscribe(createRequest(ENDPOINT), CUSTOMER_ID);

            verify(pushSubscriptionRepository).save(any(PushSubscription.class));
        }
    }

    @Nested
    @DisplayName("subscribe - endpoint 검증")
    class ValidateEndpoint {

        @Test
        @DisplayName("HTTP endpoint → INVALID_ENDPOINT_URL 예외")
        void httpEndpoint_throwsException() {
            PushSubscriptionRequest request =
                    createRequest("http://fcm.googleapis.com/fcm/send/test");

            assertThatThrownBy(() -> webPushService.subscribe(request, CUSTOMER_ID))
                    .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("잘못된 URI → INVALID_ENDPOINT_URL 예외")
        void invalidUri_throwsException() {
            PushSubscriptionRequest request = createRequest("not a valid uri");

            assertThatThrownBy(() -> webPushService.subscribe(request, CUSTOMER_ID))
                    .isInstanceOf(ApplicationException.class);
        }
    }
}
