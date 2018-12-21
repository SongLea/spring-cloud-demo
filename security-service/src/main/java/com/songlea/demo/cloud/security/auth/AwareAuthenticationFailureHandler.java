package com.songlea.demo.cloud.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.songlea.demo.cloud.security.config.LocaleMessageConfig;
import com.songlea.demo.cloud.security.exceptions.AuthMethodNotSupportedException;
import com.songlea.demo.cloud.security.exceptions.InvalidJwtTokenException;
import com.songlea.demo.cloud.security.exceptions.JwtTokenExpiredException;
import com.songlea.demo.cloud.security.exceptions.NoUsernameOrPasswordException;
import com.songlea.demo.cloud.security.model.ErrorCode;
import com.songlea.demo.cloud.security.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 验证失败处理器,默认的为SimpleUrlAuthenticationFailureHandler
 */
@Component
public class AwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwareAuthenticationFailureHandler.class);

    private final ObjectMapper mapper;
    private final LocaleMessageConfig localeMessageConfig;

    @Autowired
    public AwareAuthenticationFailureHandler(ObjectMapper mapper, LocaleMessageConfig localeMessageConfig) {
        this.mapper = mapper;
        this.localeMessageConfig = localeMessageConfig;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException {
        LOGGER.error("access authentication failure handler, url:{} " + request.getRequestURI(), e);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        if (e instanceof BadCredentialsException || e instanceof UsernameNotFoundException
                || e instanceof NoUsernameOrPasswordException) {
            // 用户名或密码错误
            mapper.writeValue(response.getWriter(), ErrorResponse.of(
                    localeMessageConfig.getMessage(ErrorResponse.INVALID_USERNAME_OR_PASSWORD),
                    ErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (e instanceof AuthMethodNotSupportedException) {
            // 验证的请求方式不正确
            mapper.writeValue(response.getWriter(), ErrorResponse.of(
                    localeMessageConfig.getMessage(ErrorResponse.AUTHENTICATION_METHOD_NOT_SUPPORTED),
                    ErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (e instanceof InsufficientAuthenticationException) {
            // 用户未分配角色
            mapper.writeValue(response.getWriter(), ErrorResponse.of(
                    localeMessageConfig.getMessage(ErrorResponse.USER_HAS_NO_ROLES_ASSIGNED),
                    ErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (e instanceof JwtTokenExpiredException || e instanceof InvalidJwtTokenException) {
            // token过期
            mapper.writeValue(response.getWriter(), ErrorResponse.of(
                    localeMessageConfig.getMessage(ErrorResponse.TOKEN_HAS_EXPIRED_OR_INVALID),
                    ErrorCode.JWT_TOKEN_EXPIRED_OR_INVALID, HttpStatus.UNAUTHORIZED));
        }
        // 验证失败
        mapper.writeValue(response.getWriter(), ErrorResponse.of(
                localeMessageConfig.getMessage(ErrorResponse.AUTHENTICATION_FAILED),
                ErrorCode.GLOBAL, HttpStatus.UNAUTHORIZED));
    }
}
