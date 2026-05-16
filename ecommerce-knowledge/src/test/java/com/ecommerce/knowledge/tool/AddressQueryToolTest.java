package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.AddressClient;
import com.ecommerce.knowledge.client.dto.AddressVO;
import com.ecommerce.knowledge.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressQueryToolTest {

    @Mock
    private AddressClient addressClient;

    @InjectMocks
    private AddressQueryTool addressQueryTool;

    @AfterEach
    void tearDown() {
        AgentUserContextHolder.clear();
    }

    @Test
    void queryCurrentUserAddressesReturnsAddressesForCurrentUser() {
        AgentUserContextHolder.set(new AgentUserContext(1002L, "USER"));
        AddressVO address = new AddressVO();
        address.setReceiverName("Alice");
        address.setIsDefault(1);
        when(addressClient.getCurrentUserAddresses(1002L)).thenReturn(Result.ok(List.of(address)));

        List<AddressVO> result = addressQueryTool.queryCurrentUserAddresses();

        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().getReceiverName());
        verify(addressClient).getCurrentUserAddresses(1002L);
    }

    @Test
    void queryCurrentUserAddressesReturnsEmptyListWhenCurrentUserMissing() {
        List<AddressVO> result = addressQueryTool.queryCurrentUserAddresses();

        assertTrue(result.isEmpty());
        verify(addressClient, never()).getCurrentUserAddresses(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void queryCurrentUserDefaultAddressReturnsDefaultAddress() {
        AgentUserContextHolder.set(new AgentUserContext(1002L, "USER"));
        AddressVO defaultAddress = new AddressVO();
        defaultAddress.setReceiverName("Alice");
        defaultAddress.setIsDefault(1);
        AddressVO secondaryAddress = new AddressVO();
        secondaryAddress.setReceiverName("Bob");
        secondaryAddress.setIsDefault(0);
        when(addressClient.getCurrentUserAddresses(1002L))
                .thenReturn(Result.ok(List.of(secondaryAddress, defaultAddress)));

        AddressVO result = addressQueryTool.queryCurrentUserDefaultAddress();

        assertEquals("Alice", result.getReceiverName());
    }

    @Test
    void queryCurrentUserDefaultAddressReturnsNullWhenCurrentUserMissing() {
        AddressVO result = addressQueryTool.queryCurrentUserDefaultAddress();

        assertNull(result);
        verify(addressClient, never()).getCurrentUserAddresses(org.mockito.ArgumentMatchers.anyLong());
    }
}
