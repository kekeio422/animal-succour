package com.fast.system.general.core.exception;

import com.fast.system.general.core.domain.AjaxResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author huacai
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理RuntimeException异常
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(HttpServletRequest request, RuntimeException e) {
        String requestURI = request.getRequestURI();
        return AjaxResult.error(e.getMessage());
    }
}