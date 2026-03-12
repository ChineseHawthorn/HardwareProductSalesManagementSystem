package com.HardwareProductSalesManagementSystem.exception;

import com.HardwareProductSalesManagementSystem.commons.APIRes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(Exception.class)  //捕获所有异常
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  //状态码
    @ResponseBody
    public APIRes handleAllExceptions(Exception e, WebRequest webRequest) {
        return APIRes.fail(500, "500错误");
    }
}