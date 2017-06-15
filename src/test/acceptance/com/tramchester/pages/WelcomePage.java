package com.tramchester.pages;


import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WelcomePage extends Page {
    public static final String ELEMENT_ID = "planJourney";
    long timeOutSeconds = 10; // initial page load is slow on snap ci

    public WelcomePage(WebDriver driver) {
        super(driver);
    }

    public WelcomePage load(String url) {
        driver.navigate().to(url);
        //driver.get(url);
        return this;
    }

    public RoutePlannerPage begin() throws InterruptedException {
        WebElement beginLink = waitForElement(ELEMENT_ID, timeOutSeconds);
        beginLink.click();
        return new RoutePlannerPage(driver);
    }

    public boolean hasBeginLink() {
        WebElement element = waitForElement(ELEMENT_ID, timeOutSeconds);
        return element.isDisplayed();
    }
}
