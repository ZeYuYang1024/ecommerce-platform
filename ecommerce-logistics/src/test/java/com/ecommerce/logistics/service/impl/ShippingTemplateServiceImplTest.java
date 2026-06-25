package com.ecommerce.logistics.service.impl;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.dto.request.CreateShippingTemplateRequest;
import com.ecommerce.logistics.dto.request.UpdateShippingTemplateRequest;
import com.ecommerce.logistics.dto.response.ShippingTemplateVO;
import com.ecommerce.logistics.entity.ShippingTemplate;
import com.ecommerce.logistics.mapper.ShippingTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingTemplateServiceImplTest {

    @Mock
    private ShippingTemplateMapper shippingTemplateMapper;

    @InjectMocks
    private ShippingTemplateServiceImpl service;

    @Test
    void shouldRejectTemplateAccessForDifferentMerchant() {
        when(shippingTemplateMapper.selectById(1L)).thenReturn(template(1L, 66L));

        assertThatThrownBy(() -> service.getTemplate(1L, 88L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                .isEqualTo(LogisticsErrorCode.TEMPLATE_FORBIDDEN.getCode());
    }

    @Test
    void shouldRejectTemplateDeleteForDifferentMerchant() {
        when(shippingTemplateMapper.selectById(1L)).thenReturn(template(1L, 66L));

        assertThatThrownBy(() -> service.deleteTemplate(1L, 88L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                .isEqualTo(LogisticsErrorCode.TEMPLATE_FORBIDDEN.getCode());

        verify(shippingTemplateMapper, never()).deleteById(1L);
    }

    @Test
    void shouldPreserveExistingMerchantIdWhenUpdateRequestOmitsIt() {
        when(shippingTemplateMapper.selectById(1L)).thenReturn(template(1L, 66L));

        UpdateShippingTemplateRequest request = new UpdateShippingTemplateRequest();
        request.setTemplateName("updated-template");
        request.setCalcType(1);
        request.setFirstUnit(2);
        request.setFirstFee(BigDecimal.valueOf(12.50));

        ShippingTemplateVO vo = service.updateTemplate(1L, request, 66L);

        assertThat(vo.getMerchantId()).isEqualTo(66L);
        verify(shippingTemplateMapper).updateById(argThat((ShippingTemplate template) ->
                Long.valueOf(66L).equals(template.getMerchantId())
                        && "updated-template".equals(template.getTemplateName())));
    }

    private ShippingTemplate template(Long id, Long merchantId) {
        ShippingTemplate template = new ShippingTemplate();
        template.setId(id);
        template.setMerchantId(merchantId);
        template.setTemplateName("origin-template");
        template.setCalcType(0);
        template.setFirstUnit(1);
        template.setFirstFee(BigDecimal.TEN);
        template.setContinueUnit(1);
        template.setContinueFee(BigDecimal.ONE);
        return template;
    }
}
