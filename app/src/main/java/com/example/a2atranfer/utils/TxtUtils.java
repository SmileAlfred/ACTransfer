package com.example.a2atranfer.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author: LiuSaiSai
 * @date: 2020/08/06 18:49
 * @description: 不知道 txt 编码格式时自动识别
 */
public class TxtUtils {

    public static String getTxtformat(BufferedInputStream bin) throws IOException {
        int p = (bin.read() << 8) + bin.read();
        String code = null;
        switch (p) {
            case 0xefbb:
                code = "UTF-8";
                break;
            case 0xfffe:
                code = "Unicode";
                break;
            case 0xfeff:
                code = "UTF-16BE";
                break;
            default:
                code = "GBK";
                break;
        }
        return code;
    }
    public static String getTxtformat(InputStream bin) throws IOException {
        int p = (bin.read() << 8) + bin.read();
        String code = null;
        switch (p) {
            case 0xefbb:
                code = "UTF-8";
                break;
            case 0xfffe:
                code = "Unicode";
                break;
            case 0xfeff:
                code = "UTF-16BE";
                break;
            default:
                code = "GBK";
                break;
        }
        return code;
    }
}
