package com.example.a2atranfer.beans;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FileInfo {

    public String md5;
    public String fileName;
    public long fileSize;
    public ArrayList<byte[]> fileContent;

    public FileInfo(String md5, String fileName, long fileSize) {
        this.md5 = md5;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public FileInfo() {
    }

    private FileInfo getFileInfo(InputStream isInfo) {
        FileInfo file = new FileInfo();
        byte[] bufferMsg = new byte[128];
        int len = 0;
        try {
            len = isInfo.read(bufferMsg);
            String receivedMsg = new String(bufferMsg, "GBK").trim();
            String[] infos = receivedMsg.split("\t");
            file.setMd5(infos[0]);
            file.setFileName(infos[1]);
            file.setFileSize(Long.parseLong(infos[2]));
        } catch (IOException e) {
            System.out.println("报错：" + e.getMessage());
        }
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public ArrayList<byte[]> getFileContent() {
        return fileContent;
    }

    public void setFileContent(ArrayList<byte[]> fileContent) {
        this.fileContent = fileContent;
    }
}
