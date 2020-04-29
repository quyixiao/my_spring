package com.design.pattern.proxy.statics;

public class DynamicDataSourceEntry {

    // 设置默认的数据源
    public final static String DEFATUL_SOURCE = null;

    private final static ThreadLocal<String> local = new ThreadLocal<String>();


    private DynamicDataSourceEntry() {

    }

    // 清空当前数据
    public static void clear() {
        local.remove();
    }

    // 获取当前正在使用的数据源的名称
    public static String get() {
        return local.get();
    }

    // 还原当前切换的的数据源
    public static void restore() {
        local.set(DEFATUL_SOURCE);
    }

    // 设置已经知道名字的source
    public static void set(String source) {
        local.set(source);
    }

    // 根据年份动态的设置数据
    public static void set(int year) {
        local.set("DB_" + year);
    }
}
