package com.example.a2atranfer.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.a2atranfer.R;
import com.example.a2atranfer.client.TCPClient;
import com.example.a2atranfer.observer.mFileListener;
import com.example.a2atranfer.socket.SocketManager4Java;
import com.example.a2atranfer.utils.Logger;
import com.example.a2atranfer.utils.MyUtils;
import com.example.a2atranfer.utils.WifiUtil;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.Struct;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HandshakeCompletedListener;

/**
 * GitHub: https://github.com/SmileAlfred/A2ATranfer
 * 博客：https://blog.csdn.net/liusaisaiV1/article/details/107756929
 * 推荐：https://www.imooc.com/article/296288
 */
public class A2ATransferActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int FILE_CODE = 0, REQUEST_CODE = 1;
    private TextView tv_local_ip, tv_log;
    private EditText et_target_ip, et_target_port, tv_trans_msg, et_content;
    private Button btnSend, btn_send_content;
    private Handler handler;

    //private SocketManager4Java socketManager4Java;

    private final String TAG = A2ATransferActivity.this.getClass().getSimpleName(),
            regEx = "/root/",
            maohao = ":",
            SSID = "SmileAlfred",
            PASW = "123456789",
            RemoteIPAddress = "192.168.1.10";
    public static final int Port = 9999,
            TRANSfERMSG = 0,
            LOCALIPMSG = 1,
            SOMEMSG = 2,
            CLIENTGETMSG = 3,
            SERVERGETMSG = 4,
            LOG = 5;

    private boolean connectSuccessful = false;

    public static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera";
    private String date, connectedSSID = "";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"),
            timeFormat = new SimpleDateFormat("HH:mm:ss");
    private boolean wifiIsOpen = false;

    private mFileListener fileListener = new mFileListener(FILE_PATH);
    //1. 获取通道
    private SocketChannel sChannel;
    private Selector selector;
    private WifiManager mWifiManager;
    private WifiUtil mWifiUtil;
    private TCPClient mTcpClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a2a_transfer);
        String[] PERMISSIONS_STORAGE = getResources().getStringArray(R.array.PERMISSIONS);
        ActivityCompat.requestPermissions(A2ATransferActivity.this, PERMISSIONS_STORAGE, REQUEST_CODE);
        initView();
        date = dateFormat.format(new Date());

        mWifiManager = (WifiManager) A2ATransferActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiUtil = new WifiUtil(A2ATransferActivity.this.getApplication());


        //监听 WiFi 连接状态
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mWifiBroadcastReceiver, filter);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case TRANSfERMSG:
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                        tv_trans_msg.append("\n[" + format.format(new Date()) + "]" + msg.obj.toString());
                        break;
                    case LOCALIPMSG:
                        tv_local_ip.setText("本机IP：" + GetIpAddress() + " 监听端口:" + msg.obj.toString());
                        break;
                    case SOMEMSG:
                        Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        break;
                    case CLIENTGETMSG:
                        String data = (String) msg.obj;
                        //TODO:对【客户端】接收到的【来自服务器】的消息
                        switch (data) {
                            case "Hello":
                                MyUtils.playSound(A2ATransferActivity.this, 0);
                                connectSuccessful = true;
                                tv_local_ip.setText("正在进行通信...");
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        mTcpClient = new TCPClient(this, handler, "client");

        //if (socketManager4Java == null) socketManager4Java = new SocketManager4Java(handler, this);
        fileListener.startWatching();
    }

    /**
     * 创建套接字进行通信
     */
    private void connectWifiSocket() {
        String log = "\t开始 connectWifiSocket()：\t" + WifiUtil.getWIFIName(A2ATransferActivity.this);
        writePadLog(log);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String log = "\t开始 connectWifiSocket() - 线程中：\t" + WifiUtil.getWIFIName(A2ATransferActivity.this);
                writePadLog(log);
                mTcpClient.requestConnectTcp(RemoteIPAddress);
                while (true) {
                    if(connectSuccessful)break;
                    mTcpClient.sendMsg("Hello");
                    Log.i(TAG, "run: 发送了 Hello");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                }
                    /*try {
                        log = "\t开始 connectWifiSocket() - 线程-循环-进入 try 中：\t" + WifiUtil.getWIFIName(A2ATransferActivity.this);
                        writePadLog(log);

                        //TODO:发送握手命令
                        while (null == sChannel) {
                            try {
                                mTcpClient.sendMsg("1_你好吗？");

                            } catch (Exception e) {
                                Log.i(TAG, "报错： " + e.getMessage());
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException ex) {
                                }
                            }
                        }

                        //开启接收方法
                        //receiveOrder();
                    } catch (Exception e) {
                        log = "\t捕获 connectWifiSocket()异常：\t" + e.getMessage() + "\t" + WifiUtil.getWIFIName(A2ATransferActivity.this);
                        writePadLog(log);
                        try {
                            connectedSSID = WifiUtil.getWIFIName(A2ATransferActivity.this);

                            //WiFi自动重连
                            if (!SSID.equals(connectedSSID)) {
                                WifiConfiguration tempConfig = isExist(connectedSSID);
                                if (tempConfig != null) {
                                    //则清除旧有配置
                                    mWifiManager.removeNetwork(tempConfig.networkId);
                                }
                                mWifiUtil.addNetWork(SSID, PASW, 3);
                                Thread.sleep(1000);
                            }
                            log = "\t捕获 connectWifiSocket()异常-我是努力连接过的！：\t" + e.getMessage() + "\t" + WifiUtil.getWIFIName(A2ATransferActivity.this);
                            writePadLog(log);
                        } catch (Exception ex) {
                            log = "\t捕获 connectWifiSocket()异常-努力连接-报错了！：\t" + e.getMessage() + "\t" + WifiUtil.getWIFIName(A2ATransferActivity.this);
                            writePadLog(log);
                        }
                        log = "\t捕获 connectWifiSocket()异常末尾：\t" + e.getMessage() + "\t" + WifiUtil.getWIFIName(A2ATransferActivity.this);
                        writePadLog(log);
                    }*/
            }
        }).start();
    }

    private void initView() {
        tv_log = findViewById(R.id.tv_log);
        tv_local_ip = findViewById(R.id.tv_local_ip);
        et_target_ip = findViewById(R.id.et_target_ip);
        et_target_port = findViewById(R.id.et_target_port);
        tv_trans_msg = findViewById(R.id.tv_trans_msg);

        et_content = findViewById(R.id.et_content);
        btn_send_content = findViewById(R.id.btn_send_content);

        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);
        btn_send_content.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send_content:
                if (sChannel == null || !sChannel.isConnected()) {
                    Toast.makeText(this, "sChannel == null", Toast.LENGTH_SHORT).show();
                    return;
                }
                String content = et_content.getText().toString();

                //3. 分配指定大小的缓冲区
                ByteBuffer buf = ByteBuffer.allocate(1024);
                try {
                    //4. 发送数据给服务端

                    while (!"exist;;".equals(content)) {
                        buf.put((new Date().toString() + "\n客户端：" + content).getBytes());
                        buf.flip();
                        Log.i(TAG, "sChannel = " + sChannel + " ; buf = " + buf);
                        sChannel.write(buf);
                        buf.clear();
                    }

                    //5. 关闭通道
                    //sChannel.close();
                } catch (IOException e) {
                }
                break;
            case R.id.btnSend:
                Intent i = new Intent(A2ATransferActivity.this, FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

                String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, path);

                Log.i(TAG, "onClick: Environment.getExternalStorageDirectory().getPath()" +
                        Environment.getExternalStorageDirectory().getPath());
                startActivityForResult(i, FILE_CODE);
                break;
            default:
                break;
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: + resultCode" + resultCode);
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            final String ipAddress = et_target_ip.getText().toString();
            final int port = Integer.parseInt(et_target_port.getText().toString());
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();
                    final ArrayList<String> fileNames = new ArrayList<>();
                    final ArrayList<String> paths = new ArrayList<>();
                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            paths.add(uri.getPath());
                            fileNames.add(uri.getLastPathSegment());
                        }
                        Message.obtain(handler, 0, "正在发送至" + ipAddress + ":" + port).sendToTarget();
                        Thread sendThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "run:fileNames.size() =  " + fileNames.size());
                                //TODO:发送文件
                                //socketManager4Java.SendFile(fileNames, paths);
                            }
                        });
                        sendThread.start();
                    }
                } else {
                    final ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);
                    final ArrayList<String> fileNames = new ArrayList<>();
                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            paths.add(uri.getPath());
                            fileNames.add(uri.getLastPathSegment());
                            //TODO:发送文件
                            //socketManager4Java.SendFile(fileNames, paths);
                        }
                        Message.obtain(handler, 0, "正在发送至" + ipAddress + ":" + port).sendToTarget();
                        Thread sendThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //TODO:发送文件
                                //socketManager4Java.SendFile(fileNames, paths);
                            }
                        });
                        sendThread.start();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mWifiBroadcastReceiver);
        //TODO:断开客户端连接；
    }


    public String GetIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.i(TAG, "静态ip设置成功！");
        int i = wifiInfo.getIpAddress();
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }

    public String getRemoteIpAddress() {
        return RemoteIPAddress;
    }

    public int getPort() {
        return Port;
    }


    /**
     * 监听 WiFi 状态广播
     */
    private BroadcastReceiver mWifiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            connectedSSID = WifiUtil.getWIFIName(A2ATransferActivity.this);
            String log = "\t捕获 广播前：\t" + connectedSSID + " ; action = " + action;
            writePadLog(log);
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                connectedSSID = WifiUtil.getWIFIName(A2ATransferActivity.this);
                log = "\t捕获 广播中：\t" + connectedSSID + " ; action = " + action;
                writePadLog(log);

                ConnectivityManager localConnectivityManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo localNetworkInfo = (localConnectivityManager == null ? null
                        : localConnectivityManager.getActiveNetworkInfo());
                if (localNetworkInfo != null) {
                    tv_local_ip.setText("检测到 WiFi 已经开启");
                    wifiIsOpen = true;

                    //TODO:断开 客户端 连接
                    if (!localNetworkInfo.isConnected()) {
                        tv_local_ip.setText("WIFI 未连接");
                    }
                } else {
                    tv_local_ip.setText("WiFi 未开启！");
                    wifiIsOpen = false;
                    wifiIsOpen = true;
                }
                //TODO:做网络通信的测试
                connectWifiSocket();
            }
        }
    };

    /**
     * 把 软件 运行过程中捕获到的 异常写道本地；
     *
     * @param log catch 住的异常写信息
     */
    public void writePadLog(String log) {
        File sdCardDir = Environment.getExternalStorageDirectory();
        File buildDir = new File(sdCardDir, "/A2ATransfer/data/" + "Log");
        if (!buildDir.exists()) buildDir.mkdirs();
        final String infoPath = (buildDir.getPath() + "/log-" + date + ".txt").replaceAll(maohao, "-").replaceAll(regEx, "");
        final String logMsg = "[" + timeFormat.format(new Date()) + "]" + "\t" + log + "\r\n";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedWriter msgWriter = new BufferedWriter(new FileWriter(new File(infoPath), true));
                    msgWriter.write(logMsg);
                    msgWriter.close();
                } catch (IOException e) {
                }
            }
        }).start();
    }

    /**
     * 判断是否存在此 WiFi 信息
     *
     * @param ssid WiFi 名称
     * @return 存在返回 true
     */
    private WifiConfiguration isExist(String ssid) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }
        return null;
    }
}