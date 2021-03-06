package leavesc.hello.library.http.exception;

import leavesc.hello.library.http.config.HttpCode;
import leavesc.hello.library.http.exception.base.BaseException;

/**
 * 作者：leavesC
 * 时间：2018/10/25 21:46
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException() {
        super(HttpCode.CODE_PARAMETER_INVALID, "404错误");
    }

}