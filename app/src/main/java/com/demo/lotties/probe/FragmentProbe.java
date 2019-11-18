package com.demo.lotties.probe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.baidu.location.BDLocation;
import com.hello.demo.LocationApplication;
import com.hello.demo.R;
import com.hello.demo.StringConverterFactory;
import com.hello.demo.Utils.AppUtils;
import com.hello.demo.Utils.IntenetUtil;
import com.hello.demo.Utils.PingIpUtil;
import com.hello.demo.Utils.SystemUtils;
import com.hello.demo.entity.BaseIp;
import com.hello.demo.entity.CdmaInformation;
import com.hello.demo.entity.HttpResult;
import com.hello.demo.entity.Lteinfo;
import com.hello.demo.entity.ProbeIP;
import com.hello.demo.entity.RouterInfo;
import com.hello.demo.entity.TaobaoIp;
import com.hello.demo.entity.YyIp;
import com.hello.demo.location.ILocationState;
import com.hello.demo.location.LocationService;
import com.hello.demo.location.RxBDAbstractLocationListener;
import com.hello.demo.myPhoneStateListener;
import com.hello.demo.ssl.SSLHelper;
import com.hello.demo.ssl.UnSafeHostnameVerifier;
import com.hello.demo.view.HomeActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.fastjson.FastJsonConverterFactory;

import static android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IDEN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static com.hello.demo.LocationApplication.DEFAULT_TIMEOUT;
import static com.hello.demo.LocationApplication.Probe_IP;
import static com.hello.demo.LocationApplication.Probe_Index;
import static com.hello.demo.LocationApplication.Probe_Port;
import static com.hello.demo.LocationApplication.Probe_Protocol;
import static com.hello.demo.LocationApplication.isWifiStateChanged;


/**
 * Created by nice on 2017/10/30.
 */


public class FragmentProbe extends Fragment implements ILocationState, EasyPermissions.PermissionCallbacks {


