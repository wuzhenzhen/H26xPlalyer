package com.kgd.h26xplalyer.yuvplayer;

public class ByteUtils {

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于（低位在前，高位在后）的顺序,和bytesToInt()配套使用
     * @param value
     * @param value：要转换的int值
     * @return byte数组
     */
    public static byte[] intLowUpToBytes( int value )
    {
        byte[] src = new byte[4];
        src[3] =  (byte) ((value>>24) & 0xFF);
        src[2] =  (byte) ((value>>16) & 0xFF);
        src[1] =  (byte) ((value>>8)  & 0xFF);
        src[0] =  (byte) (value & 0xFF);
        return src;
    }

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于（高位在前,低位在后）的顺序,和bytesToInt2()配套使用
     *  @param value：要转换的int值
     * @return byte数组
     */
    public static byte[] intUpLowToBytes2(int value)
    {
        byte[] src = new byte[4];
        src[0] = (byte) ((value>>24) & 0xFF);
        src[1] = (byte) ((value>>16) & 0xFF);
        src[2] = (byte) ((value>>8)  & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }
    /* 从byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes()配套使用
     *
     * @param src: byte数组
     * @param offset: 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesLowUpToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset+1] & 0xFF)<<8)
                | ((src[offset+2] & 0xFF)<<16)
                | ((src[offset+3] & 0xFF)<<24));
        return value;
    }
    /**
     * 从byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesUpLowToInt2(byte[] src, int offset) {
        int value;
        value =  (src[offset+3] & 0xFF) |
                (src[offset+2] & 0xFF)<<8 |
                (src[offset+1] & 0xFF)<<16 |
                ((src[offset] & 0xFF)<<24);
        return  value ;
    }

    public static int pinJie2ByteToInt(byte byte1, byte byte2) {
        int result = byte1&0xff;
        result = (result << 8) | (0x00FF & (byte2&0xff));
        return result;
    }

    public static long bytes2long(byte[] bs)  throws Exception {
        int bytes = bs.length;
        if(bytes > 1) {
            if((bytes % 2) != 0 || bytes > 8) {
                throw new Exception("not support");
            }}
        switch(bytes) {
            case 0:
                return 0;
            case 1:
                return (long)((bs[0] & 0xff));
            case 2:
                return (long)((bs[0] & 0xff) <<8 | (bs[1] & 0xff));
            case 4:
                return (long)((bs[0] & 0xffL) <<24 | (bs[1] & 0xffL) << 16 | (bs[2] & 0xffL) <<8 | (bs[3] & 0xffL));
            case 8:
                return (long)((bs[0] & 0xffL) <<56 | (bs[1] & 0xffL) << 48 | (bs[2] & 0xffL) <<40 | (bs[3] & 0xffL)<<32 |
                        (bs[4] & 0xffL) <<24 | (bs[5] & 0xffL) << 16 | (bs[6] & 0xffL) <<8 | (bs[7] & 0xffL));
            default:
                throw new Exception("not support");
        }
        //return 0;
    }

}
