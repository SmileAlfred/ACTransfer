package com.example.a2atranfer.socket;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.a2atranfer.activity.A2ATransferActivity;
import com.example.a2atranfer.utils.Logger;
import com.example.a2atranfer.utils.MD5FileUtils;
import com.example.a2atranfer.utils.TxtUtils;
import com.example.a2atranfer.utils.WifiUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author: LiuSaiSai
 * @date: 2020/07/31 21:35
 * @description: 1. 发送端：发送文件给接收端前，先验证接受端是否存在该文件，存在则发挥NO不接收，本地不存在发送YSE并开始文件传输；
 * 验证格式为：32位MD5码（用于验证本地数据库是否存在）$文件名$文件大小；
 * 2. 接收端，验证，通过后接收；
 * 3. 数组清空小技巧:    Arrays.fill(arr,0);或者   Arrays.fill(arrCahr,'\0');
 * 4. 另外char型数组要用num【number】形式来比较。
 * 5. 待办事项：多文件传输BUG，要么取消多文件选择功能，
 * 传说中的粘包
 */
public class SocketManager4Java {
    private static final int REFUSE_TRANSFER = 1;//文件已存在
    private final String TAG = SocketManager4Java.this.getClass().getSimpleName(),
            SSID = "AutoManVSXiaoGuaiGuai", PASW = "123456789";

    private String targetIpAddress, regEx = "/root/";
    private int targetPort = 9996;

    private Handler handler = null;
    private A2ATransferActivity mContext;
    private String MD5R = "", fileNameR = "";
    private int connectSuccessful;
    private Socket mSocket = null;
    private OutputStream sendOPS;
    private final String RECEIVE = "receive";//表示允许接受，


    public SocketManager4Java(Handler handler, A2ATransferActivity context) {
        this.handler = handler;
        mContext = context;
        connectWifiSocket();
    }

    void showMsg(int what, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }


