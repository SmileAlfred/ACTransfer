# ACTransfer
安卓端，实现和C++通过Socket进行通信；

添加结构体的通信，其间涉及到大小端以及字节排序。

# 参考文档：
  https://blog.csdn.net/qq_21539671/article/details/98743397
  https://blog.csdn.net/c_o_d_e_/article/details/113092095
  Android网络编程(十四) 之 Socket与NIO: https://blog.csdn.net/lyz_zyx/article/details/104062815

# 发现的问题（2021年2月9日）
1. selectionKey 设置监听（读、写）状态，是不是仅能支持一种？
2. 如何有效切换监听状态？切换时消耗多少时间？
3. 如果仅能读、写，那么如何实现连续高效的通信？
4. 如何解决同时发送多个文件与接收多个文件而不发生混乱？
