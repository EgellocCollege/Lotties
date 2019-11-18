package com.demo.lotties.entity;

/**
 * Created by nice on 2017/9/26.
 */

public class UpdateInfo {

    private String url;
    private String testip;
    private String traceip;
    private String tcpport;
    private String protocol;
    private String filename;

//    private String versionInfo;
//    private int versioncode;

    public String getTraceip() {
        return traceip;
    }

    public void setTraceip(String traceip) {
        this.traceip = traceip;
    }
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTestip() {
        return testip;
    }

    public void setTestip(String testip) {
        this.testip = testip;
    }

    public String getTcpport() {
        return tcpport;
    }

    public void setTcpport(String tcpport) {
        this.tcpport = tcpport;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    @Override
    public String toString() {
        return "UpdateInfo{" +
                "url='" + url + '\'' +
                ", testip='" + testip + '\'' +
                ", traceip='" + traceip + '\'' +
                ", tcpport='" + tcpport + '\'' +
                ", protocol='" + protocol + '\'' +
                ", filename='" + filename + '\'' +
//                ", versionInfo='" + versionInfo + '\'' +
//                ", versioncode=" + versioncode +
                '}';
    }
}
