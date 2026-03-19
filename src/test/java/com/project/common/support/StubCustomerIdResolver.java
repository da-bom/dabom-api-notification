package com.project.common.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.project.common.auth.aop.CustomerId;

/** 테스트에서 @CustomerId 파라미터를 고정값으로 주입하는 스텁 리졸버 */
public class StubCustomerIdResolver implements HandlerMethodArgumentResolver {

    public static final Long CUSTOMER_ID = 1L;

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
