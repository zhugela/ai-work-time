package com.personal.jz.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(ErrorCodeEnums e) {
        super(e.getMsg());
        this.code = e.getCode();
    }
    public BizException(ErrorCodeEnums e, String detail) {
        super(e.getMsg() + ": " + detail);
        this.code = e.getCode();
    }
    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
