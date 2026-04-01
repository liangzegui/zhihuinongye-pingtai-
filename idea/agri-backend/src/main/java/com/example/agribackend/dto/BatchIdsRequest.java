package com.example.agribackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量操作请求体：接收前端传递的ID数组
 */
@Data
public class BatchIdsRequest {
    private List<Integer> ids;
}
