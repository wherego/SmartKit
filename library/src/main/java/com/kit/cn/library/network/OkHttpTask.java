package com.kit.cn.library.network;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.kit.cn.library.network.cache.CacheEntity;
import com.kit.cn.library.network.cache.CacheMode;
import com.kit.cn.library.network.cookie.CookieJarImpl;
import com.kit.cn.library.network.cookie.store.CookieStore;
import com.kit.cn.library.network.https.HttpsUtils;
import com.kit.cn.library.network.interceptor.LoggerInterceptor;
import com.kit.cn.library.network.model.HttpHeaders;
import com.kit.cn.library.network.model.HttpParams;
import com.kit.cn.library.network.request.DeleteRequest;
import com.kit.cn.library.network.request.GetRequest;
import com.kit.cn.library.network.request.HeadRequest;
import com.kit.cn.library.network.request.OptionsRequest;
import com.kit.cn.library.network.request.PostRequest;
import com.kit.cn.library.network.request.PutRequest;
import com.kit.cn.library.network.utils.OkLogger;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.Call;
import okhttp3.Interceptor;

/**
 * Created by zhouwen on 16/8/18.
 */
public class OkHttpTask {
    public static final int DEFAULT_MILLISECONDS = 60000; //默认的超时时间

    private static OkHttpTask mInstance;                 //单例
    private Handler mDelivery;                            //用于在主线程执行的调度器
    private okhttp3.OkHttpClient.Builder okHttpClientBuilder;     //ok请求的客户端
    private HttpParams mCommonParams;                     //全局公共请求参数
    private HttpHeaders mCommonHeaders;                   //全局公共请求头
    private CacheMode mCacheMode;                         //全局缓存模式
    private long mCacheTime = CacheEntity.CACHE_NEVER_EXPIRE;  //全局缓存过期时间,默认永不过期
    private static Application context;                   //全局上下文
    private CookieJarImpl cookieJar;                      //全局 Cookie 实例

    private OkHttpTask() {
        okHttpClientBuilder = new okhttp3.OkHttpClient.Builder();
        okHttpClientBuilder.hostnameVerifier(new DefaultHostnameVerifier());
        okHttpClientBuilder.connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        okHttpClientBuilder.writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        mDelivery = new Handler(Looper.getMainLooper());
    }

    public static OkHttpTask getInstance() {
        if (mInstance == null) {
            synchronized (OkHttpTask.class) {
                if (mInstance == null) {
                    mInstance = new OkHttpTask();
                }
            }
        }
        return mInstance;
    }

    /** 必须在全局Application先调用，获取context上下文，否则缓存无法使用 */
    public static void init(@NonNull Application app) {
        context = app;
    }

    /** 获取全局上下文 */
    public static Context getContext() {
        if (context == null) throw new IllegalStateException("请先在全局Application中调用 OkHttpTask.init() 初始化！");
        return context;
    }

    public Handler getDelivery() {
        return mDelivery;
    }

    public okhttp3.OkHttpClient getOkHttpClient() {
        return okHttpClientBuilder.build();
    }

    /** 对外暴露 OkHttpTask,方便自定义 */
    public okhttp3.OkHttpClient.Builder getOkHttpClientBuilder() {
        return okHttpClientBuilder;
    }

    /** get请求 */
    public static GetRequest get(String url) {
        return new GetRequest(url);
    }

    /** post请求 */
    public static PostRequest post(String url) {
        return new PostRequest(url);
    }

    /** put请求 */
    public static PutRequest put(String url) {
        return new PutRequest(url);
    }

    /** head请求 */
    public static HeadRequest head(String url) {
        return new HeadRequest(url);
    }

    /** delete请求 */
    public static DeleteRequest delete(String url) {
        return new DeleteRequest(url);
    }

    /** patch请求 */
    public static OptionsRequest options(String url) {
        return new OptionsRequest(url);
    }

    /** 调试模式,默认打开所有的异常调试 */
    public OkHttpTask debug(String tag) {
        debug(tag, true);
        return this;
    }

