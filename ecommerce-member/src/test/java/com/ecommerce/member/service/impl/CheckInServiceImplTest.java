package com.ecommerce.member.service.impl;

import com.ecommerce.member.dto.response.CheckInStatusVO;
import com.ecommerce.member.entity.CheckInRecord;
import com.ecommerce.member.mapper.CheckInRecordMapper;
import com.ecommerce.member.service.PointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceImplTest {

    @Mock
    private CheckInRecordMapper checkInRecordMapper;

    @Mock
    private PointsService pointsService;

    @InjectMocks
    private CheckInServiceImpl service;

    @Test
    void getStatusShouldReturnTodayConsecutiveDaysWhenUserAlreadyCheckedInToday() {
        CheckInRecord todayRecord = new CheckInRecord();
        todayRecord.setConsecutiveDays(3);
        todayRecord.setPointsAwarded(1);

        when(checkInRecordMapper.selectOne(any()))
                .thenReturn(todayRecord)
                .thenReturn(null);

        CheckInStatusVO result = service.getStatus(1001L);

        assertThat(result.getCheckedToday()).isTrue();
        assertThat(result.getConsecutiveDays()).isEqualTo(3);
        assertThat(result.getPointsAwardedToday()).isEqualTo(todayRecord.getPointsAwarded());
    }
}
