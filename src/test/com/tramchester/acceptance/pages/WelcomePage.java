package com.tramchester.acceptance.pages;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class WelcomePage extends Page {
    public static final String ELEMENT_ID = "planJourney";
    long timeOutSeconds = 10; // initial page load is slow on snap ci

    public WelcomePage(WebDriver driver) {
        super(driver);
    }

    public WelcomePage load(String url) {
        driver.navigate().to(url);

        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(presenceOfElementLocated(By.id("welcome")));

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
