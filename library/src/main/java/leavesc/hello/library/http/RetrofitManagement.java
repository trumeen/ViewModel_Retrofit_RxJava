package leavesc.hello.library.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import leavesc.hello.library.holder.ContextHolder;
import leavesc.hello.library.http.config.HttpCode;
import leavesc.hello.library.http.exception.AccountInvalidException;
import leavesc.hello.library.http.exception.ServerResultException;
import leavesc.hello.library.http.exception.TokenInvalidException;
import leavesc.hello.library.http.interceptor.HeaderInterceptor;
import leavesc.hello.library.http.interceptor.HttpInterceptor;
import leavesc.hello.library.http.model.BaseResponseBody;
import leavesc.hello.monitor.MonitorInterceptor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 作者：leavesC
 * 时间：2018/10/26 23:11
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
public enum RetrofitManagement {

    INSTANCE;

    public static RetrofitManagement getInstance() {
        return INSTANCE;
    }

    private static final long READ_TIMEOUT = 6000;

    private static final long WRITE_TIMEOUT = 6000;

    private static final long CONNECT_TIMEOUT = 6000;

    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    private List<Interceptor> interceptorList;

    private boolean log = true;

    private Retrofit createRetrofit(String url) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .addInterceptor(new HttpInterceptor())
                .addInterceptor(new HeaderInterceptor())
                .retryOnConnectionFailure(true);
        if (interceptorList != null) {
            for (Interceptor interceptor : interceptorList) {
                builder.addInterceptor(interceptor);
            }
        }
        if (log) {
            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(httpLoggingInterceptor);
            //这是我的另一个开源库：https://github.com/leavesC/Monitor
            builder.addInterceptor(new MonitorInterceptor(ContextHolder.getContext()));
        }
        OkHttpClient client = builder.build();
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    <T> ObservableTransformer<BaseResponseBody<T>, T> applySchedulers() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(result -> {
                    switch (result.getCode()) {
                        case HttpCode.CODE_SUCCESS: {
                            return createData(result.getData());
                        }
                        case HttpCode.CODE_TOKEN_INVALID: {
                            throw new TokenInvalidException();
                        }
                        case HttpCode.CODE_ACCOUNT_INVALID: {
                            throw new AccountInvalidException();
                        }
                        default: {
                            throw new ServerResultException(result.getCode(), result.getMsg());
                        }
                    }
                });
    }

    private <T> Observable<T> createData(T t) {
        return Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(ObservableEmitter<T> emitter) {
                try {
                    emitter.onNext(t);
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
    }

    <T> T getService(Class<T> clz, String host) {
        T value;
        if (serviceMap.containsKey(host)) {
            Object obj = serviceMap.get(host);
            if (obj == null) {
                value = createRetrofit(host).create(clz);
                serviceMap.put(host, value);
            } else {
                value = (T) obj;
            }
        } else {
            value = createRetrofit(host).create(clz);
            serviceMap.put(host, value);
        }
        return value;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    public void addInterceptor(List<Interceptor> interceptorList) {
        if (this.interceptorList == null) {
            this.interceptorList = new ArrayList<>();
        }
        if (interceptorList != null && interceptorList.size() > 0) {
            this.interceptorList.addAll(interceptorList);
        }
    }

}