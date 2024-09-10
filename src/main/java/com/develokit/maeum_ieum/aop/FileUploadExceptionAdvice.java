package com.develokit.maeum_ieum.aop;

import com.develokit.maeum_ieum.ex.CustomValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

public class FileUploadExceptionAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxSizeException(MaxUploadSizeExceededException exc) {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("img", "파일 크기는 10MB를 초과할 수 없습니다.");

        throw new CustomValidationException("유효성 검사 실패", errorMap);
    }
}
