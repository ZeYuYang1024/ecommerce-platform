package com.ecommerce.gateway;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GlobalExceptionHandlerWebFluxCompatibilityTest {

    @Test
    void globalExceptionHandlerMethodsAreLoadableWithoutServletApi() {
        assertDoesNotThrow(GlobalExceptionHandler.class::getDeclaredMethods);
    }
}
