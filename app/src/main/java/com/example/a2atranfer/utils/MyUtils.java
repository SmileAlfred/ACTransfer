package com.example.a2atranfer.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.example.a2atranfer.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author LiuSaiSai
 * @date :2020/07/01 07:51
 * @description:实现以下功能 1. 隐藏键盘；
 * 2. 隐藏状态栏（可修改可简化）
 * 3. dp2px
 * 4. 播放声音
 * 5. 删除文件等一些公共方法
 */
public class MyUtils {
    /**
     * 重写 onTouchEvent 隐藏键盘
     *
     * @param activity 当前 活动
     * @param event    触摸手势；
     */
    public static void hideKeyBoard(Activity activity, MotionEvent event) {
        InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (activity.getCurrentFocus() != null && activity.getCurrentFocus().getWindowToken() != null && manager != null) {
                manager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    public static void hideKeyBoard(Activity activity) {
        InputMethodManager manager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (activity.getCurrentFocus() != null && activity.getCurrentFocus().getWindowToken() != null && manager != null) {
            manager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    /**
     * @param size KB
     * @return 根据文件具体大小 返回对应的 缓存区大小
     */
    public static int bufSize(long size) {
        long KB = (size / 1024);
        long MB = KB / 1024;
        long GB = MB / 1024;
        if (GB > 0) return 1024 * 1024 * 1024;// ?GB
        if (MB > 0) return 1024 * 1024;// ?MB
        if (KB > 0) return 1024;
        return 1024 * 1024;
    }
    /**
     * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, int dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * @param context
     * @param mode    1 表示提示音乐、0 表示警告音乐
     */
    public static void playSound(Context context, int mode) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        if (mode == 0) mediaPlayer = MediaPlayer.create(context, R.raw.notify);
        if (mode == 1) mediaPlayer = MediaPlayer.create(context, R.raw.warning);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.start();
        mediaPlayer.setVolume(1f, 1f);
    }


    /**
     * 删除指定文件
     *
     * @param file
     */
    public static void deleteFile(Context context, File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(context, files[i]);
                }
            }
            file.delete();
        } else {
        }
    }

    public static void chooseDate_M(Context mContext, final EditText etDate, final TextView tv) {
        final Calendar c = Calendar.getInstance();
        //日期选择器对话框
        new DatePickerDialog(mContext,
                new DatePickerDialog.OnDateSetListener() {
                    //日期改变监听器
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String monthString = (month + 1) + "";
                        String dayString = dayOfMonth + "";
                        if (month < 9) {
                            monthString = 0 + monthString;
                        }
                        if (dayOfMonth < 10) {
                            dayString = 0 + dayString;
                        }
                        if (etDate != null)
                            etDate.setText(year + "-" + monthString + "-" + dayString);
                        if (tv != null) tv.setText(year + "-" + monthString + "-" + dayString);
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * 使 输入流 转换成 String
     *
     * @param inputStream
     * @return
     */
    public static String getString(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "gbk");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuffer sb = new StringBuffer("");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    /**
     * 比较两个日期格式的 先后
     *
     * @param startDate 年-月-日
     * @return endDate > startDate 返回 true；
     */
    public static boolean compareDate(String startDate, String endDate) {
        startDate = startDate.replaceAll("-", "");
        endDate = endDate.replaceAll("-", "");
        int start = Integer.parseInt(startDate);
        int end = Integer.parseInt(endDate);
        return (end >= start) && (start / 100 == end / 100);
    }

    /**
     * @return byte 数组：data1 与 data2拼接的结果
     */
    public static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

    public static double[] getDouble(byte[] buffer) {
        double[] doubles = new double[buffer.length / 8];
        byte[] tempByte8 = new byte[8];
        for (int i = 0; i < buffer.length; ) {
            System.arraycopy(buffer, (i / 8) * tempByte8.length, tempByte8, 0, tempByte8.length);
            doubles[i / 8] = FormatUtils.byteArrayToDouble(tempByte8);
            i += 8;
        }
        return doubles;
    }

    private static final String TAG = MyUtils.class.getSimpleName();

    /**
     * 根据 60 钢轨 方程获取 钢轨轮廓尺寸 文件
     */
    Thread get60RailDataThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                //如果SD卡已准备好
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    //得到SD卡根目录
                    File sdCardDir = Environment.getExternalStorageDirectory();
                    //打开data目录，如不存在则生成
                    File buildDir = new File(sdCardDir, "/RailMeasurement/data");
                    if (!buildDir.exists()) buildDir.mkdirs();
                    //新建文件句柄，如已存在仍新建文档
                    File saveFile = new File(buildDir, "rail.txt");
                    List<Byte> railData = new ArrayList<Byte>();

                    double y = 0.0;
                    String yStr = "";
                    byte[] bytesY;
                    for (double x = 0.0; x < 36.424; x += 0.01) {
                        y = 0.0;
                        if (x < 9.951) {
                            y = Math.sqrt(300 * 300 - x * x) - 300;
                        } else if (x >= 9.951 && x < 25.35) {
                            y = -80.121 + Math.sqrt(80 * 80 - (x - 7.297) * (x - 7.297));
                        } else if (x >= 25.35 && x < 35.40) {
                            y = -14.849 + Math.sqrt(13 * 13 - (x - 22.416) * (x - 22.416));
                        } else if (x >= 35.40 && x < 36.424) {
                            y = -20 * x + 693.805;
                        }

                        y -= -20 * 36.4 + 693.805;

                        yStr = String.format("%.8f", y) + " ";
                        bytesY = yStr.getBytes();
                        for (int i = 0; i < bytesY.length; i++) {
                            railData.add(bytesY[i]);
                        }
                    }

                    byte[] railBytes = new byte[railData.size()];
                    for (int j = 0; j < railData.size(); j++) {
                        railBytes[j] = railData.get(j);
                    }
                    FileOutputStream stream = new FileOutputStream(saveFile);
                    stream.write(railBytes);
                    stream.close();
                } else {
                    //Toast.makeText(mContext, getResources().getString(R.string.noSDCard), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                return;
            }
        }
    });
}