package com.ruiweishen.a2atranfer;

/**
 * @author: LiuSaiSai
 * @date: 2020/08/08 10:27
 * @description: 用于发送结构体 → 发送 YES/NO，是否接收文件流
 */
public class YNConfirm {
    private char[] head;
    private int size;

    public YNConfirm(char[] head, int size) {
        this.head = head;
        this.size = size;
    }

    public char[] getHead() {
        return this.head;
    }

    public int getSize() {
        return this.size;
    }

    public void setHead(char[] head) {
        this.head = head;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
