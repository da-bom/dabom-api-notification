package com.project.domain.webpush.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.common.support.StubCustomerIdResolver;
import com.project.domain.notification.service.NotificationService;
import com.project.domain.webpush.service.WebPushService;

@ExtendWith(MockitoExtension.class)
class WebPushControllerTest {

    @Mock private WebPushService webPushService;

    @Mock private NotificationService notificationService;

    @InjectMocks private WebPushController webPushController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(webPushController)
                        .setCustomArgumentResolvers(new StubCustomerIdResolver())
                        .build();
    }

    @Test
    @DisplayName("GET /push/vapid-public-key - VAPID 공개 키 조회 시 200 OK를 반환한다")
    void getVapidPublicKey_returns200() throws Exception {
        when(webPushService.getVapidPublicKey()).thenReturn("test-vapid-key");

        mockMvc.perform(get("/push/vapid-public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicKey").value("test-vapid-key"));
    }

    @Test
    @DisplayName("POST /push/subscribe - 푸시 구독(업서트) 시 200 OK를 반환한다")
    void subscribe_returns200() throws Exception {
        String requestBody =
                """
                {
                    "endpoint": "https://fcm.googleapis.com/fcm/send/test",
                    "keys": {
                        "p256dh": "test-p256dh-key",
                        "auth": "test-auth-key"
                    }
                }
                """;

        mockMvc.perform(
                        post("/push/subscribe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("DELETE /push/subscribe - 푸시 구독 해제 시 200 OK를 반환한다")
    void unsubscribe_returns200() throws Exception {
        mockMvc.perform(delete("/push/subscribe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST /push/send - 관리자 푸시 발송 시 200 OK를 반환한다")
    void sendPushMessage_returns200() throws Exception {
        String requestBody =
                """
                {
                    "customerId": 1,
                    "title": "테스트 제목",
                    "message": "테스트 메시지"
                }
                """;

        mockMvc.perform(
                        post("/push/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
