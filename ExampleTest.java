import io.netty.handler.codec.http.HttpMethod;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.littleshoot.proxy.HttpFiltersSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.CapabilityType;

import io.github.bonigarcia.wdm.WebDriverManager;

import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.proxy.dns.AdvancedHostResolver;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.TestCase.assertEquals;

public class ExampleTest {

    private WebDriver webDriver;
    private BrowserMobProxy proxy;
    @Before
    public void setup() throws InterruptedException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        String clientCertPath = "/Users/coty/Code/katalon/demo/nodejs-ssl-trusted-peer-example/certs/client/my-app-client.p12";
        String certificatePassword = "secret";
        String hostname = "local.foobar3000.com";
        this.proxy = new SslBrowserMobProxyServer(
                clientCertPath,
                certificatePassword,
                hostname
        );
        this.proxy.setTrustAllServers(true);
        this.proxy.start(0);
        AdvancedHostResolver advancedHostResolver = proxy.getHostNameResolver();
        advancedHostResolver.remapHost("local.foobar3000.com", "127.0.0.1");
        advancedHostResolver.remapHost("google.com", "142.251.32.174");
        proxy.setHostNameResolver(advancedHostResolver);

        proxy.addRequestFilter((request, contents, messageInfo) -> {
            return null;
        });

        this.webDriver = setupEdgeDriver(this.proxy, false);
    }

    @Test
    public void pageTitleIsFoo() {
        // given
        String url = "https://local.foobar3000.com:8043"; // NOTE: do not use https here!

        // when
        this.webDriver.get(url);
        this.webDriver.manage().timeouts().implicitlyWait(5, SECONDS);

        // then
        WebElement title = this.webDriver.findElement(By.tagName("pre"));
        assertEquals("Hello, client.example.net!", title.getText());
    }

    @After
    public void teardown() {
        this.webDriver.quit();
        this.proxy.stop();
    }

    private WebDriver setupChromeDriver(BrowserMobProxy proxy, boolean headless) {
        WebDriverManager.edgedriver().setup();

        Proxy seleniumProxy = null;

        if (proxy != null) {
            seleniumProxy = ClientUtil.createSeleniumProxy((BrowserMobProxy) proxy);
        }

//        DesiredCapabilities capabilities = new DesiredCapabilities();
//        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

        EdgeOptions options = new EdgeOptions();
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        List<String> args = Arrays.asList("use-fake-ui-for-media-stream", "use-fake-device-for-media-stream");
        Map<String, Object> map = new HashMap<>();
        map.put("args", args);
        options.setCapability("ms:edgeOptions", map);
//        options.setAcceptInsecureCerts(true);
        options.setProxy(seleniumProxy);


//        capabilities.setCapability(ChromeOptions.CAPABILITY, options);

        WebDriver driver = new EdgeDriver(options);

        return driver;
    }

    private WebDriver setupEdgeDriver(BrowserMobProxy proxy, boolean headless) {
        WebDriverManager.edgedriver().setup();

        Proxy seleniumProxy = null;

        if (proxy != null) {
            seleniumProxy = ClientUtil.createSeleniumProxy((BrowserMobProxy) proxy);
        }

//        DesiredCapabilities capabilities = new DesiredCapabilities();
//        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

        EdgeOptions options = new EdgeOptions();
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        List<String> args = Arrays.asList("use-fake-ui-for-media-stream", "use-fake-device-for-media-stream");
        Map<String, Object> map = new HashMap<>();
        map.put("args", args);
        options.setCapability("ms:edgeOptions", map);
//        options.setAcceptInsecureCerts(true);
        options.setProxy(seleniumProxy);


//        capabilities.setCapability(ChromeOptions.CAPABILITY, options);

        WebDriver driver = new EdgeDriver(options);

        return driver;
    }

}
