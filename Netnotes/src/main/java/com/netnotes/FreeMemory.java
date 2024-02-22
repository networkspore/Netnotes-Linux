package com.netnotes;

import com.google.gson.JsonObject;

public class FreeMemory {

    public static double k = 1024;

    private long m_swapTotal = -1;
    private long m_swapFree = -1;
    private long m_memFree = -1;
    private long m_memAvailable = -1;
    private long m_memTotal = -1;


    public FreeMemory(long  swapTotalKB, long swapFreeKB, long memFreeKB, long memAvailableKB, long memTotalKB){
        m_swapTotal = swapTotalKB;
        m_swapFree = swapFreeKB;
        m_memFree = memFreeKB;
        m_memAvailable = memAvailableKB;
        m_memTotal = memTotalKB;
    }

    public long getSwapTotalKB(){
        return m_swapTotal;
    }
    public void setSwapTotalKB(long kB){
        m_swapTotal = kB;
    }

    public long getSwapFreeKB(){
        return m_swapFree;
    }
    public void setSwapFreeKB(long kB){
        m_swapFree = kB;
    }

    public void setMemFreeKB(long kB){
        m_memFree = kB;
    }

    public long getMemFreeKB(){
        return m_memFree;
    }

    public void setMemAvailableKB(long kB){
        m_memAvailable = kB;
    }

    public long getMemAvailableKB(){
        return m_memAvailable;
    }

    public long getMemTotalKB(){
        return m_memTotal;
    }

    public void setMemTotalKB(long kB){
        m_memTotal = kB;
    }

    public double getSwapTotalGB(){
        return kBToGB(m_swapTotal);
    }
    public double getSwapFreeGB(){
        return kBToGB(m_swapFree);
    }
    public double getMemFreeGB(){
        return kBToGB(m_memFree);
    }
    public double getMemAvailableGB(){
        return kBToGB(m_memAvailable);
    }
    public double getMemTotalGB(){
        return kBToGB(m_memTotal);
    }

    public double getSwapTotalMB(){
        return kBToMB(m_swapTotal);
    }
    public double getSwapFreeMB(){
        return kBToMB(m_swapFree);
    }
    public double getMemFreeMB(){
        return kBToMB(m_memFree);
    }
    public double getMemAvailableMB(){
        return kBToMB(m_memAvailable);
    }
    public double getMemTotalMB(){
        return kBToMB(m_memTotal);
    }

    

    public static int floorKBytestoMB(long kB){
        return (int) Math.floor(kBToMB(kB));
    }



    public static double kBToMB(long kB){
        return  kB / k;
    }

    public static double kBToGB(long kB){
        return kB / Math.pow(k, 2);
    }

    public static int floorKBToGB(long kB){
        return (int) Math.floor(kBToGB(kB));
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("swapFree", m_swapFree);
        json.addProperty("swapTotal", m_swapTotal);
        json.addProperty("memFree", m_memFree);
        json.addProperty("memTotal", m_memTotal);
        json.addProperty("memAvailable", m_memAvailable);

        return json;
    }
}
