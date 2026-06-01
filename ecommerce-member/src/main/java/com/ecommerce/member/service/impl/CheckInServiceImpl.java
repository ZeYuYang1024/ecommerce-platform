package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.member.common.MemberErrorCode;
import com.ecommerce.member.dto.response.CheckInStatusVO;
import com.ecommerce.member.entity.CheckInRecord;
import com.ecommerce.member.mapper.CheckInRecordMapper;
import com.ecommerce.member.service.CheckInService;
import com.ecommerce.member.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final CheckInRecordMapper checkInRecordMapper;
    private final PointsService pointsService;

    private static final int BASE_POINTS = 1;
    private static final int CONSECUTIVE_BONUS_DAYS = 7;
    private static final int CONSECUTIVE_BONUS_POINTS = 5;

    @Override
    @Transactional
    public CheckInStatusVO checkIn(Long userId) {
        LocalDate today = LocalDate.now();

        // 检查今日是否已签到
        Long exists = checkInRecordMapper.selectCount(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getCheckDate, today));
        if (exists > 0) {
            throw new BusinessException(MemberErrorCode.ALREADY_CHECKED_IN);
        }

        // 查询昨日签到记录，计算连续天数
        LocalDate yesterday = today.minusDays(1);
        CheckInRecord yesterdayRecord = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getCheckDate, yesterday));
        int consecutiveDays = yesterdayRecord != null ? yesterdayRecord.getConsecutiveDays() + 1 : 1;

        // 计算积分
        int points = BASE_POINTS;
        if (consecutiveDays % CONSECUTIVE_BONUS_DAYS == 0) {
            points += CONSECUTIVE_BONUS_POINTS;
        }

        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String bizKey = "CHECKIN:" + userId + ":" + dateStr;

        // 保存签到记录
        CheckInRecord record = new CheckInRecord();
        record.setId(SnowflakeUtils.nextId());
        record.setUserId(userId);
        record.setCheckDate(today);
        record.setConsecutiveDays(consecutiveDays);
        record.setPointsAwarded(points);
        record.setBizKey(bizKey);
        checkInRecordMapper.insert(record);

        // 发放积分
        pointsService.earn(userId, points, "CHECKIN", dateStr, bizKey, "每日签到");

        CheckInStatusVO vo = new CheckInStatusVO();
        vo.setCheckedToday(true);
        vo.setConsecutiveDays(consecutiveDays);
        vo.setPointsAwardedToday(points);
        return vo;
    }

    @Override
    public CheckInStatusVO getStatus(Long userId) {
        LocalDate today = LocalDate.now();

        CheckInRecord todayRecord = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getCheckDate, today));

        // 查询昨天的记录获取连续天数
        LocalDate yesterday = today.minusDays(1);
        CheckInRecord yesterdayRecord = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getCheckDate, yesterday));
        int consecutiveDays;
        if (todayRecord != null) {
            consecutiveDays = todayRecord.getConsecutiveDays();
        } else {
            consecutiveDays = yesterdayRecord != null ? yesterdayRecord.getConsecutiveDays() : 0;
        }

        CheckInStatusVO vo = new CheckInStatusVO();
        vo.setCheckedToday(todayRecord != null);
        vo.setConsecutiveDays(consecutiveDays);
        vo.setPointsAwardedToday(todayRecord != null ? todayRecord.getPointsAwarded() : 0);
        return vo;
    }
}
