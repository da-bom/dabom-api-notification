package com.project.common.exception;

import com.project.common.exception.code.BaseErrorCode;

public class ApplicationException extends BaseException {

    public ApplicationException(BaseErrorCode code) {
        super(code);
    }

    public ApplicationException(BaseErrorCode code, String message) {
        super(code, message);
    }
}
