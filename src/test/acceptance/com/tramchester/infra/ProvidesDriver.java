package com.tramchester.infra;

import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static java.lang.String.format;

public abstract class ProvidesDriver {
    public abstract void init();
    public abstract WelcomePage getWelcomePage();
    public abstract Cookie getCookieNamed(String name);
    public abstract RoutePlannerPage getRoutePlannerPage() throws InterruptedException;
    public abstract void setProfileForGeoFile(Path fullPath);
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

    protected abstract TakesScreenshot getDriver();

}
