package com.example.agribackend.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code = 200;
    private String msg = "success";
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}