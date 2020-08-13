package com.ruiweishen.a2atranfer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.nononsenseapps.filepicker.FilePickerActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * GitHub: https://github.com/SmileAlfred/A2ATranfer
 * 博客：https://blog.csdn.net/liusaisaiV1/article/details/107756929
 * 推荐：https://www.imooc.com/article/296288
 */
public class A2ATransferActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int FILE_CODE = 0;
    private static final int REQUEST_CODE = 1;
    private TextView tvMsg, tv_test;
    private EditText txtIP, txtPort, txtEt;
    private Button btnSend;
    private ListView mLvWifi;
    private FrameLayout mflWifi;
    private Handler handler;
    private SocketManager socketManager;

    private final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CHANGE_NETWORK_STATE"};

    private final String TAG = A2ATransferActivity.this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a2a_transfer);
        ActivityCompat.requestPermissions(A2ATransferActivity.this, PERMISSIONS_STORAGE, REQUEST_CODE);
        initView();



        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
                        txtEt.append("\n[" + format.format(new Date()) + "]" + msg.obj.toString());
                        break;
                    case 1:
                        tvMsg.setText("本机IP：" + GetIpAddress() + " 监听端口:" + msg.obj.toString());
                        break;
                    case 2:
                        Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        };
        socketManager = new SocketManager(handler, this);
    }


    private void initView() {
        tvMsg = (TextView) findViewById(R.id.tvMsg);
        txtIP = (EditText) findViewById(R.id.txtIP);
        txtPort = (EditText) findViewById(R.id.txtPort);
        txtEt = (EditText) findViewById(R.id.et);
        tv_test = findViewById(R.id.tv_test);

        mLvWifi = findViewById(R.id.lv_wifi);
        mflWifi = findViewById(R.id.fl_wifi);

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSend:
                Intent i = new Intent(A2ATransferActivity.this, FilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                Log.i(TAG, "onClick: Environment.getExternalStorageDirectory().getPath()" + Environment.getExternalStorageDirectory().getPath());

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
            final String ipAddress = txtIP.getText().toString();
            final int port = Integer.parseInt(txtPort.getText().toString());
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
                                socketManager.SendFile(fileNames, paths, ipAddress, port);
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
                            socketManager.SendFile(fileNames, paths, ipAddress, port);
                        }
                        Message.obtain(handler, 0, "正在发送至" + ipAddress + ":" + port).sendToTarget();
                        Thread sendThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                socketManager.SendFile(fileNames, paths, ipAddress, port);
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
        //unregisterReceiver(mReceiver);
        //System.exit(0);
    }

    public String GetIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        System.out.println("静态ip设置成功！");
        int i = wifiInfo.getIpAddress();
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }

}