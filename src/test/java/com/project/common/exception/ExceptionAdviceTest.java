package com.project.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Test
    @DisplayName("Spring MVC 예외 발생 시 details에 원본 메시지를 포함한다")
    void springMvcException_includesDetailsMessage() throws Exception {
        mockMvc.perform(
                        post("/test/validation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_002"))
                .andExpect(jsonPath("$.error.details").exists());
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

        @PostMapping("/test/validation")
        public void validation(@Valid @RequestBody StubRequest request) {
            // 유효성 검증 실패를 유도하기 위한 빈 구현 (요청 바인딩만 수행)
        }
    }

    record StubRequest(@NotBlank String name) {}
}
