package com.example.a2atranfer.beans;

import com.example.a2atranfer.utils.FormatUtils;

/**
 * @author: LiuSaiSai
 * @date: 2020/09/17 15:49
 * @description: 使用 对象 发送信息
 */
public class MsgBean {

    private static final String TAG = MsgBean.class.getSimpleName();

    public byte[] buf;
    //首位的各种命令 ORDER
    public static final int TEMP = 0,

    //握手
    ORDER_REQUEST_CONNECTION = 1,

    //请求是否接收文件
    ORDER_REQUEST_IS_RECEIVE = 2,
    //允许接收文件
    ORDER_ALLOW_SEND = 3,
    //拒绝接收文件
    ORDER_REJECT_SEND = 4,
    //谨发送消息
    ORDER_SEND_STR = 5,

    //接收成功
    ORDER_SUCESS__MSG = 6;


    //属性：命令消息
    public int msg_code;

    //接收 文件 的长度占 8 个字节；
    public long fileLength;

    //文件名（或聊天的消息）
    public String fileName;
    //传递的 文件名 的长度；（或发送消息的长度）
    public int fileNameLength;

    public MsgBean() {
    }

    /**
     * 发送结构体 时的构造器
     */
    public MsgBean(int msg_code, String fileName, long fileLength) {
        this.msg_code = msg_code;
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.fileNameLength = fileName.getBytes().length;


        byte[] intBytes = new byte[4];
        byte[] longBytes = new byte[8];
        byte[] fileNameBytes = new byte[fileNameLength];
        this.buf = new byte[12 + fileNameLength];

        //发送命令
        intBytes = FormatUtils.toLH(this.msg_code);
        System.arraycopy(intBytes, 0, buf, 0, 4);

        //发送文件名
        fileNameBytes = fileName.getBytes();
        System.arraycopy(fileNameBytes, 0, buf, 4, fileNameLength);

        //发送文件大小
        longBytes = FormatUtils.toLH(this.fileLength);
        System.arraycopy(longBytes, 0, buf, 4+fileNameLength, 8);
    }


    /**
     * 对接受的结构体数据进行解析
     *
     * @param buffer 结构体的字节流
     */
    public static MsgBean getStruct(byte[] buffer) {
        MsgBean msgBean = new MsgBean();
        byte[] intBytes = new byte[4];
        byte[] longBytes = new byte[8];
        int fileNameLength = buffer.length - 12;
        byte[] fileNameBytes = new byte[fileNameLength];

        //获取命令
        System.arraycopy(buffer, 0, intBytes, 0, 4);
        msgBean.msg_code = FormatUtils.byteArrayToInt(intBytes);

        //获取文件名
        System.arraycopy(buffer, 4, fileNameBytes, 0, fileNameLength);
        msgBean.fileName = new String(fileNameBytes);

        //获取文件大小
        System.arraycopy(buffer, 4+fileNameLength, longBytes, 0, 8);
        msgBean.fileName = new String(fileNameBytes);

        return msgBean;
    }


    /**
     * 返回要发送的数组
     */
    public byte[] getBuf() {
        return buf;
    }

    public int getMsg_code() {
        return msg_code;
    }

    public void setMsg_code(int msg_code) {
        this.msg_code = msg_code;
    }

    public int getFileNameLength() {
        return fileNameLength;
    }

    public void setFileNameLength(int fileNameLength) {
        this.fileNameLength = fileNameLength;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(int fileLength) {
        this.fileLength = fileLength;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "MsgBean{" +
                "msg_code=" + msg_code +
                ", fileNameLength=" + fileNameLength +
                ", fileLength=" + fileLength +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}