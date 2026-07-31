package com.example.commerce.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结构
 *
 * 注：保留无参构造器供 Jackson 反序列化（测试/内部调用用到），
 * 对外仍用静态工厂方法构造。
 */
@Data
@NoArgsConstructor
public class Response<T> {

    private int code;
    private String message;
    private T data;

    private Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> success() {
        return new Response<>(0, "OK", null);
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(0, "OK", data);
    }

    public static <T> Response<T> fail(String message) {
        return new Response<>(500, message, null);
    }

    public static <T> Response<T> fail(int code, String message) {
        return new Response<>(code, message, null);
    }
}
