import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import okhttp3.*;
import org.openqa.selenium.Proxy;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.stream.Stream;

/**
 * This proxy intercepts https requests and adds certificate based client authentication.
 * It was only tested chromeDriver. For other drivers, minor adjustments in the filter may be necessary.
 */
public class SeleniumSslProxy extends Proxy {

    private BrowserMobProxy browserMobProxy;

    public SeleniumSslProxy(File clientSslCertificate, String certificatePassword) {
        BrowserMobProxy browserMobProxy = new BrowserMobProxyServer();

//        browserMobProxy.addRequestFilter(new HttpsClientCertFilter());

        browserMobProxy.addRequestFilter((request, contents, messageInfo) -> {
            System.out.println("In request filter...");
//            String url = request.getUri().replace("http://", "https://");
//            if (Stream.of("accounts.google.com", "gstatic.com").anyMatch(url::contains)
//                    || !url.startsWith("https://")) {
//                return null; // do not intercept driver-specific and non-https requests
//            }
//            SSLContext sslContext = createSslContext(clientSslCertificate, certificatePassword);
//            Response intermediateResponse = doHttpsRequest(sslContext, url, request.getMethod(), contents.getContentType(), contents.getBinaryContents());
//            return convertOkhttpResponseToNettyResponse(intermediateResponse);
            return null;
        });

        browserMobProxy.getHostNameResolver().remapHost("local.foobar3000.com", "127.0.0.1");

        this.browserMobProxy = browserMobProxy;
        this.setProxyType(Proxy.ProxyType.MANUAL);
    }

    public void start() {
        this.browserMobProxy.start();
        InetSocketAddress connectableAddressAndPort = new InetSocketAddress(ClientUtil.getConnectableAddress(), browserMobProxy.getPort());
        String proxyStr = String.format("%s:%d", connectableAddressAndPort.getHostString(), connectableAddressAndPort.getPort());
        this.setHttpProxy(proxyStr);
        this.setSslProxy(proxyStr);
    }

    public void stop() {
        this.browserMobProxy.stop();
    }
}
