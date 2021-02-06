package com.example.a2atranfer.struct;

/**
 * 通过新协议 是否 是文件信息（0 - 1 ），文件信息长度（int），文件内容长度（long）
 */
public class AJStruct {

    public  int isFileInfo;//0 - 是文件信息；1 - 文件内容
    public  int infoLength;//文件信息长度；
    public  long fileContent;//文件内容长度

    public AJStruct() {
    }

    public AJStruct(int isFileInfo, int infoLength, long fileContent) {
        this.isFileInfo = isFileInfo;
        this.infoLength = infoLength;
        this.fileContent = fileContent;
    }

    public int getIsFileInfo() {
        return isFileInfo;
    }

    public void setIsFileInfo(int isFileInfo) {
        this.isFileInfo = isFileInfo;
    }

    public int getInfoLength() {
        return infoLength;
    }

    public void setInfoLength(int infoLength) {
        this.infoLength = infoLength;
    }

    public long getFileContent() {
        return fileContent;
    }

    public void setFileContent(long fileContent) {
        this.fileContent = fileContent;
    }
}
