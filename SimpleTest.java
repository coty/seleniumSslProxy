import com.charleskorn.okhttp.systemkeystore.ExtensionsKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SimpleTest {
    static TrustManager[] trustAllCerts = new X509TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
    };

    public static void main(String[] args) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        Boolean allowUntrusted = true;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ExtensionsKt.useOperatingSystemCertificateTrustStore(builder);

        if (allowUntrusted) {

            File clientSslCertificate = new File("/Users/coty/Code/katalon/demo/nodejs-ssl-trusted-peer-example/certs/client/my-app-client.p12");
            String certificatePassword = "secret";

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(clientSslCertificate), certificatePassword.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, certificatePassword.toCharArray());
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]); // for the non-deprecated version, a truststore must be used as a second parameter

            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    System.out.println("Trust Host :" + hostname);
                    return true;
                }
            };
            builder.hostnameVerifier(hostnameVerifier);
        }

        OkHttpClient client = builder.build();

        String url = "https://local.foobar3000.com:8043";
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
