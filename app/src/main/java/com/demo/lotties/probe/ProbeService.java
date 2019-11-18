package com.demo.lotties.probe;

import com.hello.demo.entity.HttpResult;
import com.hello.demo.entity.ProbeIP;

import io.reactivex.Observable;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ProbeService {

    @POST()
    Observable<HttpResult<ProbeIP>> getProbeIP(@Url String url);
}
