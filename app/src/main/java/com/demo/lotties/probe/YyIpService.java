package com.demo.lotties.probe;

import io.reactivex.Observable;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by nice on 2017/8/30.
 */

public interface YyIpService {

    // Observable 是为了和RxJava配合实现时间订阅
    @Headers({
            "User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0"
    })
    @POST
   Observable<String> getPublicIP(@Url String url);
}
