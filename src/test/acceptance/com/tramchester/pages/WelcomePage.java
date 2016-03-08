package com.tramchester.pages;


import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WelcomePage extends Page {
    long timeOutSeconds = 10; // initial page load is slow on snap ci

    public WelcomePage(WebDriver driver) {
        super(driver);
    }

    public void load(String url) {
        driver.get(url);
    }

    public RoutePlannerPage begin() throws InterruptedException {
        WebElement beginLink = waitForElement("plan", timeOutSeconds);
        beginLink.click();
        return new RoutePlannerPage(driver);
    }
}
