package com.example.agribackend.service.impl;

import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.entity.WarningLogEntity;
import com.example.agribackend.entity.WarningRuleEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.mapper.WarningLogMapper;
import com.example.agribackend.mapper.WarningRuleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 预警服务单元测试
 * 使用 Mockito 模拟数据库和WebSocket，测试预警触发逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预警服务单元测试")
class WarningServiceImplTest {

    @Mock
    private EnvDataMapper envDataMapper;

    @Mock
    private WarningRuleMapper warningRuleMapper;

    @Mock
    private WarningLogMapper warningLogMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WarningServiceImpl warningService;

    // ==================== checkWarnings 测试 ====================

    @Test
    @DisplayName("无环境数据时不触发预警")
    void checkWarnings_noData_noWarning() {
        when(envDataMapper.selectOne(any())).thenReturn(null);

        warningService.checkWarnings();

        verify(warningLogMapper, never()).insert(any(WarningLogEntity.class));
    }

    @Test
    @DisplayName("无启用规则时不触发预警")
    void checkWarnings_noRules_noWarning() {
        EnvDataEntity data = new EnvDataEntity();
        data.setTemperature(25.0);
        data.setSensorId(1);

        when(envDataMapper.selectOne(any())).thenReturn(data);
        when(warningRuleMapper.selectList(any())).thenReturn(Collections.emptyList());

        warningService.checkWarnings();

        verify(warningLogMapper, never()).insert(any(WarningLogEntity.class));
    }

    @Test
    @DisplayName("温度超过最大阈值 → 触发高温预警")
    void checkWarnings_tempExceedsMax_triggersWarning() {
        // 模拟：当前温度 40°C，规则最大 35°C
        EnvDataEntity data = new EnvDataEntity();
        data.setTemperature(40.0);
        data.setHumidity(60.0);
        data.setSensorId(1);
        when(envDataMapper.selectOne(any())).thenReturn(data);

        WarningRuleEntity rule = new WarningRuleEntity();
        rule.setId(1);
        rule.setSensorType("temp_hum");
        rule.setMinValue(10.0);
        rule.setMaxValue(35.0);
        rule.setEnabled(1);
        when(warningRuleMapper.selectList(any())).thenReturn(List.of(rule));
        when(warningLogMapper.insert(any(WarningLogEntity.class))).thenReturn(1);

        warningService.checkWarnings();

        // 温度超标应插入预警日志
        verify(warningLogMapper, atLeastOnce()).insert(any(WarningLogEntity.class));
    }

    @Test
    @DisplayName("温度低于最小阈值 → 触发低温预警")
    void checkWarnings_tempBelowMin_triggersWarning() {
        EnvDataEntity data = new EnvDataEntity();
        data.setTemperature(5.0);
        data.setSensorId(1);
        when(envDataMapper.selectOne(any())).thenReturn(data);

        WarningRuleEntity rule = new WarningRuleEntity();
        rule.setId(1);
        rule.setSensorType("temp_hum");
        rule.setMinValue(10.0);
        rule.setMaxValue(35.0);
        rule.setEnabled(1);
        when(warningRuleMapper.selectList(any())).thenReturn(List.of(rule));
        when(warningLogMapper.insert(any(WarningLogEntity.class))).thenReturn(1);

        warningService.checkWarnings();

        verify(warningLogMapper, atLeastOnce()).insert(any(WarningLogEntity.class));
    }

    @Test
    @DisplayName("所有数据在正常范围内 → 不触发预警")
    void checkWarnings_normalData_noWarning() {
        EnvDataEntity data = new EnvDataEntity();
        data.setTemperature(25.0);
        data.setHumidity(60.0);
        data.setSoilAdc(2000);
        data.setLightIntensity(1500);
        data.setCo2(600);
        data.setSensorId(1);
        when(envDataMapper.selectOne(any())).thenReturn(data);

        WarningRuleEntity tempRule = new WarningRuleEntity();
        tempRule.setId(1);
        tempRule.setSensorType("temp_hum");
        tempRule.setMinValue(10.0);
        tempRule.setMaxValue(35.0);
        tempRule.setEnabled(1);

        WarningRuleEntity co2Rule = new WarningRuleEntity();
        co2Rule.setId(2);
        co2Rule.setSensorType("co2");
        co2Rule.setMinValue(300.0);
        co2Rule.setMaxValue(1000.0);
        co2Rule.setEnabled(1);

        when(warningRuleMapper.selectList(any())).thenReturn(List.of(tempRule, co2Rule));

        warningService.checkWarnings();

        verify(warningLogMapper, never()).insert(any(WarningLogEntity.class));
    }

    @Test
    @DisplayName("CO₂超标触发预警")
    void checkWarnings_co2Exceeds_triggersWarning() {
        EnvDataEntity data = new EnvDataEntity();
        data.setCo2(1500);
        data.setSensorId(1);
        when(envDataMapper.selectOne(any())).thenReturn(data);

        WarningRuleEntity rule = new WarningRuleEntity();
        rule.setId(1);
        rule.setSensorType("co2");
        rule.setMinValue(300.0);
        rule.setMaxValue(1000.0);
        rule.setEnabled(1);
        when(warningRuleMapper.selectList(any())).thenReturn(List.of(rule));
        when(warningLogMapper.insert(any(WarningLogEntity.class))).thenReturn(1);

        warningService.checkWarnings();

        verify(warningLogMapper, atLeastOnce()).insert(any(WarningLogEntity.class));
    }

    // ==================== markAsHandled 测试 ====================

    @Test
    @DisplayName("标记预警为已处理 - 成功")
    void markAsHandled_success() {
        when(warningLogMapper.update(any(), any())).thenReturn(1);

        assertTrue(warningService.markAsHandled(1));
    }

    @Test
    @DisplayName("标记不存在的预警 - 返回false")
    void markAsHandled_notFound() {
        when(warningLogMapper.update(any(), any())).thenReturn(0);

        assertFalse(warningService.markAsHandled(999));
    }

    // ==================== getWarningLogsPaged 测试 ====================

    @Test
    @DisplayName("分页查询预警日志 - 返回正确结构")
    void getWarningLogsPaged_returnsCorrectStructure() {
        when(warningLogMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        Map<String, Object> result = warningService.getWarningLogsPaged(1, 10, null, null, null);

        assertNotNull(result);
        assertTrue(result.containsKey("list"));
        assertTrue(result.containsKey("total"));
    }
}
