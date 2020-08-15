package com.ruiweishen.a2atranfer;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.text.BoringLayout;
import android.util.Log;
import android.widget.Toast;

import com.ruiweishen.a2atranfer.struct.PcStruct;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.DoubleToIntFunction;

/**
 * @author: LiuSaiSai
 * @date: 2020/07/31 21:35
 * @description: 1. 发送端：发送文件给接收端前，先验证接受端是否存在该文件，存在则发挥NO不接收，本地不存在发送YSE并开始文件传输；
 * 验证格式为：32位MD5码（用于验证本地数据库是否存在）$文件名$文件大小；
 * 2. 接收端，验证，通过后接收；
 * 3. 数组清空小技巧:    Arrays.fill(arr,0);或者   Arrays.fill(arrCahr,'\0');
 * 4. 另外char型数组要用num【number】形式来比较。
 * 5. 待办事项：多文件传输BUG，要么取消多文件选择功能，
 */
public class SocketManager {
    private ServerSocket ss;
    private Handler handler = null;
    private final String TAG = SocketManager.this.getClass().getSimpleName();
    private final String regEx = "/root/";
    int port = 9999;
    final HashSet<String> MD5Lists = new HashSet<String>();
    //判断接收到的信息是不是YES；0 - 默认值，1 - YES，2 - No
    private int isReceiveYes = 0;
    //private Socket receiveYesSocket;
    private Context mContext;
    private String targetIpAddress = "192.168.1.10";
    private int targetPort = 9999;
    //接收方法中的 MD5 值和文件名
    private String MD5R = "";
    private String fileNameR = "";
    private boolean isExistR = false;


