package com.example.a2atranfer.beans;

/**
 * @author: LiuSaiSai
 * @date: 2020/08/12 16:33
 * @description: 结构体类；
 * 参考：https://blog.csdn.net/qq_40384776/article/details/103711300
 * https://blog.csdn.net/lianxianxun230/article/details/77428669
 * https://www.cnblogs.com/linzhanfly/p/9794752.html
 * https://blog.csdn.net/DoasIsay/article/details/103797033
 * 基本类型转 byte 数组：https://www.cnblogs.com/moonciki/p/8145834.html
 */
public class PcStruct {

    public void setnMsgCode(int nMsgCode) {
        this.nMsgCode = nMsgCode;
    }

    public void setnDataCnt(int nDataCnt) {
        this.nDataCnt = nDataCnt;
    }

    public void setPdDataX(double[] pdDataX) {
        this.pdDataX = pdDataX;
    }

    public void setPdDataY(double[] pdDataY) {
        this.pdDataY = pdDataY;
    }

    public byte[] buf;

    public int nMsgCode;
    public int nDataCnt;
    public double pdDataX[] = new double[2];
    public double pdDataY[] = new double[2];

    public static final String TAG = PcStruct.class.getClass().getSimpleName();



    /**
     * 通过接收到的 byte[] 对象解析成结构体对象；
     * 注意：PC 接收端，发送的结构体还是 48 个字节，并没有修改，此时的顺序时 code，X[2]，Y[2],data,
     * @param bytes 来自 PC 端的字节流文件；
     * @return
     */
    public static PcStruct getPcStructInstance(byte[] bytes) {
        PcStruct pcStruct = new PcStruct();

        byte[] tempBytes4 = new byte[8];
        byte[] tempBytes8 = new byte[8];

        System.arraycopy(bytes, 0, tempBytes4, 0, 8);
        pcStruct.setnMsgCode(byteArrayToInt(tempBytes4));
        System.arraycopy(bytes, 40, tempBytes4, 0, 8);
        pcStruct.setnDataCnt(byteArrayToInt(tempBytes4));

        System.arraycopy(bytes, 8, tempBytes8, 0, 8);
        double mPdDataX0 = byteArrayToDouble(tempBytes8);
        System.arraycopy(bytes, 16, tempBytes8, 0, 8);
        double mPdDataX1 = byteArrayToDouble(tempBytes8);
        double[] mPdDataX = new double[]{mPdDataX0, mPdDataX1};
        pcStruct.setPdDataX(mPdDataX);

        System.arraycopy(bytes, 24, tempBytes8, 0, 8);
        double mPdDataY0 = byteArrayToDouble(tempBytes8);
        System.arraycopy(bytes, 32, tempBytes8, 0, 8);
        double mPdDataY1 = byteArrayToDouble(tempBytes8);
        double[] mPdDataY = new double[]{mPdDataY0, mPdDataY1};
        pcStruct.setPdDataY(mPdDataY);

        return pcStruct;
    }

    public PcStruct() {
    }

    /**
     * 这里通过构造器 + getBuf() 的方式，获取输出字节流，向PC端传输结构体；
     * PC 端已经设置为原始对齐方式，即，字节 无 对齐方式；这里需要注意的是！传递的顺序 应该和PC端定义结构参数的顺序一致
     */
    //参数字节：4+4 + 8+8+8+8
    public PcStruct(int nMsgCode, int nDataCnt, double pdDataX0, double pdDataX1, double pdDataY0, double pdDataY1) {
        byte[] tempByte4 = new byte[4];
        byte[] tempByte8 = new byte[8];
        buf = new byte[40];
        tempByte4 = toLH(nMsgCode);
        System.arraycopy(tempByte4, 0, buf, 0, 4);

        tempByte8 = toLH(pdDataX0);
        System.arraycopy(tempByte8, 0, buf, 4, 8);
        tempByte8 = toLH(pdDataX1);
        System.arraycopy(tempByte8, 0, buf, 12, 8);

        tempByte8 = toLH(pdDataY0);
        System.arraycopy(tempByte8, 0, buf, 20, 8);
        tempByte8 = toLH(pdDataY1);
        System.arraycopy(tempByte8, 0, buf, 28, 8);
        tempByte4 = toLH(nDataCnt);
        System.arraycopy(tempByte4, 0, buf, 36, 4);
    }

