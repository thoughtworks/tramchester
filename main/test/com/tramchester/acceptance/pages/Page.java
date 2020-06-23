package com.tramchester.acceptance.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Page {
    protected WebDriver driver;
    private final long timeOut = 4;

    public Page(WebDriver driver) {
        this.driver = driver;
    }

    protected WebElement waitForElement(String elementId, long timeoutInSeconds) {
        return waitForElement(By.id(elementId), timeoutInSeconds);
    }

    protected WebElement waitForElement(By select, long timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        wait.until(webDriver ->  driver.findElement(select));
        return driver.findElement(select);
    }

    protected WebElement findElementById(String id) {
        return waitForElement(id, timeOut);
    }

    public String getExpectedBuildNumberFromEnv() {
        // prefer release number if set
        String releaseNumber = System.getenv("RELEASE_NUMBER");
        if (releaseNumber!=null) {
            return releaseNumber;
        }
        String build = System.getenv("CIRCLE_BUILD_NUM");
        if (build!=null) {
            return build;
        }
        // 0 for dev machines
        return "0";
    }

}
