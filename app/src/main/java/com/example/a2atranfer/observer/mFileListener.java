package com.example.a2atranfer.observer;

import android.os.FileObserver;

import com.example.a2atranfer.utils.Logger;

public class mFileListener extends FileObserver {

    private static final String TAG = mFileListener.class.getSimpleName();
    private EventCallback callback;

    public mFileListener(String path) {
        super(path);
    }

    public void setEventCallback(EventCallback callback) {
        this.callback = callback;
    }

    /**
     * @param event 文件的变更信息
     * @param path  具体的文件名（指定监听目录下的）；
     */
    @Override
    public void onEvent(int event, String path) {
        switch (event) {
            case FileObserver.ACCESS:// 文件被访问
                Logger.i(TAG,path + " 文件被访问");
                break;

            case FileObserver.MODIFY:// 文件被修改
                Logger.i(TAG,path + " 文件被修改");
                break;

            case FileObserver.ATTRIB:// 文件属性被修改   FileObserver.ATTRI
                Logger.i(TAG,path + " 文件属性被修改");
                break;

            case FileObserver.CLOSE_WRITE:// 可写文件被close
                Logger.i(TAG,path + " 可写文件被关闭");
                if (callback != null) {
                    callback.onEvent(path);
                }
                break;

            case FileObserver.CLOSE_NOWRITE:// 不可写文件被close
                Logger.i(TAG,path + " 不可写文件被关闭");
                break;

            case FileObserver.OPEN:// 文件被打开
                Logger.i(TAG,path + " 文件被打开");
                break;

            case FileObserver.MOVED_FROM:// 文件被移走
                Logger.i(TAG,path + " 文件被移走");
                break;

            case FileObserver.MOVED_TO:// 文件被移进来
                Logger.i(TAG,path + " 文件被移入");
                break;

            case FileObserver.DELETE:// 文件被删除
                Logger.i(TAG,path + " 文件被删除");
                break;

            case FileObserver.CREATE:// 创建新文件
                Logger.i(TAG,path + " 创建新文件");
                break;

            case FileObserver.DELETE_SELF:// 自删除
                Logger.i(TAG,path + " 文件自删除");
                break;

            case FileObserver.MOVE_SELF:// 自移动
                Logger.i(TAG,path + " 文件自移动");
                break;
        }
    }

    public interface EventCallback {
        void onEvent(String path);
    }
}