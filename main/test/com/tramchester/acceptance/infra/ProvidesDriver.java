package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.domain.presentation.LatLong;
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
    protected abstract String getDriverName();

    protected void takeScreenShot(String testName) {
        TakesScreenshot driver = getDriver();
        if (driver==null) {
            return;
        }
        try {
            byte[] bytes = driver.getScreenshotAs(OutputType.BYTES);
            String filename = format("build/reports/tests/%s.png", safeFilename(testName));
            File target = Paths.get(filename).toAbsolutePath().toFile();
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException | TimeoutException ioException) {
            logger.warn("Can't takle screenshoot", ioException);
        }
    }

    private String safeFilename(String testName) {
        int endOfTestName = testName.indexOf("(");
        return testName.substring(0, endOfTestName) + "_" + getDriverName();
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


    public void clearCookies() {
        getDriver().manage().deleteAllCookies();
    }

}
