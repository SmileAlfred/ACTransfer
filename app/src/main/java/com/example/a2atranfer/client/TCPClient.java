package com.example.a2atranfer.client;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.a2atranfer.R;
import com.example.a2atranfer.activity.A2ATransferActivity;
import com.example.a2atranfer.beans.MsgBean;
import com.example.a2atranfer.utils.Logger;
import com.example.a2atranfer.utils.MyUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.Struct;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TCPClient {
    private static final String TAG = TCPClient.class.getSimpleName();

    private byte[] msgBytes;
    private ByteBuffer byteBufMsg, byteBufContent;

    private String mClientName,             //客户端命名
            fileWholeName;                  //文件 路径+名称
    private boolean transfering = false;    //是否正在传输文件？

    private Selector mSelector;
    private SocketChannel mSocketChannel;

    private ThreadPoolExecutor mConnectThreadPool;  // 消息连接和接收的线程池
    private Context mContext;
    private Handler mHandler;
    private long fileLength = -1L;


    public TCPClient(Context context, Handler handler, String clientName) {
        this.mContext = context;
        this.mHandler = handler;
        byteBufMsg = ByteBuffer.allocate(64);
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
        //如果此前未连接，那么提醒 此时已连接
        if (!A2ATransferActivity.connectSuccessful) {
            MyUtils.playSound(mContext, 0);
            A2ATransferActivity.connectSuccessful = true;

            Message mMessage = new Message();
            mMessage.obj = "正在进行通信...";
            mMessage.what = A2ATransferActivity.LOCALIPMSG;
            mHandler.sendMessage(mMessage);

            Message message = new Message();
            message.obj = "服务端同意了连接请求";
            message.what = A2ATransferActivity.LOG;
            mHandler.sendMessage(message);
        }

        // 判断此通道上是否正在进行连接操作。
        if (mSocketChannel.isConnectionPending()) {
            mSocketChannel.finishConnect();
            mSocketChannel.register(mSelector, SelectionKey.OP_READ);
        }
    }

    private void handleRead() throws Exception {
        if (transfering) {
            Message.obtain(mHandler, A2ATransferActivity.LOG, "终于要写入文件了！").sendToTarget();
            int bufferSize = (int) MyUtils.bufSize(fileLength);
            //if (null == byteBufContent || bufferSize != byteBufContent.capacity())
            byteBufContent = ByteBuffer.allocate(bufferSize);

            FileChannel fileChannel = new FileOutputStream(fileWholeName).getChannel();

            int writed = 0;
            int len = -1;

            //将客户端写入通道的数据读取并存储到buffer中
            while (writed < fileLength) {
                //while ((len = client.read(byteBufContent)) > 0) {
                len = mSocketChannel.read(byteBufContent);
                //这里睡 10ms 很关键！这里是多线程的问题；TODO:多线程解决这个愚蠢的解决方式……
                if (len <= 0) {
                    Thread.sleep(5);
                    continue;
                }

                writed += len;
                //System.out.println(writed + " / "+fileLength + " B");
                byteBufContent.flip();//将缓冲区翻转为读模式

                fileChannel.write(byteBufContent);
                Message.obtain(mHandler, A2ATransferActivity.PROGRESS, writed + "/" + fileLength).sendToTarget();


                byteBufContent.clear();//清除本次缓存区内容
            }
            Message.obtain(mHandler, A2ATransferActivity.TRANSfERMSG, "文件接收完成；保存在：" + fileWholeName + "; " + fileChannel.size()).sendToTarget();

            fileChannel.close();
            transfering = false;
            fileLength = -1L;
            fileWholeName = "";
        } else {
            //读取服务器发送来的数据到缓冲区中
            byteBufMsg = ByteBuffer.allocate(64);
            int bytesRead = mSocketChannel.read(byteBufMsg);
            byteBufMsg.flip();

            //发送来的消息 非空，那么读取
            if (bytesRead > 0) {
                //对消息进行处理，若发来的是文件，那么返回文件大小（byte）
                fileLength = handleReceivedMsg(byteBufMsg.array());
                //如果文件大小 > 0 ,那么就是开辟一个新的缓存区，对文件进行接收
                if (fileLength > 0) transfering = true;

                byteBufMsg.clear();
            } else {
                //disconnectTcp();
            }
        }
    }

    /**
     * 对服务器发送来的 命令 进行解析和处理，
     *
     * @param array 命令对象 的 array
     * @return 返回文件的大小，若不是传输文件返回 -1
     * @throws Exception
     */
    private long handleReceivedMsg(byte[] array) throws Exception {
        final MsgBean msgBeanReceived = MsgBean.getStruct(array);
        Message.obtain(mHandler, A2ATransferActivity.TRANSfERMSG, "接收到命令：" + msgBeanReceived).sendToTarget();

        final MsgBean msgBean4Send = new MsgBean();
        Logger.i(TAG, "客户端收到MSG：" + msgBeanReceived);
        long fileLength = -1L;
        int msg_code = msgBeanReceived.getMsg_code();
        final String name;
        switch (msg_code) {
            case MsgBean.ORDER_REQUEST_IS_RECEIVE://请求是否接收文件？
                name = msgBeanReceived.getFileName();
                fileWholeName = MyUtils.createFile(name);
                fileLength = msgBeanReceived.getFileLength();
                Message.obtain(mHandler, A2ATransferActivity.TRANSfERMSG, "服务器正发送文件：" + name + " ; 文件大小：" + fileLength + " B").sendToTarget();
                Message.obtain(mHandler, A2ATransferActivity.ISRECEIVE, name).sendToTarget();

                //Test          break;
                //Test      case "N":
                //Test          msgBean4Send = new MsgBean(MsgBean.ORDER_REJECT_SEND, "", 0);
                //Test          sendMsg(selectionKey, msgBean4Send);
                //Test          break;
                //Test      default:
                //Test          scanner.close();
                //Test          break;
                //Test  }
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
                //更新UI：服务器界面显示【客户端返回的消息：】
                Message mMessage = new Message();
                mMessage.obj = msgBeanReceived;
                mMessage.what = A2ATransferActivity.SERVER;
                mHandler.sendMessage(mMessage);
                break;
            case MsgBean.ORDER_SUCESS__MSG://接收成功
                //TODO:服务器接收成功的反馈消息 的处理
                break;
            default:
                break;
        }
        return fileLength;
    }


    private void handleWrite() throws IOException {
        if (null == msgBytes) {
            return;
        }

        ByteBuffer sendBuffer = ByteBuffer.allocate(64);
        //sendBuffer.put(mSendMsg.getBytes());
        sendBuffer.put(msgBytes);
        sendBuffer.flip();

        mSocketChannel.write(sendBuffer);
        msgBytes = null;
        //不可删除
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
            mMessage.obj = "sendMsg失败,mSelector = " + mSelector + " ; mSelector.isOpen() = " + mSelector.isOpen()
                    + " ; mSocketChannel = " + mSocketChannel + "mSocketChannel.isOpen() = " + mSocketChannel.isOpen();
            mMessage.what = A2ATransferActivity.LOG;
            mHandler.sendMessage(mMessage);

            return;
        }

        //mSendMsg = msg;
        msgBytes = msg.getBuf();
        mSocketChannel.register(mSelector, SelectionKey.OP_WRITE);
        mSelector.wakeup();

        /*//更新UI：服务器界面显示【客户端返回的消息：】*/
        Message mMessage = new Message();
        mMessage.obj = msg.fileName;
        mMessage.what = A2ATransferActivity.LOG;
        mHandler.sendMessage(mMessage);
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
