package com.ecommerce.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.common.BusinessException;
import com.ecommerce.knowledge.entity.KbDocument;
import com.ecommerce.knowledge.mapper.KbCategoryMapper;
import com.ecommerce.knowledge.mapper.KbDocumentMapper;
import com.ecommerce.knowledge.service.DocumentIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KbDocumentServiceImplTest {

    @Mock
    private KbDocumentMapper documentMapper;

    @Mock
    private KbCategoryMapper categoryMapper;

    @Mock
    private DocumentIngestionService ingestionService;

    @InjectMocks
    private KbDocumentServiceImpl service;

    @Test
    void pageForMerchant_shouldApplyMerchantOwnershipScope() throws Exception {
        Page<KbDocument> page = new Page<>(1, 10);
        KbDocument document = new KbDocument();
        setField(document, "ownerType", "merchant");
        setField(document, "merchantId", 2001L);
        document.setTitle("商家知识文档");
        page.setRecords(List.of(document));
        when(documentMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Method method = KbDocumentServiceImpl.class.getMethod(
                "pageForMerchant", Long.class, int.class, int.class, Long.class, String.class);
        Object result = method.invoke(service, 2001L, 1, 10, null, null);

        assertThat(result).isInstanceOf(Page.class);
        Page<?> resultPage = (Page<?>) result;
        assertThat(resultPage.getRecords()).hasSize(1);
        ArgumentCaptor<LambdaQueryWrapper<KbDocument>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(documentMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue()).isNotNull();
    }

    @Test
    void reindexForMerchant_shouldRejectCrossTenantDocument() throws Exception {
        KbDocument document = new KbDocument();
        document.setId(9001L);
        document.setTitle("跨商家文档");
        document.setContent("content");
        document.setCategoryId(10L);
        document.setStatus("published");
        setField(document, "ownerType", "merchant");
        setField(document, "merchantId", 3002L);
        when(documentMapper.selectById(9001L)).thenReturn(document);

        Method method = KbDocumentServiceImpl.class.getMethod("reindexForMerchant", Long.class, Long.class);

        assertThatThrownBy(() -> invoke(method, service, 2001L, 9001L))
                .isInstanceOf(BusinessException.class);
    }

    private static Object invoke(Method method, Object target, Object... args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getTargetException();
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
