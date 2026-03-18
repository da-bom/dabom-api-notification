package com.project.domain.notification.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.project.common.auth.aop.CustomerId;
import com.project.domain.notification.dto.NotificationSlice;
import com.project.domain.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final Long CUSTOMER_ID = 1L;

    @Mock private NotificationService notificationService;

    @InjectMocks private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(notificationController)
                        .setCustomArgumentResolvers(new StubCustomerIdResolver())
                        .build();
    }

    @Test
    @DisplayName("GET /notifications - 알림 목록 조회 시 200 OK를 반환한다")
    void getNotifications_returns200() throws Exception {
        when(notificationService.getNotifications(
                        anyLong(), isNull(), anyInt(), isNull(), isNull()))
                .thenReturn(new NotificationSlice(List.of(), null, false, 0L));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /notifications/unread-count - 읽지 않은 알림 수 조회 시 200 OK를 반환한다")
    void getUnreadCount_returns200() throws Exception {
        when(notificationService.getUnreadCount(anyLong())).thenReturn(5L);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read - 알림 읽음 처리 시 200 OK를 반환한다")
    void markAsRead_returns200() throws Exception {
        mockMvc.perform(patch("/notifications/{notificationId}/read", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /notifications/read-all - 전체 읽음 처리 시 200 OK를 반환한다")
    void markAllAsRead_returns200() throws Exception {
        mockMvc.perform(patch("/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("DELETE /notifications/{id} - 알림 삭제 시 200 OK를 반환한다")
    void deleteNotification_returns200() throws Exception {
        mockMvc.perform(delete("/notifications/{notificationId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * @CustomerId 파라미터를 고정값으로 주입하는 스텁 리졸버
     */
    static class StubCustomerIdResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CustomerId.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return CUSTOMER_ID;
        }
    }
}
