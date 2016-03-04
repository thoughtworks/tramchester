package com.tramchester.pages;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WelcomePage extends Page {
    long timeOutSeconds = 2;

    public WelcomePage(WebDriver driver) {
        super(driver);
    }

    public void load(String url) {
        driver.get(url);
    }

    public void begin() {
        WebElement beginLink = waitForElement("plan", timeOutSeconds);
        beginLink.click();
    }
}
