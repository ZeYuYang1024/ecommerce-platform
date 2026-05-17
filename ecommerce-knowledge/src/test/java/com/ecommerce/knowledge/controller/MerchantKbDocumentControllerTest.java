package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.dto.request.CreateDocumentRequest;
import com.ecommerce.knowledge.service.KbDocumentService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantKbDocumentControllerTest {

    @Test
    void create_shouldDelegateToMerchantDocumentServiceMethod() throws Exception {
        RecordingHandler handler = new RecordingHandler();
        KbDocumentService service = (KbDocumentService) Proxy.newProxyInstance(
                KbDocumentService.class.getClassLoader(),
                new Class<?>[]{KbDocumentService.class},
                handler);

        Class<?> controllerClass = Class.forName("com.ecommerce.knowledge.controller.MerchantKbDocumentController");
        Object controller = controllerClass.getConstructor(KbDocumentService.class).newInstance(service);

        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setCategoryId(11L);
        request.setTitle("商家文档");
        request.setContent("文档内容");

        Method createMethod = controllerClass.getMethod("create", Long.class, CreateDocumentRequest.class);
        createMethod.invoke(controller, 2001L, request);

        assertThat(handler.methodName).isEqualTo("createForMerchant");
        assertThat(handler.args).containsExactly(2001L, request);
    }

    private static class RecordingHandler implements InvocationHandler {

        private String methodName;
        private Object[] args;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            this.methodName = method.getName();
            this.args = args == null ? new Object[0] : Arrays.copyOf(args, args.length);
            return null;
        }
    }
}