    /**
     * 返回要发送的数组
     */
    public byte[] getBuf() {
        return buf;
    }


    /**
     * double 转 byte[]
     * 小端 高前低后
     *
     * @return
     */
   /* public static byte[] toLH (double d) {
        long value = Double.doubleToRawLongBits(d);
        byte[] byteRet = new byte[8];
        for (int i = 0; i < 8; i++) {
            byteRet[i] = (byte) ((value >> 8 * i) & 0xff);
        }
        return byteRet;
    }*/


    /**
     * 将 int转为高字节在前，低字节在后的byte数组
     *
     * @return
     */
   /*public static byte[] toLH(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }*/


    /**
     * long转byte数组，小端模式
     *
     * @param value
     * @return
     */
    long longToBytes_Little(long value) {
        byte bytes[] = new byte[8];
        bytes[7] = (byte) (0xff & (value >> 56));
        bytes[6] = (byte) (0xff & (value >> 48));
        bytes[5] = (byte) (0xff & (value >> 40));
        bytes[4] = (byte) (0xff & (value >> 32));
        bytes[3] = (byte) (0xff & (value >> 24));
        bytes[2] = (byte) (0xff & (value >> 16));
        bytes[1] = (byte) (0xff & (value >> 8));
        bytes[0] = (byte) (0xff & value);

        return (0xff00000000000000L & ((long) bytes[0] << 56)
                | (0xff000000000000L & ((long) bytes[1] << 48))
                | (0xff0000000000L & ((long) bytes[2] << 40))
                | (0xff00000000L & ((long) bytes[3] << 32))
                | (0xff000000L & ((long) bytes[4] << 24))
                | (0xff0000L & ((long) bytes[5] << 16))
                | (0xff00L & ((long) bytes[6] << 8)) | (0xffL & (long) bytes[7]));
    }

    /**
     * long 转 byte[]
     * 小端
     *
     * @param data
     * @return
     */
    static final long fx = 0xffL;

    public static byte[] getLongBytes(long data) {
        int length = 8;
        byte[] bytes = new byte[length];

        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((data >> (i * 8)) & fx);
        }
        return bytes;
    }


    @Override
    public String toString() {
        return "PcStruct{" +
                "nMsgCode=" + nMsgCode +
                ", nDataCnt=" + nDataCnt +
                ", pdDataX[0]=" + pdDataX[0] +
                ", pdDataX[1]=" + pdDataX[1] +
                ", pdDataY[0]=" + pdDataY[0] +
                ", pdDataY[1]=" + pdDataY[1] +
                '}';
    }


    //不对
    /*private static byte[] toLH(int a) {
          byte[] bs = new byte[4];
        for (int i = bs.length - 1; i >= 0; i--) {
            bs[i] = (byte) (a % 0xFF);
            a = a / 0xFF;
        }
        return bs;
    }*/
    //不对
    /*public static byte[] toLH(double d) {
        byte[] output = new byte[8];
        long lng = Double.doubleToLongBits(d);
        for (int i = 0; i < 8; i++) {
            output[i] = (byte) ((lng >> ((7 - i) * 8)) & 0xff);
        }
        return output;
    }*/

    /**
     * 将int转为低字节在前，高字节在后的byte数组
     */
    private static byte[] toLH(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    /**
     * double 转 byte[] 小端 低前高后
     */
    public static byte[] toLH(double data) {
        long intBits = Double.doubleToLongBits(data);
        byte[] bytes = getLongBytes(intBits);
        return bytes;
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
    public static double byteArrayToDouble(byte[] b) {
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
