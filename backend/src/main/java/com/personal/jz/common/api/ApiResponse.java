package com.personal.jz.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {
    private int code;
    private String msg;
    private T data;
    private String traceId;

    public static <T> ApiResponse<T> ok() { return ok(null); }
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }
    public static <T> ApiResponse<T> fail(int code, String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
    public static <T> ApiResponse<T> fail(ErrorCodeEnums e) {
        return fail(e.getCode(), e.getMsg());
    }
}
