package com.demo.lotties;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import android.os.Bundle;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Retrofit retrofitIP, retrofitProbe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        retrofitIP = new Retrofit.Builder()
                .client(new OkHttpClient
                        .Builder()
                        .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(StringConverterFactory.create())
                .baseUrl(YY_BASE_URL)
                .build();

        yyIpService = retrofitIP.create(YyIpService.class);

        yyIpService.getPublicIP("get_ip_info.php/")
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                        Log.e(TAG, "获取YY IP : Subscribe At Thread:" + Thread.currentThread().getName());
                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull String jsonResult) {
                        Log.e(TAG, "获取YY IP : OnNext " + jsonResult);
//                                yyIp = JSONObject.parseObject(jsonResult.substring(jsonResult.indexOf("{"),jsonResult.length()-1),YyIp.class);
                        publicIp = JSONObject.parseObject(jsonResult.substring(jsonResult.indexOf("{"), jsonResult.length() - 1), YyIp.class);

                        tvPublicIP.setText(((YyIp) publicIp).getCip());
                        tvPublicISP.setText(((YyIp) publicIp).getIsp());
                        tvPublicOwnership.setText(((YyIp) publicIp).getProvince() + ((YyIp) publicIp).getCity());
                        tvPublicTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date().getTime()));
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {

                        Log.e(TAG, "获取YY IP : onError " + e.getMessage());

                        YYIPService = false;

                        publicIp = new YyIp(); // 避免获取 query_url  报 null 错误

                        Log.e(TAG, "获取YY IP : onError ");
                        tvPublicIP.setText("分析失败");
                        tvPublicISP.setText("分析失败");
                        tvPublicOwnership.setText("分析失败");
                        tvPublicTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));
                        emitterPublicIp.onNext(PublicIp_Error);
                        emitterPublicIp.onComplete();
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "获取YY IP : onComplete ");
                        emitterPublicIp.onNext(Response_OK);
                        emitterPublicIp.onComplete();
                        Log.e(TAG, "获取YY IP  ：onComplete At" + Thread.currentThread().getName());
                    }
                });

    }
}).subscribeOn(Schedulers.io());
    }


}
