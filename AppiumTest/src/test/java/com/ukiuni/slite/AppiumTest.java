package com.ukiuni.slite;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;

/**
 * Created by tito on 15/10/10.
 */
public class AppiumTest {
    AndroidDriver driver;

    @Before
    public void initAppium() throws MalformedURLException {

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.BROWSER_NAME, "");
        capabilities.setCapability(MobileCapabilityType.APP_PACKAGE, "com.ukiuni.slite");
        capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, "com.ukiuni.slite.MainActivity");
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android Emulator");
        capabilities.setCapability("unicodeKeyboard", true);
        capabilities.setCapability("resetKeyboard", true);
        driver = new AndroidDriver<>(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
        driver.manage().timeouts().implicitlyWait(80, TimeUnit.SECONDS);
    }

    @Test
    public void testSignin() {
        if(false)if (null != driver.findElementById("signoutButton")) {
            driver.findElementById("signoutButton").click();
        }
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mail")));
        driver.findElementById("mail").clear();
        driver.findElementById("mail").sendKeys("peek@ukiuni.com");
        driver.findElementById("password").clear();
        driver.findElementById("password").sendKeys("peek");
        driver.findElementById("signinButton").click();
    }
}
