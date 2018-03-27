package com.tramchester.acceptance.pages;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class WelcomePage extends Page {
    public static final String ELEMENT_ID = "planJourney";
    long timeOutSeconds = 15; // initial page load is slow on ci
    private ProvidesDateInput providesDateInput;

    public WelcomePage(WebDriver driver, ProvidesDateInput providesDateInput) {
        super(driver);
        this.providesDateInput = providesDateInput;
    }

    public WelcomePage load(String url) {
        driver.navigate().to(url);

        WebDriverWait wait = new WebDriverWait(driver, timeOutSeconds);
        wait.until(presenceOfElementLocated(By.id("welcome")));

        return this;
    }

    public RoutePlannerPage begin() {
        WebElement beginLink = waitForElement(ELEMENT_ID, timeOutSeconds);
        beginLink.click();
        return new RoutePlannerPage(driver, providesDateInput);
    }

    public boolean hasBeginLink() {
        WebElement element = waitForElement(ELEMENT_ID, timeOutSeconds);
        return element.isDisplayed();
    }
}