    /**
     * 调试模式,第二个参数表示所有catch住的log是否需要打印
     * 一般来说,这些异常是由于不标准的数据格式,或者特殊需要主动产生的,并不是框架错误,如果不想每次打印,这里可以关闭异常显示
     */
    public OkHttpTask debug(String tag, boolean isPrintException) {
        okHttpClientBuilder.addInterceptor(new LoggerInterceptor(tag, true));
        OkLogger.debug(isPrintException);
        return this;
    }

    /**
     * 此类是用于主机名验证的基接口。 在握手期间，如果 URL 的主机名和服务器的标识主机名不匹配，
     * 则验证机制可以回调此接口的实现程序来确定是否应该允许此连接。策略可以是基于证书的或依赖于其他验证方案。
     * 当验证 URL 主机名使用的默认规则失败时使用这些回调。如果主机名是可接受的，则返回 true
     */
    public class DefaultHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /** https的全局访问规则 */
    public OkHttpTask setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        okHttpClientBuilder.hostnameVerifier(hostnameVerifier);
        return this;
    }

    /** https的全局自签名证书 */
    public OkHttpTask setCertificates(InputStream... certificates) {
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(null, null, certificates);
        okHttpClientBuilder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        return this;
    }

    /** https双向认证证书 */
    public OkHttpTask setCertificates(InputStream bksFile, String password, InputStream... certificates) {
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(bksFile, password, certificates);
        okHttpClientBuilder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        return this;
    }

    /** 全局cookie存取规则 */
    public OkHttpTask setCookieStore(CookieStore cookieStore) {
        cookieJar = new CookieJarImpl(cookieStore);
        okHttpClientBuilder.cookieJar(cookieJar);
        return this;
    }

    /** 获取全局的cookie实例 */
    public CookieJarImpl getCookieJar() {
        return cookieJar;
    }

    /** 全局读取超时时间 */
    public OkHttpTask setReadTimeOut(int readTimeOut) {
        okHttpClientBuilder.readTimeout(readTimeOut, TimeUnit.MILLISECONDS);
        return this;
    }

    /** 全局写入超时时间 */
    public OkHttpTask setWriteTimeOut(int writeTimeout) {
        okHttpClientBuilder.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
        return this;
    }

    /** 全局连接超时时间 */
    public OkHttpTask setConnectTimeout(int connectTimeout) {
        okHttpClientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        return this;
    }

    /** 全局的缓存模式 */
    public OkHttpTask setCacheMode(CacheMode cacheMode) {
        mCacheMode = cacheMode;
        return this;
    }

    /** 获取全局的缓存模式 */
    public CacheMode getCacheMode() {
        return mCacheMode;
    }

    /** 全局的缓存过期时间 */
    public OkHttpTask setCacheTime(long cacheTime) {
        if (cacheTime <= -1) cacheTime = CacheEntity.CACHE_NEVER_EXPIRE;
        mCacheTime = cacheTime;
        return this;
    }

    /** 获取全局的缓存过期时间 */
    public long getCacheTime() {
        return mCacheTime;
    }

    /** 获取全局公共请求参数 */
    public HttpParams getCommonParams() {
        return mCommonParams;
    }

    /** 添加全局公共请求参数 */
    public OkHttpTask addCommonParams(HttpParams commonParams) {
        if (mCommonParams == null) mCommonParams = new HttpParams();
        mCommonParams.put(commonParams);
        return this;
    }

    /** 获取全局公共请求头 */
    public HttpHeaders getCommonHeaders() {
        return mCommonHeaders;
    }

    /** 添加全局公共请求参数 */
    public OkHttpTask addCommonHeaders(HttpHeaders commonHeaders) {
        if (mCommonHeaders == null) mCommonHeaders = new HttpHeaders();
        mCommonHeaders.put(commonHeaders);
        return this;
    }

    /** 添加全局拦截器 */
    public OkHttpTask addInterceptor(@Nullable Interceptor interceptor) {
        okHttpClientBuilder.addInterceptor(interceptor);
        return this;
    }

    /** 根据Tag取消请求 */
    public void cancelTag(Object tag) {
        for (Call call : getOkHttpClient().dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : getOkHttpClient().dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }
}