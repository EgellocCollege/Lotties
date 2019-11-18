package com.demo.lotties.probe;

import com.hello.demo.entity.HttpResult;
import com.hello.demo.entity.TaobaoIp;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Url;

/**
 * Created by nice on 2017/10/30.
 */

public interface TaobaoIpService {
    @Headers({
            "User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0"
    })
    @GET
    Observable<HttpResult<TaobaoIp>> getPublicIP(@Url String url);

}
