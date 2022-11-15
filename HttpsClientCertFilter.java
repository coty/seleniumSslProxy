import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.filters.RequestFilter;
import io.netty.handler.codec.http.*;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.charleskorn.okhttp.systemkeystore.ExtensionsKt;

import org.openqa.selenium.Proxy;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.stream.Stream;

import java.security.cert.*;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpsClientCertFilter implements RequestFilter {
    TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }
    };

    @Override
    public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents, HttpMessageInfo messageInfo) {

        File clientSslCertificate = new File("/Users/coty/Code/katalon/demo/nodejs-ssl-trusted-peer-example/certs/client/my-app-client.p12");
        String certificatePassword = "secret";

        String uri = request.getUri();
        String url = "https://" + uri;
//        String url = uri.replace("http://", "https://");
        if (Stream.of("accounts.google.com", "gstatic.com").anyMatch(url::contains)
                || !url.startsWith("https://")) {
            return null; // do not intercept driver-specific and non-https requests
        }
        SSLContext sslContext = createSslContext(clientSslCertificate, certificatePassword);
        Response intermediateResponse = doHttpsRequest(sslContext, url, request.getMethod(), contents.getContentType(), contents.getBinaryContents());
        return convertOkhttpResponseToNettyResponse(intermediateResponse);
    }

    private Response doHttpsRequest(SSLContext sslContext, String url, HttpMethod httpMethod, String mediaType, byte[] body) {
        RequestBody requestBody = null;
        if (httpMethod != HttpMethod.GET) {
            // might need to prohibit body for other methods too
            requestBody = RequestBody.create(MediaType.get(mediaType), (byte[])body);
        }

        Request request = new Request.Builder()
                .url(url)
                .method(httpMethod.name(), requestBody)
                .build();


        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        ExtensionsKt.useOperatingSystemCertificateTrustStore(builder)
        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) TRUST_ALL_CERTS); // for the non-deprecated version, a truststore must be used as a second parameter
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
//                Log.d(TAG, "Trust Host :" + hostname);
                return true;
            }
        };
        builder.hostnameVerifier(hostnameVerifier);
        OkHttpClient client = builder.build();

        try {
            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SSLContext createSslContext(File clientSslCertificate, String certificatePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(clientSslCertificate), certificatePassword.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, certificatePassword.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);

            return sslContext;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    private FullHttpResponse convertOkhttpResponseToNettyResponse(Response okhttpResponse) {
        HttpResponseStatus httpResponseStatus = HttpResponseStatus.valueOf(okhttpResponse.code());
        HttpVersion httpVersion = HttpVersion.valueOf(okhttpResponse.protocol().toString());

        ByteBuf content = null;
        try {
            ResponseBody body = okhttpResponse.body();
            content = Unpooled.wrappedBuffer(body.bytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(httpVersion, httpResponseStatus, content);
        okhttpResponse.headers().toMultimap().forEach((key, values) -> {
                nettyResponse.headers().remove(key);
        nettyResponse.headers().add(key, String.join(",", values));
        });

        return nettyResponse;
    }
}
