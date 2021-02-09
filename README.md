# ACTransfer
安卓端，实现和C++通过Socket进行通信；

添加结构体的通信，其间涉及到大小端以及字节排序。

# 参考文档：
  https://blog.csdn.net/qq_21539671/article/details/98743397
  https://blog.csdn.net/c_o_d_e_/article/details/113092095
  Android网络编程(十四) 之 Socket与NIO: https://blog.csdn.net/lyz_zyx/article/details/104062815

# 发现的问题
##  2021年2月9日
 1. selectionKey 设置监听（读、写）状态，是不是仅能支持一种？
 2. 如何有效切换监听状态？切换时消耗多少时间？
 3. 如果仅能读、写，那么如何实现连续高效的通信？
 4. 如何解决同时发送多个文件与接收多个文件而不发生混乱？

# 问题解决
##  2021年2月9日
1. selectionKey 设置监听（读、写）状态，仅能支持一种;
2. 有效的切换监听状态已经在知识点中写道；
3. 高效的进行通信，要注意不要在消息处理方法中做太耗时操作，这样会导致无法进行下一次的读或写；
4. 同时发送多个文件，采用多线程不靠谱，必须新建客户端重新建立连接，进行发送，发送完成后关闭客户端，这一切应该写在线程池中；

# 知识点
##  2021年2月9日
1. 当向通道中注册 SelectionKey.OP_READ 事件后，如果客户端有向缓存中write数据，下次轮询时，则会 isReadable()=true；
    当向通道中注册 SelectionKey.OP_WRITE 事件后，这时你会发现当前轮询线程中isWritable()一直为ture，如果不设置为其他事件
2. 请注意，缓冲区的相关知识，尤其是读写前的指针操作 buffer.flip();

