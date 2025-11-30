package com.freedom.chain.enumst;

/**
 * @Description:
 * @author: freedom
 * @Date: 2021/7/8 15:48
 */
public enum ResultCodeEnum implements ResultCode {
    /*
    *错误代码类型
    * */
    SUCCESS(200,"成功"),
    FAILED(403,"失败"),
    ERROR(500,"异常"),
    //执行方法时遇到错误
    UNKNO(3000,"未知异常请联系管理员!");
    private final int code;
    private final String msg;

    ResultCodeEnum (int code,String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode () {
        return code;
    }

    @Override
    public String getMsg () {
        return msg;
    }
    /**
     * 根据编码查询枚举。
     *
     * @param code 编码。
     * @return 枚举。
     */
    public static ResultCodeEnum getByCode(int code) {
        for (ResultCodeEnum value : ResultCodeEnum.values()) {
            if (code==value.getCode()) {
                return value;
            }
        }
        return UNKNO;
    }

    /**
     * 枚举是否包含此code
     * @param code 枚举code
     * @return 结果
     */
    public static Boolean contains(int code){
        for (ResultCodeEnum value : ResultCodeEnum.values()) {
            if (code == value.getCode()) {
                return true;
            }
        }
        return  false;
    }

    @Override
    public String toString () {
        return "ResultCodeEnum{" + "code='" + code + '\'' + ", msg='" + msg + '\'' + '}';
    }

}