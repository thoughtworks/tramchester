package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.domain.presentation.LatLong;
import org.junit.rules.TestName;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.String.format;

public abstract class ProvidesDriver {
    private static final Logger logger = LoggerFactory.getLogger(ProvidesDriver.class);

    public abstract void init();
    public abstract AppPage getAppPage();
    public abstract Cookie getCookieNamed(String name);
    public abstract void setStubbedLocation(LatLong place) throws IOException;
    public abstract void commonAfter(TestName testName);

    protected void takeScreenShot(TestName testName) {
        TakesScreenshot driver = getDriver();
        if (driver==null) {
            return;
        }
        try {
            TakesScreenshot takesScreenshot = driver;
            byte[] bytes = takesScreenshot.getScreenshotAs(OutputType.BYTES);
            String filename = format("build/reports/tests/%s.png", safeFilename(testName));
            File target = Paths.get(filename).toAbsolutePath().toFile();
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException ioException) {
            logger.warn("Can't takle screenshoot", ioException);
        }
        catch (org.openqa.selenium.TimeoutException timeout) {
            logger.warn("Can't takle screenshoot", timeout);
        }
    }

    private String safeFilename(TestName testName) {
        String result = testName.getMethodName().
                replaceAll(":","_").
                replaceAll(" ","");
        return result;
    }

    protected abstract RemoteWebDriver getDriver();

    public void moveTo(WebElement webElement){
        new Actions(getDriver()).moveToElement(webElement).perform();
    }

    public void click(WebElement webElement) {
        webElement.click();
    }

    public abstract void quit();

    public void close() {
        RemoteWebDriver driver = getDriver();
        if (driver!=null) {
            driver.close();
        }
     }

    public abstract boolean isEnabledGeo();

    public void clearCookies() {
        getDriver().manage().deleteAllCookies();
    }

}
