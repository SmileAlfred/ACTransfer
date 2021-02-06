package com.example.a2atranfer.utils;

/**
 * @author: LiuSaiSai
 * @date: 2020/08/08 21:06
 * @description: java 字符穿自动补齐
 */
public class StrFormatUtil {

    /**
     * 字符串的右对齐输出
     *
     * @param c      填充字符
     * @param l      填充后字符串的总长度
     * @param string 要格式化的字符串
     */
    public static String flushRight(char c, long l, String string) {
        String str = "";
        String temp = "";
        if (string.length() > l)
            str = string;
        else
            for (int i = 0; i < l - string.length(); i++)
                temp = temp + c;
        str = temp + string;
        return str;
    }

    /**
     * 字符串的左对齐输出
     *
     * @param c      填充字符
     * @param l      填充后字符串的总长度
     * @param string 要格式化的字符串
     */
    public static String flushLeft(char c, long l, String string) {
        String str = "";
        String temp = "";
        if (string.length() > l)
            str = string;
        else
            for (int i = 0; i < l - string.length(); i++)
                temp = temp + c;
        str = string + temp;
        return str;
    }

}