    private void connectWifiSocket() {
        showMsg(1, targetPort);
        targetIpAddress = mContext.getRemoteIpAddress();
        targetPort = mContext.getPort();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (connectSuccessful != 1) {
                    try {
                        if (mSocket == null) mSocket = new Socket(targetIpAddress, targetPort);
                        if (sendOPS == null) sendOPS = mSocket.getOutputStream();

                        connectSuccessful = 1;
                        ReceiveFile();
                    } catch (final IOException e) {
                        try {
                            if (!SSID.equals(WifiUtil.getWIFIName((Activity) mContext.getApplicationContext()))) {
                                WifiUtil mWifiUtil = new WifiUtil((Activity)mContext.getApplicationContext());
                                mWifiUtil.addNetWork(SSID, PASW, 3);
                            }
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        mSocket = null;
                    }
                }
            }
        }).start();
    }

    /**
     * 实现发送文件给 PC 端，先发送文件信息，而后直接发送文件，不进行验证是否电脑端存在；
     * 待办事项：不能同时发送多个文件；不能短时间内发送多个文件
     *
     * @param fileName 需要传输的文件名
     * @param path     文件路径
     */
    public void SendFile(ArrayList<String> fileName, ArrayList<String> path) {
        while (connectSuccessful != 1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        OutputStream nameOPS;
        BufferedOutputStream outputData;

        try {
            //发送文件 MD5码$文件名$文件大小；
            for (int i = 0; i < fileName.size(); i++) {
                Logger.i(TAG, "SendFile: Socketname =  " + mSocket);


                String tempPath = path.get(i).replaceAll(regEx, "");
                File file = new File(tempPath);
                String MD5 = MD5FileUtils.md5(file);

                String confirmStr = MD5 + "\t" + fileName.get(i) + "\t" + file.length();

                //方式三：可行
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(sendOPS, "GBK");

                outputStreamWriter.write(confirmStr);
                outputStreamWriter.flush();

                outputStreamWriter = null;
                showMsg(0, "正在发送" + fileName.get(i));

                //确定 首次发送过来的文件 的编码集，解决汉字乱码问题；
                //String code = TxtUtils.getTxtformat(receiveIPS);
                //4.开始读取，获取输入信息;Error: 服务器端并没有读取到数据;解决:https://bLogger.csdn.net/neon_z/article/details/53707170
                //接收 发送文件请求信息
                // byte[] bytes = new byte[16];
                // receiveIPS.read(bytes);
                /**
                 * 解析获取到的文件信息；并分离出 MD5、Name 以及文件大小；
                 */
                //String bytesName = new String(bytes).trim();
                //Logger.i(TAG, "接收到 bytesName = " + bytesName);
                /*if (!RECEIVE.equals(bytesName)) {
                    showMsg(0, "对方拒绝接收！");
                    continue;
                }*/

                //TODO：发送文件内容
                BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(tempPath));
                //判断是否读到文件末尾
                int size = 0;
                long receivedSize = 0L;
                byte[] buffer = new byte[1024 * 1024 * 100];//100MB
                outputData = new BufferedOutputStream(sendOPS);

                while (true) {
                    size = fileInput.read(buffer);
                    Logger.i(TAG, "SendFile: size = " + size);

                    if (size == -1) {
                        break;
                    }
                    outputData.write(buffer, 0, size);
                    outputData.flush();

                    receivedSize += size;
                    if (receivedSize == file.length()) {
                        break;
                    }
                }
                outputData = null;
                showMsg(0, fileName.get(i) + "  发送完成");
            }
            showMsg(0, "所有文件发送完成");
        } catch (Exception e) {
            Logger.i(TAG, "发送错误   +   " + e.getMessage());
        }
    }

    private InputStream receiveIPS;

    void ReceiveFile() {
        try {
            //3.获取客户端发来的数据
            receiveIPS = mSocket.getInputStream();
            while (true) {
                //确定 首次发送过来的文件 的编码集，解决汉字乱码问题；
                String code = TxtUtils.getTxtformat(receiveIPS);
                //4.开始读取，获取输入信息;Error: 服务器端并没有读取到数据;解决:https://bLogger.csdn.net/neon_z/article/details/53707170
                //接收 发送文件请求信息
                byte[] bytes = new byte[128];
                receiveIPS.read(bytes);
                /**
                 * 解析获取到的文件信息；并分离出 MD5、Name 以及文件大小；
                 */
                String bytesName = new String(bytes, code).trim();
                Logger.i(TAG, "ReceiveFile: 接收到文件信息：" + bytesName);
                String[] infos = bytesName.split("\t");
                if (infos.length != 3) continue;

                MD5R = infos[0];
                fileNameR = infos[1];
                String receivedFileSize = infos[2];
                //receiveIPS = null;

                //3. 获取客户端发送过来的数据
                BufferedInputStream receiveDataIPS = new BufferedInputStream(receiveIPS);

                /**
                 * 5. 写入文件；确定接收后，正式接收文件
                 * 待办事项，接收文件有瑕疵，
                 */
                while (true) {
                    //得到SD卡根目录
                    File sdCardDir = Environment.getExternalStorageDirectory();
                    //打开data目录，如不存在则生成
                    File buildDir = new File(sdCardDir, "/A2ATranfer/data");
                    if (!buildDir.exists()) buildDir.mkdirs();

                    String savePath = buildDir.getPath() + "/" + fileNameR;
                    savePath = savePath.replaceAll(regEx, "");


                    //装载文件名的数组
                    int singleFileSize = Integer.parseInt(receivedFileSize);
                    byte[] buffer = new byte[singleFileSize < 1024 * 1024 * 100 ? singleFileSize : 1024 * 1024 * 100];//100MB缓存
                    int size = -1;

                    //方式二
                    long received = 0L;

                    File receivedFile = new File(savePath);
                   /* if (receivedFile.exists()) {
                        Logger.i(TAG, "ReceiveFile: 文件已存在！");
                        break;
                    }*/
                    showMsg(0, "正在接收:" + infos[1] + " 文件大小为：" + infos[2]);
                    Log.i(TAG, "ReceiveFile: " + "正在接收:" + infos[1] + " 文件大小为：" + infos[2]);
                    FileOutputStream fileOut = new FileOutputStream(receivedFile, false);
                    while (true) {
                        size = receiveDataIPS.read(buffer);
                        //从网络读取，写入到文件


                        if (received + size > Long.parseLong(receivedFileSize)) {
                            size = (int) (Long.parseLong(receivedFileSize) - received);//防止粘包
                        }

                        fileOut.write(buffer, 0, size);
                        fileOut.flush();
                        received += size;
                        Logger.i(TAG, "received = " + received + " ; size = " + size);
                        if (Long.parseLong(receivedFileSize) <= received) {
                            break;
                        }
                        if (size < buffer.length) {
                            Thread.sleep(500);
                        }
                    }
                    fileOut = null;

                    //重置接收文件判断，因为发送的第一次是数据信息，第二次才是数据内容。不接受第一次就没有文件名信息
                    //告诉发送端我已经接收完毕
                    showMsg(0, fileNameR + "接收完成\n" + "保存在：" + savePath);
                    MD5R = "";
                    break;
                }
            }
        } catch (Exception e) {
            showMsg(0, "接收错误:\n" + e.getMessage());
        }
    }
}