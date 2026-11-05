package com.florachain.payment;

public record CallbackResult(Type type, String orderNo, String message) {
    public enum Type { ACCEPTED, DUPLICATE, REJECTED, IGNORED }

    public static CallbackResult accepted(String orderNo) { return new CallbackResult(Type.ACCEPTED, orderNo, "处理成功"); }
    public static CallbackResult duplicate(String orderNo) { return new CallbackResult(Type.DUPLICATE, orderNo, "重复回调，已忽略"); }
    public static CallbackResult rejected(String msg) { return new CallbackResult(Type.REJECTED, null, msg); }
    public static CallbackResult ignored(String msg) { return new CallbackResult(Type.IGNORED, null, msg); }
}
