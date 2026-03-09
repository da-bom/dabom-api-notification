package com.project.global.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.project.global.auth.JwtTokenUtil;
import com.project.global.auth.LoginInterceptor;
import com.project.global.auth.aop.CustomerArgumentResolver;
import com.project.global.util.NetworkValidator;

import nl.martijndwars.webpush.PushService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtTokenUtil jwtTokenUtil;

    @Value("${vapid.key.public}")
    private String vapidPublicKey;

    @Value("${vapid.key.private}")
    private String vapidPrivateKey;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${push-client.connect-timeout:5000}")
    private int pushConnectTimeout;

    @Value("${push-client.socket-timeout:5000}")
    private int pushSocketTimeout;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(jwtTokenUtil))
                .excludePathPatterns(
                        "/auth/**",
                        "/admin/auth/login",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CustomerArgumentResolver(jwtTokenUtil));
    }

    @Bean
    public CloseableHttpClient pushHttpClient() {
        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setConnectTimeout(pushConnectTimeout)
                        .setSocketTimeout(pushSocketTimeout)
                        .build();

        DnsResolver ssrfSafeDnsResolver =
                host -> {
                    InetAddress[] addresses = InetAddress.getAllByName(host);
                    for (InetAddress addr : addresses) {
                        if (NetworkValidator.isInternalAddress(addr)) {
                            throw new UnknownHostException("Blocked internal address: " + host);
                        }
                    }
                    return addresses;
                };

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .setDnsResolver(ssrfSafeDnsResolver)
                .build();
    }

    @Bean
    public PushService pushService() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        return new PushService(vapidPublicKey, vapidPrivateKey);
    }
}
