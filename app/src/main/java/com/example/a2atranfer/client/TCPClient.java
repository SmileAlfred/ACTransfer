package com.example.a2atranfer.client;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.a2atranfer.activity.A2ATransferActivity;
import com.example.a2atranfer.beans.MsgBean;
import com.example.a2atranfer.utils.Logger;
import com.example.a2atranfer.utils.MyUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TCPClient {
    private static final String TAG = TCPClient.class.getSimpleName();

    private String mSendMsg;
    private byte[] msgBuf;
    private String mClientName;     // 客户端命名
    private Selector mSelector;
    private SocketChannel mSocketChannel;

    private ThreadPoolExecutor mConnectThreadPool;  // 消息连接和接收的线程池
    private Context mContext;
    private Handler mHandler;


    public TCPClient(Context context, Handler handler, String clientName) {
        this.mContext = context;
        this.mHandler = handler;
        msgBuf = new byte[128];
        init(clientName);
    }

    /**
     * 基本初始化
     *
     * @param clientName
     */
    private void init(String clientName) {
        mClientName = clientName;
        mConnectThreadPool = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "client_connection_thread_pool");
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        Log.i(TAG, mClientName + " 已启动连接，请免重复操作");

                        //更新UI：服务器界面显示【客户端返回的消息：】
                        Message mMessage = new Message();
                        mMessage.obj = mClientName + " 已启动连接，请免重复操作";
                        mMessage.what = A2ATransferActivity.LOG;
                        mHandler.sendMessage(mMessage);
                    }
                }
        );
    }

    /**
     * 请求连接服务端
     */
    public void requestConnectTcp(final String ipAdress) {
        mConnectThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                initSocketAndReceiveMsgLoop(ipAdress);
            }
        });
    }

    /**
     *
     */
    private void initSocketAndReceiveMsgLoop(String ipAdress) {
        try {
            mSocketChannel = SocketChannel.open();
            // 设置为非阻塞方式
            mSocketChannel.configureBlocking(false);
            // 连接服务端地址和端口
            mSocketChannel.connect(new InetSocketAddress(ipAdress, 9999));

            // 注册到Selector，请求连接
            mSelector = Selector.open();
            mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);
            while (mSelector != null && mSelector.isOpen() && mSocketChannel != null && mSocketChannel.isOpen()) {
                // 选择一组对应Channel已准备好进行I/O的Key
                int select = mSelector.select();     // 当没有消息时，这里也是会阻塞的
                if (select <= 0) {
                    continue;
                }
                Set<SelectionKey> selectionKeys = mSelector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    // 移除当前的key
                    iterator.remove();

                    if (selectionKey.isValid() && selectionKey.isConnectable()) {
                        handleConnect();
                    }
                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        Message mMessage = new Message();
                        mMessage.obj = "handleRead() 前" ;
                        mMessage.what = A2ATransferActivity.LOG;
                        mHandler.sendMessage(mMessage);
                        handleRead();
                    }
                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        handleWrite();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void handleConnect() throws IOException {
        // 判断此通道上是否正在进行连接操作。
        if (mSocketChannel.isConnectionPending()) {
            mSocketChannel.finishConnect();
            mSocketChannel.register(mSelector, SelectionKey.OP_READ);
            Log.i(TAG, mClientName + " 请求跟服务端建立连接");
        }
    }

    private void handleRead()  {

        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
       try{
           int bytesRead = mSocketChannel.read(byteBuffer);
           Message mMessage = new Message();
           mMessage.obj = "handleRead() 中："+Arrays.toString(byteBuffer.array()) ;
           mMessage.what = A2ATransferActivity.LOG;
           mHandler.sendMessage(mMessage);

           if (bytesRead > 0) {
               MsgBean msgBean = MsgBean.getStruct(byteBuffer.array());
               switch (msgBean.msg_code) {
                   case MsgBean.ORDER_REQUEST_CONNECTION://握手
                       MyUtils.playSound(mContext, 0);
                       A2ATransferActivity.connectSuccessful = true;

                       mMessage = new Message();
                       mMessage.obj = "正在进行通信...";
                       mMessage.what = A2ATransferActivity.LOCALIPMSG;
                       mHandler.sendMessage(mMessage);
                       break;
                   case MsgBean.ORDER_REQUEST_IS_RECEIVE://请求是否接收文件？

                       break;


                   case MsgBean.ORDER_ALLOW_SEND://允许接收文件
                       Message msg = new Message();
                       msg.obj = "服务器 同意 我方发送文件";
                       msg.what = A2ATransferActivity.CLIENTGETMSG;
                       mHandler.sendMessage(msg);
                       break;
                   case MsgBean.ORDER_REJECT_SEND://拒绝接收文件
                       msg = new Message();
                       msg.obj = "服务器 拒绝 我方发送文件";
                       msg.what = A2ATransferActivity.CLIENTGETMSG;
                       mHandler.sendMessage(msg);
                       break;
                   case MsgBean.ORDER_SEND_STR://谨发送消息
                       msgBean = new MsgBean(MsgBean.ORDER_SEND_STR, "客户端暂时只能发送这些了", 0);
                       sendMsg(msgBean);
                       break;
                   case MsgBean.ORDER_SUCESS__MSG://接收成功
                       break;
                   default:
                       break;
               }

           /* String inMsg = new String(byteBuffer.array(), 0, bytesRead);
            //更新UI：客户端界面显示【服务器返回的消息：】
            Message msg = new Message();
            msg.obj = inMsg;
            msg.what = A2ATransferActivity.CLIENTGETMSG;
            mHandler.sendMessage(msg);
            Log.i(TAG, mClientName + " (更新 UI 后)收到服务端数据： " + inMsg);*/
           } else {
               Log.i(TAG, mClientName + "  断开跟 服务端的连接");

               //更新UI：服务器界面显示【客户端返回的消息：】
               mMessage = new Message();
               mMessage.obj = mClientName + " 断开跟 服务端的连接";
               mMessage.what = A2ATransferActivity.LOG;
               mHandler.sendMessage(mMessage);
               disconnectTcp();
           }
       }catch (Exception e){
           Message  mMessage = new Message();
           mMessage.obj = "handleRead 报错："+e.getMessage();
           mMessage.what = A2ATransferActivity.LOG;
           mHandler.sendMessage(mMessage);
       }

    }

    private void handleWrite() throws IOException {
       /* if (TextUtils.isEmpty(mSendMsg)) {
            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage = new Message();
            mMessage.obj = "handleWrite 内容为空";
            mMessage.what = A2ATransferActivity.LOG;
            mHandler.sendMessage(mMessage);
            return;
        }*/
        ByteBuffer sendBuffer = ByteBuffer.allocate(128);
        //sendBuffer.put(mSendMsg.getBytes());
        sendBuffer.put(msgBuf);
        sendBuffer.flip();

        mSocketChannel.write(sendBuffer);

        mSendMsg = null;
        mSocketChannel.register(mSelector, SelectionKey.OP_READ);
    }

    /**
     * 发送数据
     *
     * @param msg
     * @throws IOException
     */
    public void sendMsg(MsgBean msg) throws ClosedChannelException {
        if (mSelector == null || !mSelector.isOpen() || mSocketChannel == null || !mSocketChannel.isOpen()) {
            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage = new Message();
            mMessage.obj = "sendMsg 失败！值为null或未开启";
            mMessage.what = A2ATransferActivity.LOG;
            mHandler.sendMessage(mMessage);

            return;
        }

        //mSendMsg = msg;
        msgBuf = msg.getBuf();
        mSocketChannel.register(mSelector, SelectionKey.OP_WRITE);
        mSelector.wakeup();

        /*//更新UI：服务器界面显示【客户端返回的消息：】
        Message mMessage = new Message();
        mMessage.obj = "已发送：" + mSendMsg;
        mMessage.what = A2ATransferActivity.LOG;
        mHandler.sendMessage(mMessage);*/
    }


    /**
     * 发送文件
     * TODO:使用选择器
     *
     * @param path 文件路径
     * @throws IOException
     */
    public void sendFile(String path) throws Exception {
        if (mSelector == null || !mSelector.isOpen() || mSocketChannel == null || !mSocketChannel.isOpen()) {
            //更新UI：服务器界面显示【客户端返回的消息：】
            Message mMessage = new Message();
            mMessage.obj = "sendMsg 失败！值为null或未开启";
            mMessage.what = A2ATransferActivity.LOG;
            mHandler.sendMessage(mMessage);
            return;
        }

        FileInputStream fis = new FileInputStream(path);

        FileChannel fileChannel = fis.getChannel();
        long size = fileChannel.size();

        //注：放在了发送信息中
        /* *//****************向服务端写入文件大小 *********************//*
        ByteBuffer byteBuf_size = ByteBuffer.allocateDirect(8);//long 容量不得小于8
        byteBuf_size.putLong(size);
        byteBuf_size.flip();
        mSocketChannel.write(byteBuf_size);
        byteBuf_size.clear();
        *//****************向服务端写入文件大小*完成 *********************/

        /****************向服务端写入文件内容 *********************/
        ByteBuffer byteBufFile = ByteBuffer.allocateDirect(MyUtils.bufSize(size));
        int len = -1;
        while ((len = fileChannel.read(byteBufFile)) != -1) {
            byteBufFile.flip();
            while (byteBufFile.hasRemaining()) {//保证字节全部写入
                mSocketChannel.write(byteBufFile);
            }
            byteBufFile.clear();
        }
        fileChannel.close();
        fis.close();


        //TODO:服务器返回接受完成时 → 更新UI：服务器界面显示【客户端返回的消息：】
        Message mMessage = new Message();
        mMessage.obj = "已发送：" + path;
        mMessage.what = A2ATransferActivity.LOG;
        mHandler.sendMessage(mMessage);

    }


    /**
     * 断开连接
     */
    public void disconnectTcp() {
        Log.i(TAG, "--------------------------------------");
        Log.i(TAG, mClientName + " 主动断开跟服务端连接");

        //更新UI：服务器界面显示【客户端返回的消息：】
        Message mMessage = new Message();
        mMessage.obj = mClientName + " 主动断开跟服务端连接";
        mMessage.what = A2ATransferActivity.LOG;
        mHandler.sendMessage(mMessage);
        close();
    }

    /**
     * 断开连接
     */
    private void close() {
        try {
            if (mSelector != null && mSelector.isOpen()) {
                mSelector.close();
            }
            if (mSocketChannel != null && mSocketChannel.isOpen()) {
                mSocketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
