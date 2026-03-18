package com.project.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.exception.code.NotificationErrorCode;

class ExceptionAdviceTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new StubController())
                        .setControllerAdvice(new ExceptionAdvice())
                        .build();
    }

    @Test
    @DisplayName("도메인 예외 발생 시 ApiResponse.fail() 형식으로 응답한다")
    void domainException_returnsApiResponseFail() throws Exception {
        mockMvc.perform(get("/test/domain-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_003"))
                .andExpect(jsonPath("$.error.message").value("알림을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("미처리 예외 발생 시 ApiResponse.fail() 형식으로 500 응답한다")
    void unhandledException_returnsApiResponseFail() throws Exception {
        mockMvc.perform(get("/test/unhandled-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_001"))
                .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /** 테스트용 스텁 컨트롤러 */
    @RestController
    static class StubController {

        @GetMapping("/test/domain-exception")
        public void domainException() {
            throw new ApplicationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }

        @GetMapping("/test/unhandled-exception")
        public void unhandledException() {
            throw new RuntimeException("unexpected error");
        }
    }
}
