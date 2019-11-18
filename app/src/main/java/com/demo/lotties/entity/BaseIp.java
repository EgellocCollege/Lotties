package com.demo.lotties.entity;

/**
 * Created by nice on 2017/10/30.
 */

public abstract class BaseIp {
    private String country;
    private String isp;
    private String city;

    public abstract String toUrlString(String probeTime);
}