    private static final String[] GROUP_PERM =
            {Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

    private static final int RC_GROUP_PERM = 120;


    @BindView(R.id.tvPublicIP)
    public TextView tvPublicIP;
    @BindView(R.id.tvProbeIP)
    public TextView tvProbeIP;
    @BindView(R.id.tvPublicISP)
    public TextView tvPublicISP;
    @BindView(R.id.tvProbeISP)
    public TextView tvProbeISP;
    @BindView(R.id.tvPublicOwnerShip)
    public TextView tvPublicOwnership;
    @BindView(R.id.tvProbeOwnerShip)
    public TextView tvProbeOwnership;
    @BindView(R.id.tvProbeTime)
    public TextView tvProbeTime;
    @BindView(R.id.tvPublicTime)
    public TextView tvPublicTime;
    @BindView(R.id.tvLatitude)
    public TextView tvLatitude;
    @BindView(R.id.tvLongitude)
    public TextView tvLongitude;
    @BindView(R.id.tvLocAddr)
    public TextView tvLocAddr;
    @BindView(R.id.tvLocAddrDesc)
    public TextView tvLocAddrDesc;
    @BindView(R.id.tvLocTime)
    public TextView tvLocTime;
    @BindView(R.id.tvWifiSSID)
    public TextView tvWifiSSID;
    @BindView(R.id.tvWifiBSSID)
    public TextView tvWifiBSSID;
    @BindView(R.id.tvWifiIP)
    public TextView tvWifiIP;
    @BindView(R.id.tvWifiMAC)
    public TextView tvWifiMAC;
    @BindView(R.id.tvWifiRssi)
    public TextView tvWifiRssi;
    @BindView(R.id.tvWifiSpeed)
    public TextView tvWifiSpeed;


    @BindView(R.id.btn_probe_start)
    public Button btn_probe_start;


    private static int Response_OK = 0;
    private static int Locatoin_Error = 0;
    private static int PublicIp_Error = 0;
    private static int TraceRoute_Error = 0;


    private String probeTime, probeURL, probeIndex, userIMSI;

    public static final String YY_BASE_URL = "https://ipip.yy.com/",
            TaoBao_BASE_URL = "http://ip.taobao.com/service/";


    private Retrofit retrofitIP, retrofitProbe;

    private  YyIpService yyIpService;
    private TaobaoIpService taobaoIpService;
    private ProbeService probeService;


    private RxBDAbstractLocationListener locationListener;
    private LocationService locationService;


    int lac, systemid, cellId;

    WifiManager wifiManager;
    WifiInfo wifiInfo;

//    Wireless wifi;

    private static final String TAG = "FragmentProbe";

    private boolean YYIPService = true;

    private static int ttl = 1;
    private int maxTtl = 20;

    CdmaInformation cdmainfo = new CdmaInformation();

    ProbeIP probeIP;
    private BDLocation location;

    OkHttpClient probeIpHttpClient;

    TelephonyManager telephonyManager;

    public SystemUtils SysInfo;

    CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    DisposableObserver disposableObserver;

    public ConnectivityManager connectivityManager;

//    private RecyclerView listViewTraceroute;
//    private TraceListAdapter traceListAdapter;

    private List<RouterInfo> traces;

    myPhoneStateListener pslistener;

    ObservableEmitter<BDLocation> observableLocationEmitter;

    @Override
    public void onReceiveLocation(BDLocation loc) {

        Log.e(TAG, "onReceiveLocation   in " + Thread.currentThread().getName());

        locationService.setLocationServiceAviable(true); // 释放 状态

        if (observableLocationEmitter != null) {
            observableLocationEmitter.onNext(loc);
        }

        Log.e("Probe：",
                "收到Lcation:" +
                        "locTime =" + loc.getTime() +
                        "Latitude =" + Double.toString(loc.getLatitude()) +
                        "Longitude =" + Double.toString(loc.getLongitude()) +
                        "LocAddr =" + loc.getProvince() + loc.getCity() + loc.getDistrict() +
                        "AddrStr =" + loc.getAddrStr() + loc.getLocationDescribe());

    }

    ObservableEmitter<Integer> emitterLocation;

    private Observable<Integer> getLocationObservable() {

        return Observable.create(new ObservableOnSubscribe<Integer>() { // 第一步：初始化Observable
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> emitter) throws Exception {
                emitterLocation = emitter;

                Observable.create(new ObservableOnSubscribe<BDLocation>() { // 第一步：初始化Observable
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<BDLocation> emitter) throws Exception {
                        // 此处最好不要进行View 操作 ，会报错：只有主线程才能对View 操作
                        Log.e(TAG, "Baidu地图启动" + "\n");
                        observableLocationEmitter = emitter;

                        // 运行之前，需要记录一下，之前loactionService 的状态
                        locationService.setLocationServiceAviable(false); // 占用状态
                        locationListener.probeStateOn(); // 切换到 分析模式
                        locationService.start();// 定位SDK启动

                    }
                }).subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<BDLocation>() { // 第三步：订阅

                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                                Log.e(TAG, "Baidu地图启动onSubscribe" + "\n");
                            }

                            @Override
                            public void onNext(@NonNull BDLocation loc) {
                                location = loc;
                                Log.e(TAG, "Baidu地图启动onNext" + "\n");
                                onComplete();
                                tvLatitude.setText("纬度:" + Double.toString(loc.getLatitude()));
                                tvLongitude.setText("经度:" + Double.toString(loc.getLongitude()));
                                tvLocAddr.setText(loc.getProvince() + loc.getCity() + loc.getDistrict());
                                tvLocAddrDesc.setText(loc.getAddrStr() + "\n" + loc.getLocationDescribe());

                                // 定位时间与扫描时间，取  扫描时间
                                tvLocTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date().getTime()));
//                                tvLocTime.setText(loc.getTime());
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                emitterLocation.onNext(Locatoin_Error);
                                emitterLocation.onComplete();
                                Log.e(TAG, "onError : Baidu地图获取失败 : " + e.getMessage() + "\n");
                            }

                            @Override
                            public void onComplete() {
                                Log.e(TAG, "Baidu地图获取onComplete" + "\n");
                                emitterLocation.onNext(Response_OK);
                                emitterLocation.onComplete();
                            }
                        });
            }
        }).subscribeOn(Schedulers.io());

    }

    ObservableEmitter<Integer> emitterTraceRoute;

    private Observable<Integer> getTraceRouteObservable() {

        return Observable.create(new ObservableOnSubscribe<Integer>() { // 第一步：初始化Observable
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> emitter) throws Exception {
                emitterTraceRoute = emitter;

                Observable.range(1, maxTtl)
                        .concatMap(new Function<Integer, ObservableSource<RouterInfo>>() {
                            @Override
                            public ObservableSource<RouterInfo> apply(@NonNull Integer integer) throws Exception {       // 更新 TTL
                                ttl = integer;
                                Log.e(TAG, "TEST TTL:" + ttl);
                                Log.e(TAG, "路由追踪 onNext() in " + Thread.currentThread().getName());

                                return ObservablePing(ttl);
                            }
                        }).subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io()) //跑在子线程    // Be notified on the main thread
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<RouterInfo>() {
                            private Disposable mTraceRoueDisposable;

                            @Override
                            public void onSubscribe(Disposable d) {
                                Log.e(TAG, "路由追踪 subscribe");
                                mTraceRoueDisposable = d;
                            }

                            // 以返回的 ping 包 作为参数 ，进行 解析
                            @Override
                            public void onNext(RouterInfo trace) {
                                // 判定 是否到达 主机，判定条件，响应 IP 不为空 ，并且 等于 主机 IP
                                if (trace.getIp() != null && trace.getIp().equals(PingIpUtil.IpToPing)) {
                                    Log.e(TAG, "latestTrace IP:" + trace.getIp());
                                    Log.e(TAG, "dispose");
                                    onComplete();
                                    mTraceRoueDisposable.dispose();
                                    Log.e(TAG, "isDisposed : " + mTraceRoueDisposable.isDisposed());
                                }
                                // 根据 IP 解析主机 名称，如：localhost
                                try {
                                    InetAddress inetAddr = InetAddress.getByName(trace.getIp());
                                    String hostname = inetAddr.getHostName();
                                    // String canonicalHostname = inetAddr.getCanonicalHostName();
                                    trace.setHostname(hostname);
                                } catch (Exception e) {
                                }
                                traces.add(trace);
                                Log.e(TAG, " trace SIZE:" + traces.size());

//                                traceListAdapter.notifyDataSetChanged();

                            }

                            @Override
                            public void onError(Throwable e) {
                                mTraceRoueDisposable.dispose();
                                emitterTraceRoute.onNext(TraceRoute_Error);
                                emitterTraceRoute.onComplete();
                            }

                            @Override
                            public void onComplete() {
                                mTraceRoueDisposable.dispose();
                                emitterTraceRoute.onNext(Response_OK);
                                emitterTraceRoute.onComplete();
                            }
                        });
            }
        }).subscribeOn(Schedulers.io());
    }

    ObservableEmitter<Integer> emitterPublicIp;

    private Observable<Integer> getPublicIpObservable() {

        if (YYIPService) {
            return Observable.create(new ObservableOnSubscribe<Integer>() { // 第一步：初始化Observable
                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> emitter) throws Exception {
                    emitterPublicIp = emitter;

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
        } else {

            return Observable.create(new ObservableOnSubscribe<Integer>() { // 第一步：初始化Observable
                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> emitter) throws Exception {
                    emitterPublicIp = emitter;

                    retrofitIP = new Retrofit.Builder()
                            .client(new OkHttpClient
                                    .Builder()
                                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                                    .build())
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            .addConverterFactory(FastJsonConverterFactory.create())
                            .baseUrl(TaoBao_BASE_URL)
                            .build();

                    taobaoIpService = retrofitIP.create(TaobaoIpService.class);

                    taobaoIpService.getPublicIP("getIpInfo.php?ip=myip")
                            .map(new Function<HttpResult<TaobaoIp>, TaobaoIp>() {
                                @Override
                                public TaobaoIp apply(@NonNull HttpResult<TaobaoIp> dataHttpResult) throws Exception {
                                    return dataHttpResult.getData();
                                }
                            })
                            .subscribeOn(Schedulers.io())
                            .unsubscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TaobaoIp>() {
                                @Override
                                public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                                    Log.e(TAG, "获取YY IP : Subscribe At Thread:" + Thread.currentThread().getName());
                                }

                                @Override
                                public void onNext(@io.reactivex.annotations.NonNull TaobaoIp taobaoIp) {
                                    Log.e(TAG, "获取YY IP : OnNext :" + taobaoIp);
                                    publicIp = taobaoIp;
//
                                    tvPublicIP.setText(((TaobaoIp) publicIp).getIp());
                                    tvPublicISP.setText(((TaobaoIp) publicIp).getIsp());
                                    tvPublicOwnership.setText(((TaobaoIp) publicIp).getRegion() + ((TaobaoIp) publicIp).getCity());
                                    tvPublicTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));
                                }

                                @Override
                                public void onError(@io.reactivex.annotations.NonNull Throwable e) {

                                    YYIPService = true;

                                    publicIp = new TaobaoIp(); // 避免获取 query_url  报 null 错误

                                    Log.e(TAG, "获取YY IP : onError ");
//                                yyIp = new YyIp();
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



    @OnClick(R.id.probe_share)
    public void probe_share(){
        ((HomeActivity)getActivity()).ScreenShotAndShare();
    }


    private void phoneCellinfo() {

        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Have permission, do the thing!
            EasyPermissions.requestPermissions(
                    this,
                    "需要权限读取IMSI做身份验证",
                    RC_GROUP_PERM,
                    GROUP_PERM);
            getActivity().finish();
        }


        CellLocation cellLocation = telephonyManager.getCellLocation();
        if ( cellLocation instanceof GsmCellLocation){
            GsmCellLocation gsm = (GsmCellLocation) telephonyManager.getCellLocation();
            if (gsm != null) {
                lac = gsm.getLac();
                systemid = gsm.getPsc();
                cellId = gsm.getCid();
            }
        }else if (cellLocation instanceof CdmaCellLocation){

            CdmaCellLocation cdma = (CdmaCellLocation) telephonyManager.getCellLocation();
            if (cellLocation != null) {

                lac = cdma.getNetworkId();
                systemid = cdma.getSystemId();
                cellId = cdma.getBaseStationId();
        }else{
                lac = 0;
                systemid = 0;
                cellId = 0;
            }
        }

        cdmainfo.setLac(lac);
        cdmainfo.setSystemid(systemid);
        cdmainfo.setCellid(cellId);
    }

    public void onProbeProcess() {


        final Lteinfo lteinfo = new Lteinfo();

        phoneCellinfo();
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {

                super.onSignalStrengthsChanged(signalStrength);
                cdmainfo.setCdmasnr(signalStrength.getEvdoSnr());
                cdmainfo.setCdmaDbm(signalStrength.getCdmaDbm());
                cdmainfo.setCdmaEcio(signalStrength.getCdmaEcio());

                cdmainfo.setEvdosnr(signalStrength.getEvdoSnr());
                cdmainfo.setEvdoDbm(signalStrength.getEvdoDbm());
                cdmainfo.setEvdoEcio(signalStrength.getEvdoEcio());

                try {
                    Method[] methods = android.telephony.SignalStrength.class.getMethods();
                    for (Method mthd : methods) {
                        if (mthd.getName().equals("getLteSignalStrength") || mthd.getName().equals("getLteRsrp")
                                || mthd.getName().equals("getLteRsrq") || mthd.getName().equals("getLteRssnr")
                                || mthd.getName().equals("getLteCqi")) {
                            if (mthd.getName().equals("getLteSignalStrength")) {
                                lteinfo.setLteSignalStrength((Integer) mthd.invoke(signalStrength));
                            }
                            if (mthd.getName().equals("getLteRsrp")) {
                                lteinfo.setLteRsrp((Integer) mthd.invoke(signalStrength));
                            }
                            if (mthd.getName().equals("getLteRsrq")) {
                                lteinfo.setLteRsrq((Integer) mthd.invoke(signalStrength));
                            }
                            if (mthd.getName().equals("getLteRssnr")) {
                                lteinfo.setLteRssnr((Integer) mthd.invoke(signalStrength));
                            }
                            if (mthd.getName().equals("getLteCqi")) {
                                lteinfo.setLteCqi((Integer) mthd.invoke(signalStrength));
                            }
                            //                            tv_lteinfo.setText(lteinfo.lteStrength());
                        }
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                //                tv_cdmainfo.setText(cdmainfo.cdmaStrength());
                //                tv_evdoinfo.setText( cdmainfo.evdoStrength());
            }

        }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment, return inflater.inflate(R.layout.fragment_probe, container, false);
        View view = inflater.inflate(R.layout.fragment_probe, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        SharedPreferences pref = getActivity().getSharedPreferences("ProbeInfo", Context.MODE_PRIVATE);


        probeURL = pref.getString("Probe_Protocol", Probe_Protocol)
                + "://"
                + pref.getString("Probe_IP", Probe_IP)
                + ":"
                + pref.getString("Probe_Port", Probe_Port)
                + "/";

        probeIndex = pref.getString("Probe_Index", Probe_Index);

        PingIpUtil.IpToPing = pref.getString("IpToPing", PingIpUtil.IpToPing);

        SysInfo = new SystemUtils();

        this.traces = new ArrayList<RouterInfo>();

//
//        wifi = new Wireless(getActivity());

//        listViewTraceroute = getActivity().findViewById(R.id.recycleListViewTraceroute);
//
//        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
//        linearLayoutManager.setOrientation(OrientationHelper.VERTICAL);
//        listViewTraceroute.setLayoutManager(linearLayoutManager);

//        traceListAdapter = new TraceListAdapter(traces);
//        listViewTraceroute.setNestedScrollingEnabled(false);
//        listViewTraceroute.setAdapter(traceListAdapter);

        mCompositeDisposable = new CompositeDisposable();

        connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        btn_probe_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (EasyPermissions.hasPermissions(getActivity(), GROUP_PERM)) {
                    Log.e(TAG, "权限组通过:");

                    ((HomeActivity) getActivity()).getBDLocationListener().baseSiteStateOn();
                    onStartButtonClick();

                } else {
                    // Have permission, do the thing!
                    Log.e(TAG, "权限组不通过");
                    EasyPermissions.requestPermissions(
                            getActivity(),
                            "APP需要相关权限才能正常运行",
                            RC_GROUP_PERM,
                            GROUP_PERM);
                }
            }
        });
    }

    @AfterPermissionGranted(RC_GROUP_PERM)
    public void onStartButtonClick() {

        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Have permission, do the thing!
            EasyPermissions.requestPermissions(
                    this,
                    "需要权限读取IMSI做身份验证",
                    RC_GROUP_PERM,
                    Manifest.permission.READ_PHONE_STATE);
        }

        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiInfo = wifiManager.getConnectionInfo();

        telephonyManager = (TelephonyManager) getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        userIMSI = telephonyManager.getSubscriberId();

        onProbeProcess();

        LocationApplication.isWifiStateChanged = false;
        Log.e("启动前", "WIFI网络状态发生变化 : " + LocationApplication.isWifiStateChanged);
        probeTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime());

        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (wifiManager.isWifiEnabled() && activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {

            tvWifiSSID.setText(wifiInfo.getSSID());
            tvWifiBSSID.setText("BSSID :" + wifiInfo.getBSSID());
            tvWifiIP.setText("IP :" +  IntenetUtil.intToIp(wifiInfo.getIpAddress()));

            tvWifiMAC.setText("MAC:" + wifiInfo.getMacAddress());

//            tvWifiState.setText("状态 :" + wifiInfo.getSupplicantState());
            tvWifiRssi.setText(Integer.toString(wifiInfo.getRssi()) + "dBm");
            tvWifiSpeed.setText(Integer.toString(wifiInfo.getLinkSpeed()) + "M");
//            tvWifiPercent.setText(Integer.toString(WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5)) + "格");



//            try {
//                tvWifiMAC.setText("MAC :" + wifi.getMacAddress());
//            } catch (UnknownHostException | SocketException e) {
//                tvWifiMAC.setText(R.string.noWifiConnection);
//            }




        } else {
            tvWifiSSID.setText("WIFI 未连接");
            tvWifiBSSID.setText("WIFI 未连接");
            tvWifiIP.setText("WIFI 未连接");
            tvWifiMAC.setText("WIFI 未连接");
            tvWifiRssi.setText("WIFI 未连接");
            tvWifiSpeed.setText("WIFI 未连接");

            Toast.makeText(getActivity(), "分析需要在WIFI环境下使用！", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isConected(getActivity())) {
            Toast.makeText(getActivity(), "请检查网络连接！", Toast.LENGTH_SHORT).show();
            return;
        }

        traces.clear();
//        traceListAdapter.notifyDataSetChanged();
        btn_probe_start.setClickable(false);
        btn_probe_start.setText("分析中");

        tvPublicIP.setText("分析中...");
        tvPublicISP.setText("分析中...");
        tvPublicOwnership.setText("分析中..." );
        tvPublicTime.setText("分析中...");

        tvProbeIP.setText("等待中....");
        tvProbeISP.setText("等待中....");
        tvProbeOwnership.setText("等待中....");
        tvProbeTime.setText("等待中....");


        disposableObserver = new DisposableObserver<Integer>() {

            int Response_All = 0;

            @Override
            public void onNext(Integer msg_code) {
                Log.e(TAG, "从后台线程 OnNext 的数据为：" + msg_code);
                Response_All += msg_code;
            }

            @Override
            public void onComplete() {


                btn_probe_start.setClickable(true);
                btn_probe_start.setText("分析中");

                Log.e(TAG, "onComplete in " + Thread.currentThread().getName());
                String traceString = "";
                for (RouterInfo trace : traces) {
                    if (trace.isSuccessful()) {
                        traceString = traceString + trace.getIp() + ",";
                    }
                }
                Log.e(TAG, "路由IP 的数据列表为：" + "route=" + traceString);
                Log.e(TAG, "分析数据上传启动前,  检测WIFI网络状态发生变化 : " + LocationApplication.isWifiStateChanged);



                btn_probe_start.setClickable(true);
                if (Response_All > Response_OK) {
                    tvProbeIP.setText("数据异常,取消");
                    tvProbeISP.setText("数据异常,取消");
                    tvProbeOwnership.setText("数据异常,取消");
                    return;
                }


                probeIpHttpClient = new OkHttpClient.Builder()
                        .sslSocketFactory(SSLHelper.getSSLCertifcation(getActivity()))
                        .hostnameVerifier(new UnSafeHostnameVerifier())
                        .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .build();

                retrofitProbe = new Retrofit.Builder()
                        .client(probeIpHttpClient)
                        .addConverterFactory(FastJsonConverterFactory.create())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .baseUrl(probeURL)
                        .build();

                probeService = retrofitProbe.create(ProbeService.class);
                Log.e(TAG, "Probe_Index。。。 Probe_Index为：" + probeURL + Probe_Index + "?" + queryUrl());

                Log.e(TAG, "Send Before,检测WIFI网络归位为 isWifiStateChanged : " + LocationApplication.isWifiStateChanged);


                ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);


                if (!mWifi.isConnected() ||  LocationApplication.isWifiStateChanged) {
                    Log.e(TAG, "Send Before, Wifi isWifiStateChanged : ");
                    Toast.makeText(getActivity(), "WIFI连接环境发生变化，已经取消分析！", Toast.LENGTH_SHORT).show();
                    LocationApplication.isWifiStateChanged = false;
                    btn_probe_start.setText("再次分析");

                    tvProbeIP.setText("WIFI中断，取消");
                    tvProbeISP.setText("WIFI中断，取消");
                    tvProbeOwnership.setText("WIFI中断，取消");
                    tvProbeTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));
                    return;
                }

                //                                    probeURL.setText( "分析地址：" + Probe_URL +  Probe_Index);
                probeService.getProbeIP(probeIndex + "?" + queryUrl())
                        .map(new Function<HttpResult<ProbeIP>, ProbeIP>() {
                            @Override
                            public ProbeIP apply(@NonNull HttpResult<ProbeIP> dataHttpResult) throws Exception {
                                return dataHttpResult.getData();
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<ProbeIP>() {
                            @Override
                            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {




                                Log.e(TAG, "onSubscribe ,检测WIFI网络归位为 isWifiStateChanged : " + LocationApplication.isWifiStateChanged);
                                Log.e(TAG, "分析数据获取中。。。 onSubscribe 线程为：" + Thread.currentThread().getName());
                                tvProbeIP.setText("进行中....");
                                tvProbeISP.setText("进行中....");
                                tvProbeOwnership.setText("进行中....");
                                tvProbeTime.setText("进行中....");
                            }

                            @Override
                            public void onNext(@io.reactivex.annotations.NonNull ProbeIP probe) {
                                Log.e(TAG, "分析数据获取中。。。 OnNext 线程为：" + Thread.currentThread().getName());
                                probeIP = probe;

                                Log.e(TAG, "分析数据为:"
                                        + "\nIP:" + probeIP.getIp()
                                        + "\nISP:" + probeIP.getIsp()
                                        + "\n区域:" + probeIP.getRegion());

                                tvProbeIP.setText(probeIP.getIp());
                                tvProbeISP.setText(probeIP.getIsp());
                                tvProbeOwnership.setText(probeIP.getRegion());
                                tvProbeTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));
                            }

                            @Override
                            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                Log.e(TAG, "分析数据获取中。。。 onError 线程为：" + Thread.currentThread().getName());
                                Log.e(TAG, "分析数据获取中。。。 onError 线程为：" + e.getMessage());
                                tvProbeIP.setText("分析失败");
                                tvProbeISP.setText("分析失败");
                                tvProbeOwnership.setText("分析失败");
                                tvProbeTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));

                                isWifiStateChanged = false;
                                Log.e(TAG, "分析数据获取OnError,检测WIFI网络归位为 : " + LocationApplication.isWifiStateChanged);

                                btn_probe_start.setClickable(true);
                                btn_probe_start.setText("再次分析");
                            }

                            @Override
                            public void onComplete() {

                                btn_probe_start.setClickable(true);
                                btn_probe_start.setText("再次分析");
                                //                                                    queryUrl();
                                isWifiStateChanged = false;
                                Log.e(TAG, "分析数据获取onComplete,检测WIFI网络归位为 : " + LocationApplication.isWifiStateChanged);
                                Log.e(TAG, "分析数据获取中。。。 onComplete 线程为：" + Thread.currentThread().getName());
                            }
                        });
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, e.toString(), e);
            }
        };

        mCompositeDisposable.add(
                Observable.merge(
                        getLocationObservable(),
                        getTraceRouteObservable(),
                        getPublicIpObservable())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(disposableObserver)
        );


    }


    private String queryUrl() {
        StringBuilder params = new StringBuilder();

        params.append("currentnettype=" +  IntenetUtil.getNetworkState(getActivity()));
        params.append("&currentnetip=" + IntenetUtil.intToIp( wifiInfo.getIpAddress()));

        params.append(publicIp.toUrlString(probeTime));

        params.append("&probetime=" + probeTime);  //PROBETIME	测试时间	String
        params.append("&probeimsi=" + userIMSI);   // PROBEIMSI	用户手机imsi卡信息	String	460110368667064
        //  params.append("&special=" + myprobe.getSpecial());
        //lac, systemid, cellId;
        params.append("&Cdmalac=" + lac);
        //CDMALAC	CDMA本地区域代码	String
        params.append("&Cdmasystemid=" + systemid);
        //CDMALAC	CDMA本地区域代码	String
        params.append("&Cdmacellid=" + cellId);
        //CDMACELLID	基站Id	String
        params.append("&gpsdetailaddress=" + location.getAddrStr() + "," + location.getLocationDescribe());
        //GPSDETAILADDRESS	GPS详细路径信息	String
        params.append("&gpsprovince=" + location.getProvince());

        params.append("&longitude=" + location.getLongitude());
        params.append("&latitude=" + location.getLatitude());
        // GPSPROVINCE	省	String	GPS省信息
        params.append("&gpscity=" + location.getCity());

        params.append("&gpsarea=" + location.getDistrict());
        //GPSDETAILADDRESS	GPS详细路径信息	String


       params.append("&wifimac=" + wifiInfo.getMacAddress());


//        try {
//            params.append("&wifimac=" + wifi.getMacAddress());
//
//        } catch (UnknownHostException | SocketException e) {
//            params.append("&wifimac=error" );
//        }


        // WIFIMAC	手机wifi的mac	String	按照签名机制生产的验签码
        params.append("&wifissid=" + wifiInfo.getSSID());
        //WIFISSID	WIFI Ap的SSID 名称	String
        params.append("&wifibssid=" + wifiInfo.getBSSID());
        // WIFIBSSID	AP的mac地址	String
        params.append("&wifistate=" + wifiInfo.getSupplicantState());
        // WIFISTATE	AP的工作状态	String
        //params.append("&probetarget=" + java.net.URLEncoder.encode(myprobe.getTarget(), "UTF-8"));
        //PROBETARGET	分析目标	String
        params.append("&phonetype=" + SystemUtils.getSystemModel());
        // PHONETYPE	电话类型	String
        params.append("&phonefactory=" + SystemUtils.getDeviceBrand());
        //PHONEFACTORY	手机所属厂家	String
        params.append("&phonesdk=" + SystemUtils.getSystemSDK());
        //PHONESDK	sdk	String
        params.append("&phoneandroid=" + SystemUtils.getSystemVersion());

        params.append("&phoneandroid=" + SystemUtils.getSystemVersion());

        params.append("&versioncode=" + AppUtils.getVersionCode(getActivity()));
        //PHONEANDROID	Android版本	String

        String traceString = "";

        for (RouterInfo trace : traces) {
            if (trace.isSuccessful()) {
                traceString = traceString + trace.getIp() + ",";
            }
        }

        params.append("&route=" + traceString);

        Log.e(TAG, "分析URL：" + params.toString());



        return params.toString();
    }

    private synchronized io.reactivex.ObservableSource<RouterInfo> ObservablePing(int TTL) {
        PingIpUtil.ttl = TTL;
        return Observable.create(new ObservableOnSubscribe<RouterInfo>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<RouterInfo> emitter) throws Exception {
                RouterInfo trace;
                trace = PingIpUtil.launchPing(PingIpUtil.IpToPing, PingIpUtil.ttl);
                emitter.onNext(trace);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());   //跑在子线程

    }

    public static boolean isConected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        return info != null && info.isConnected() && info.getState() == NetworkInfo.State.CONNECTED;
    }

    /**
     * Check speed of the connection
     */
    public static String checkingNetworkSpeed(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI) {
            return "WIFI";
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return "1xRTT";
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return "CDMA";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return "EDGE";
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return "EVDO_0";
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return "EVDO_A";
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return "GPRS (2.5G)";
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return "HSDPA(4G)";
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return "HSPA (4G)";
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return "HSUPA (3G)";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return "UMTS (3G)";

                // API level 7 not supported this type
                case NETWORK_TYPE_EHRPD:
                    return "EHRPD";
                case NETWORK_TYPE_EVDO_B:
                    return "EVDO_B";
                case NETWORK_TYPE_HSPAP:
                    return "HSPA+ (4G)";
                case NETWORK_TYPE_IDEN:
                    return "IDEN";
                case NETWORK_TYPE_LTE:
                    return "LTE (4G)";
                // Unknown type
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    return "未知网络";
                default:
                    return "";
            }
        } else {
            return "";
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        isWifiStateChanged = false;

//        netWorkStateReceiver = new NetWorkStateReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
//        getActivity().registerReceiver(netWorkStateReceiver, filter);

        locationService = ((HomeActivity) getActivity()).getLocationService();
        locationListener = ((HomeActivity) getActivity()).getBDLocationListener();
    }

    @Override
    public void onStop() {
        // 注销

        Log.e(TAG, "onStop netWorkStateReceiver");
        //  unregisterReceiver(netWorkStateReceiver);
//
//        getActivity().unregisterReceiver(netWorkStateReceiver);

        if (telephonyManager != null) {
            telephonyManager.listen(pslistener, PhoneStateListener.LISTEN_NONE);
        }

        super.onStop();
    }

    //在onResume()方法注册
    @Override
    public void onResume() {

        Log.d(TAG, "onResume: WifiStateReceiver Register");
//        if (netWorkStateReceiver == null) {
//            netWorkStateReceiver = new NetWorkStateReceiver();
//            IntentFilter filter = new IntentFilter();
//            filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
//            getActivity().registerReceiver(netWorkStateReceiver, filter);
//        }
        super.onResume();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        gotoAppSettingAndFnish();
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    private void gotoAppSettingAndFnish() {
        Toast.makeText(getActivity(), "权限不足，无法启动,请允许相关权限,", Toast.LENGTH_LONG).show();
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        localIntent.setData(Uri.fromParts("package", getActivity().getPackageName(), null));

        startActivity(localIntent);
        getActivity().finish();
    }

}