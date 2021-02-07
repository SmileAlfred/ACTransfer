package com.example.a2atranfer.beans;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * @author: LiuSaiSai
 * @date: 2020/08/13 20:29
 * @description:
 */
public class PcStructure extends Structure {
    public int nMsgCode;
    public double pdDataX[] = new double[2];
    public double pdDataY[] = new double[2];
    public int nDataCnt;

    public static class ByReference extends PcStructure implements Structure.ByReference {}
    public static class ByValue extends PcStructure implements Structure.ByValue {}


    @Override
    protected List getFieldOrder() {
        return  Arrays.asList(new String[] { "nMsgCode", "pdDataX[]","pdDataY[]", "nDataCnt"});
    }

    public void sayUser(PcStructure.ByReference struct) {

    }
}