    public SocketManager(Handler handler, Context context) {
        this.handler = handler;
        mContext = context;
        while (port > 9000) {
            try {
                //1. 建立连接监听窗口；
                //ss = new ServerSocket(port);
//                if (!ss.getReuseAddress()) {
//                    ss.setReuseAddress(true);
//                }
                ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(port));
                //Log.i(TAG, "server.getReuseAddress() = " + ss.getReuseAddress());
                break;
            } catch (Exception e) {
                port--;
                //Log.i(TAG, "SocketManager: e = " + e);
            }
        }
        SendMessage(1, port);
        Thread receiveFileThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ReceiveFile();
                }
            }
        });
        receiveFileThread.start();
    }

    void SendMessage(int what, Object obj) {
        if (handler != null) {
            Message.obtain(handler, what, obj).sendToTarget();
        }
    }

    /**
     * 实现发送文件给 PC 端，先发送文件信息，而后直接发送文件，不进行验证是否电脑端存在；
     * 待办事项：不能同时发送多个文件；不能短时间内发送多个文件
     *
     * @param fileName  需要传输的文件名
     * @param path      文件路径
     * @param ipAddress 目标 IP 地址
     * @param port      目标 端口
     */
    public void SendFile(ArrayList<String> fileName, ArrayList<String> path, String ipAddress, int port) {
        targetIpAddress = ipAddress;
        targetPort = port;
        OutputStream nameOPS;
        OutputStream outputData;
        try {
            //发送文件 MD5码$文件名$文件大小；
            for (int i = 0; i < fileName.size(); i++) {
                Socket name = new Socket(ipAddress, port);
                Log.i(TAG, "SendFile: Socketname =  " + name);
                String tempPath = path.get(i).replaceAll(regEx, "");
                File file = new File(tempPath);
                String MD5 = MD5FileUtils.md5(file);

                String confirmStr = MD5 + "$" + fileName.get(i) + "$" + file.length();
                Log.i(TAG, "SendFile: While : confirmStr =  " + confirmStr);

                //方式三：可行
                nameOPS = name.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(nameOPS, "GBK");
                outputStreamWriter.write(confirmStr);
                outputStreamWriter.flush();

                nameOPS = null;
                SendMessage(0, "正在发送" + fileName.get(i));

                //待办事项：验证是否是YES时可以获取 byte[3]，将其字节转换成 char，拼接后和 YES比较

                /*  //3.获取客户端发来的数据  "YSE"
                InputStream receiveStructIPS = receiveSocket.getInputStream();

                //确定 首次发送过来的文件 的编码集，解决汉字乱码问题；
                String code = TxtUtils.getTxtformat(receiveStructIPS);
                Log.i(TAG, "ReceiveFile  code  = " + code);

                //4.开始读取，获取输入信息;Error: 服务器端并没有读取到数据;解决:https://blog.csdn.net/neon_z/article/details/53707170
                //接收 发送文件请求信息
                byte[] bytes = new byte[128];
                receiveStructIPS.read(bytes);*/

                //方法二接收 PC 端发送的结构体，并验证是否接收正确
                InputStream inputStream = name.getInputStream();

                byte[] bytes = new byte[48];
                inputStream.read(bytes);
                /**
                 * public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
                 * 　　Object src : 原数组
                 *    int srcPos : 从元数据的起始位置开始
                 * 　　Object dest : 目标数组
                 * 　　int destPos : 目标数组的开始起始位置
                 * 　　int length  : 要copy的数组的长度
                 */
                PcStruct pcStructR = PcStruct.getPcStructInstance(bytes);
                Log.i(TAG, "SendFile: 接收结构体 =  pcStructR： " + pcStructR.toString());


                //TODO：发送文件内容
                FileInputStream fileInput = new FileInputStream(tempPath);
                //判断是否读到文件末尾
                int size = 0;
                long receivedSize = 0L;
                byte[] buffer = new byte[1024];

                while (true) {
                    size = fileInput.read(buffer);

                    if (size == -1) {
                        break;
                    }
                    outputData = name.getOutputStream();
                    outputData.write(buffer, 0, size);
                    outputData.flush();

                    receivedSize += size;
                    if (!(receivedSize < file.length())) {
                        Log.i(TAG, "SendFile: While : receivedSize =  " + receivedSize + "  file.length()  =  " + file.length());
                        break;
                    }
                }
                outputData = null;
                SendMessage(0, fileName.get(i) + "  发送完成");
            }
            SendMessage(0, "所有文件发送完成");
        } catch (Exception e) {
            Log.i(TAG, "发送错误   +   " + e.getMessage());
        }
    }

    void ReceiveFile() {
        //接收文件吗？用于进入正式接收文件逻辑
        try {
            //2. 连接客户端对象
            //阻塞式方法，只有客户端连接了才会继续往下运行
            while (true) {
                //Log.i(TAG, "ReceiveFile  ss.accept()  之前   " );
                Socket receiveSocket = ss.accept();
                Log.i(TAG, "ReceiveFile  Socket receiveSocket =   " + receiveSocket);
                //3.获取客户端发来的数据
                InputStream receiveIPS = receiveSocket.getInputStream();

                //确定 首次发送过来的文件 的编码集，解决汉字乱码问题；
                String code = TxtUtils.getTxtformat(receiveIPS);
                Log.i(TAG, "ReceiveFile  code  = " + code);

                //4.开始读取，获取输入信息;Error: 服务器端并没有读取到数据;解决:https://blog.csdn.net/neon_z/article/details/53707170
                //接收 发送文件请求信息
                byte[] bytes = new byte[128];
                receiveIPS.read(bytes);
                /**
                 * 解析获取到的文件信息；并分离出 MD5、Name 以及文件大小；
                 */
                String bytesName = new String(bytes, code).trim();

                MD5R = bytesName.substring(0, bytesName.indexOf("$"));
                isExistMD5(MD5R);
                if(isExistR){
                    Log.i(TAG, "ReceiveFile: 文件已存在");
                    break;
                }
                Log.i(TAG, "ReceiveFile  receiveSocket  =  " + "  MD5Lists.size()  = " + MD5Lists.size());
                //去掉MD5
                bytesName = bytesName.replaceAll(MD5R, "");
                //返回一个新字符串
                bytesName = bytesName.substring(1, bytesName.length());
                fileNameR = bytesName.substring(0, bytesName.indexOf("$"));

                String fileSize = bytesName.replaceAll(fileNameR, "");
                fileSize = fileSize.substring(1, fileSize.length());
                receiveIPS = null;

                //TODO：3.  返回消息给发送端YES；PC端不是 C++，而是MFC框架，所以……
                // 不要使用DataInputStream和DataOutputStream。用InputStream和OutputStream的方式发送“字节流”
                //DataInputStream和DataOutputStream需要配合使用才能得到正确的数据，MFC里面是没有这两种对象,它不知道如何解析这些数据
                //c语言中的char 与java中的byte相互对应,

                //待办事项：删除。（向 PC 端发送 "YES"；）String → byte[]
                /*if(!"".equals(MD5R)) {
                    isExistR = isExistMD5(MD5R);
                    //不存在发送YES接收
                    String str  = "YES ";
                    //String str = isExistR ? " YES " : " YES ";
                    Log.i(TAG, "sendYes: str(" + str + ") 的字节数：  " + getStrLength(str));

                    if (!isExistR) {
                        SendMessage(0, "准备接收:" + fileNameR);
                        sureToReceiveFile = true;
                    } else {
                        SendMessage(0, fileNameR + " 已存在。");
                        sureToReceiveFile = false;
                    }

                    Socket pwSocket = new Socket(targetIpAddress, targetPort);
                    Log.i(TAG, "sendYes: targetIpAddress = " + targetIpAddress + " targetPort = " + targetPort);

                    //方式六：
                    OutputStream pw = pwSocket.getOutputStream();
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(pw, "GBK");
                    outputStreamWriter.write(str);
                    outputStreamWriter.flush();
                    Log.i(TAG, "sendYes: Writer 结束");
                    outputStreamWriter = null;
                    pw = null;
                    pwSocket = null;
                }*/

                //待办事项：向PC端发送结构体；→ 测试 PC 端接收情况
                OutputStream PCStructOPS = receiveSocket.getOutputStream();
                PCStructOPS.write(new PcStruct(2, 3, 4.1, 4.2, 4.3, 4.4).getBuf());
                PCStructOPS.flush();


                /**
                 * 5. 写入文件；确定接收后，正式接收文件
                 * 待办事项，接收文件有瑕疵，
                 */
                while (true) {
                    SendMessage(0, "正在接收:" + fileNameR);
                    //接受文件内容
                    Log.i(TAG, "ReceiveFile file Socket data  ==  " + receiveSocket);
                    //3. 获取客户端发送过来的数据
                    InputStream receiveDataIPS = receiveSocket.getInputStream();

                    //得到SD卡根目录
                    File sdCardDir = Environment.getExternalStorageDirectory();
                    //打开data目录，如不存在则生成
                    File buildDir = new File(sdCardDir, "/A2ATranfer/data");
                    if (!buildDir.exists()) buildDir.mkdirs();

                    String savePath = buildDir.getPath() + "/" + fileNameR;
                    savePath = savePath.replaceAll(regEx, "");


                    //装载文件名的数组
                    byte[] buffer = new byte[1024];
                    int size = -1;

                    //方式二
                    long received = 0L;
                    FileOutputStream fileOut = new FileOutputStream(new File(savePath), false);
                    Log.i(TAG, "ReceiveFile file 开始循环写入  ==  ");
                    while (true) {
                        Log.i(TAG, "ReceiveFile file size  =  " + size);
                        size = receiveDataIPS.read(buffer);
                        if (size < 0) {
                            break;
                        }
                        Log.i(TAG, "ReceiveFile file size  ==  " + size);
                        //从网络读取，写入到文件
                        fileOut.write(buffer, 0, size);
                        fileOut.flush();
                        received += size;
                        if (!(received < Long.parseLong(fileSize))) {
                            Log.i(TAG, "SendFile: While : receivedSize =  " + received + "  file.length()  =  " + fileSize);
                            break;
                        }
                    }
                    System.out.println("文件接收完成！！！次数=" + size);
                    fileOut = null;


                    //重置接收文件判断，因为发送的第一次是数据信息，第二次才是数据内容。不接受第一次就没有文件名信息
                    //告诉发送端我已经接收完毕
                    SendMessage(0, fileNameR + "接收完成\n" + "保存在：" + savePath);
                    MD5R = "";
                    break;
                }
            }
        } catch (Exception e) {
            SendMessage(0, "接收错误:\n" + e.getMessage());
        }
    }


    /**
     * 2. 验证 MD5；是否存在 MD5，存在返回 true，返回 false 表示不存在，发送YES
     *
     * @param md5 待检测的 值
     * @return
     */
    private void isExistMD5(String md5) {
        if (MD5Lists.contains(md5)) {
            isExistR = true;
        } else {
            MD5Lists.add(md5);
            isExistR = false;
        }
    }






    /**
     * int转为低字节在前，高字节在后的byte数组 VC
     *
     * @param n
     * @return byte[]
     */
    private byte[] toLH(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    /**
     * 将String转为byte数组
     */
    public static byte[] stringToBytes(String s, int length) {
        while (s.getBytes().length < length) {
            s += " ";
        }
        return s.getBytes();
    }

    /**
     * 获取 字符串 所占字节长度
     *
     * @param s
     * @return
     */
    public static int getStrLength(String s) {
        int length = 0;
        for (int i = 0; i < s.length(); i++) {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <= 255) {
                length++;
            } else {
                length += 2;
            }
        }
        return length;
    }

    /**
     * 好使！字节数组到int的转换.
     */
    public static int byteArrayToInt(byte[] b) {
        int s = 0;
        // 最低位
        int s0 = b[0] & 0xff;
        int s1 = b[1] & 0xff;
        int s2 = b[2] & 0xff;
        int s3 = b[3] & 0xff;
        s3 <<= 24;
        s2 <<= 16;
        s1 <<= 8;
        s = s0 | s1 | s2 | s3;
        return s;
    }

    /**
     * 好使！字节数组到double的转换.
     */
    public static double byteArrayTodouble(byte[] b) {
        long m;
        m = b[0];
        m &= 0xff;
        m |= ((long) b[1] << 8);
        m &= 0xffff;
        m |= ((long) b[2] << 16);
        m &= 0xffffff;
        m |= ((long) b[3] << 24);
        m &= 0xffffffffl;
        m |= ((long) b[4] << 32);
        m &= 0xffffffffffl;
        m |= ((long) b[5] << 40);
        m &= 0xffffffffffffl;
        m |= ((long) b[6] << 48);
        m &= 0xffffffffffffffl;
        m |= ((long) b[7] << 56);
        return Double.longBitsToDouble(m);
    }
}