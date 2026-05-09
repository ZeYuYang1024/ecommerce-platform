package com.ecommerce.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.merchant.common.MerchantErrorCode;
import com.ecommerce.merchant.dto.request.MerchantAuditRequest;
import com.ecommerce.merchant.dto.request.MerchantRegisterRequest;
import com.ecommerce.merchant.dto.response.MerchantVO;
import com.ecommerce.merchant.entity.Merchant;
import com.ecommerce.merchant.entity.MerchantAudit;
import com.ecommerce.merchant.mapper.MerchantAuditMapper;
import com.ecommerce.merchant.mapper.MerchantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantAuditMapper auditMapper;
    @InjectMocks private MerchantServiceImpl service;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = new Merchant();
        merchant.setId(1L);
        merchant.setName("测试店铺");
        merchant.setContactName("张三");
        merchant.setContactPhone("13800138000");
        merchant.setBusinessLicense("https://example.com/lic.jpg");
        merchant.setStatus(0);
    }

    @Nested
    class RegisterTests {
        @Test
        void shouldCreateMerchant() {
            when(merchantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(merchantMapper.insert(any(Merchant.class))).thenReturn(1);

            MerchantRegisterRequest req = new MerchantRegisterRequest();
            req.setName("新店铺"); req.setContactName("李四");
            req.setContactPhone("13900139000"); req.setBusinessLicense("url");

            MerchantVO vo = service.register(req);
            assertThat(vo.getName()).isEqualTo("新店铺");
            assertThat(vo.getStatus()).isEqualTo(0);
            assertThat(vo.getStatusText()).isEqualTo("待审核");
        }

        @Test
        void shouldRejectDuplicateName() {
            when(merchantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            MerchantRegisterRequest req = new MerchantRegisterRequest();
            req.setName("测试店铺"); req.setContactName("x"); req.setContactPhone("x"); req.setBusinessLicense("x");

            assertThatThrownBy(() -> service.register(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(MerchantErrorCode.MERCHANT_NAME_EXISTS.getCode());
        }
    }

    @Nested
    class AuditTests {
        @Test
        void shouldApprovePendingMerchant() {
            when(merchantMapper.selectById(1L)).thenReturn(merchant);
            when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
            when(auditMapper.insert(any(MerchantAudit.class))).thenReturn(1);

            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(1); req.setComment("通过");

            MerchantVO vo = service.audit(1L, req, 1L);
            assertThat(vo.getStatus()).isEqualTo(1);
        }

        @Test
        void shouldRejectNonPending() {
            merchant.setStatus(1); // already approved
            when(merchantMapper.selectById(1L)).thenReturn(merchant);

            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(1);

            assertThatThrownBy(() -> service.audit(1L, req, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(MerchantErrorCode.MERCHANT_NOT_PENDING.getCode());
        }

        @Test
        void shouldRejectInvalidAction() {
            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(99);

            assertThatThrownBy(() -> service.audit(1L, req, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(MerchantErrorCode.INVALID_AUDIT_ACTION.getCode());
        }
    }

    @Nested
    class QueryTests {
        @Test
        void shouldReturnMerchantById() {
            when(merchantMapper.selectById(1L)).thenReturn(merchant);
            MerchantVO vo = service.getById(1L);
            assertThat(vo.getName()).isEqualTo("测试店铺");
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(merchantMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldListAllByStatus() {
            when(merchantMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(java.util.Collections.singletonList(merchant));
            List<MerchantVO> list = service.list(0);
            assertThat(list).hasSize(1);
        }

        @Test
        void shouldListAllWhenStatusNull() {
            when(merchantMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(java.util.Collections.singletonList(merchant));
            List<MerchantVO> list = service.list(null);
            assertThat(list).hasSize(1);
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldHandleMaxLengthShopName() {
            String longName = "这是一个非常非常长的店铺名称用来测试边界值".repeat(3);
            when(merchantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(merchantMapper.insert(any(Merchant.class))).thenReturn(1);
            MerchantRegisterRequest req = new MerchantRegisterRequest();
            req.setName(longName); req.setContactName("x"); req.setContactPhone("x"); req.setBusinessLicense("x");
            MerchantVO vo = service.register(req);
            assertThat(vo.getName()).isEqualTo(longName);
        }

        @Test
        void shouldHandleSingleCharShopName() {
            when(merchantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(merchantMapper.insert(any(Merchant.class))).thenReturn(1);
            MerchantRegisterRequest req = new MerchantRegisterRequest();
            req.setName("A"); req.setContactName("x"); req.setContactPhone("x"); req.setBusinessLicense("x");
            MerchantVO vo = service.register(req);
            assertThat(vo.getName()).isEqualTo("A");
        }

        @Test
        void shouldAuditWithReject() {
            when(merchantMapper.selectById(1L)).thenReturn(merchant);
            when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
            when(auditMapper.insert(any(MerchantAudit.class))).thenReturn(1);
            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(2); req.setComment("资质不全");
            MerchantVO vo = service.audit(1L, req, 1L);
            assertThat(vo.getStatus()).isEqualTo(2);
            assertThat(vo.getStatusText()).isEqualTo("已驳回");
        }

        @Test
        void shouldAuditWithBan() {
            when(merchantMapper.selectById(1L)).thenReturn(merchant);
            when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
            when(auditMapper.insert(any(MerchantAudit.class))).thenReturn(1);
            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(3);
            MerchantVO vo = service.audit(1L, req, 1L);
            assertThat(vo.getStatus()).isEqualTo(3);
            assertThat(vo.getStatusText()).isEqualTo("已关停");
        }

        @Test
        void shouldAuditWithNullComment() {
            when(merchantMapper.selectById(1L)).thenReturn(merchant);
            when(merchantMapper.updateById(any(Merchant.class))).thenReturn(1);
            when(auditMapper.insert(any(MerchantAudit.class))).thenReturn(1);
            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(1); req.setComment(null);
            assertThatCode(() -> service.audit(1L, req, 1L)).doesNotThrowAnyException();
        }

        @Test
        void shouldHandleActionBoundaryZero() {
            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(0);
            assertThatThrownBy(() -> service.audit(1L, req, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldHandleActionBoundaryFour() {
            MerchantAuditRequest req = new MerchantAuditRequest();
            req.setAction(4);
            assertThatThrownBy(() -> service.audit(1L, req, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldListWithStatusAlreadyApproved() {
            merchant.setStatus(1);
            when(merchantMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(java.util.Collections.singletonList(merchant));
            List<MerchantVO> list = service.list(1);
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStatusText()).isEqualTo("已通过");
        }

        @Test
        void shouldListEmptyWhenNoMerchants() {
            when(merchantMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(java.util.Collections.emptyList());
            List<MerchantVO> list = service.list(null);
            assertThat(list).isEmpty();
        }

        @Test
        void shouldGetByIdWithMaxLong() {
            when(merchantMapper.selectById(Long.MAX_VALUE)).thenReturn(null);
            assertThatThrownBy(() -> service.getById(Long.MAX_VALUE))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
