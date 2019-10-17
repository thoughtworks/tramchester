package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.domain.presentation.LatLong;
import org.junit.rules.TestName;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static java.lang.String.format;

public abstract class ProvidesDriver {
    public abstract void init();
    public abstract WelcomePage getWelcomePage();
    public abstract AppPage getAppPage();
    public abstract Cookie getCookieNamed(String name);
    public abstract RoutePlannerPage getRoutePlannerPage();
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
            File target = new File(format("build/reports/tests/%s.png", testName.getMethodName()));
            FileOutputStream output = new FileOutputStream(target);
            output.write(bytes);
            output.close();
        } catch (IOException e) {
            // unable to take screenshot
        }
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
        getDriver().close(); }

    public abstract boolean isEnabledGeo();

    public void clearCookies() {
        getDriver().manage().deleteAllCookies();
    }
}
