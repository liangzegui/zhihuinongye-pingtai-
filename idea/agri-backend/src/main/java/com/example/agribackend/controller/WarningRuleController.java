package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.entity.WarningRuleEntity;
import com.example.agribackend.mapper.WarningRuleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预警规则管理接口
 * 提供预警阈值的增删改查功能
 */
@RestController
@RequestMapping("/api/warning-rules")
@Tag(name = "预警规则管理", description = "管理环境参数预警阈值规则")
public class WarningRuleController {

    @Autowired
    private WarningRuleMapper warningRuleMapper;

    /**
     * 查询所有预警规则
     */
    @GetMapping
    @Operation(summary = "获取所有预警规则")
    public Result<List<WarningRuleEntity>> listRules() {
        List<WarningRuleEntity> rules = warningRuleMapper.selectList(null);
        return Result.success(rules);
    }

    /**
     * 新增预警规则
     */
    @PostMapping
    @Operation(summary = "新增预警规则")
    public Result<Void> createRule(@RequestBody WarningRuleEntity rule) {
        if (rule.getSensorType() == null || rule.getSensorType().isEmpty()) {
            return Result.error(400, "传感器类型不能为空");
        }
        if (rule.getMinValue() != null && rule.getMaxValue() != null
                && rule.getMinValue() > rule.getMaxValue()) {
            return Result.error(400, "最小阈值不能大于最大阈值");
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        warningRuleMapper.insert(rule);
        return Result.ok("规则创建成功");
    }

    /**
     * 更新预警规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新预警规则")
    public Result<Void> updateRule(@PathVariable Integer id, @RequestBody WarningRuleEntity rule) {
        WarningRuleEntity existing = warningRuleMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "规则不存在");
        }
        if (rule.getMinValue() != null && rule.getMaxValue() != null
                && rule.getMinValue() > rule.getMaxValue()) {
            return Result.error(400, "最小阈值不能大于最大阈值");
        }
        rule.setId(id);
        warningRuleMapper.updateById(rule);
        return Result.ok("规则更新成功");
    }

    /**
     * 删除预警规则
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除预警规则")
    public Result<Void> deleteRule(@PathVariable Integer id) {
        WarningRuleEntity existing = warningRuleMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "规则不存在");
        }
        warningRuleMapper.deleteById(id);
        return Result.ok("规则删除成功");
    }

    /**
     * 切换规则启用/禁用状态
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换规则启用状态")
    public Result<Void> toggleRule(@PathVariable Integer id) {
        WarningRuleEntity existing = warningRuleMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "规则不存在");
        }
        existing.setEnabled(existing.getEnabled() == 1 ? 0 : 1);
        warningRuleMapper.updateById(existing);
        return Result.ok(existing.getEnabled() == 1 ? "规则已启用" : "规则已禁用");
    }
}
