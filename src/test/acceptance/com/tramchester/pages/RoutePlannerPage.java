package com.tramchester.pages;


import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RoutePlannerPage extends Page {
    long timeoutInSeconds = 2;

    public RoutePlannerPage(WebDriver driver) throws InterruptedException {
        super(driver);
    }

    public void setFromStop(String name) {
        setSelectorTo("fromStop", name);
    }

    public void setToStop(String name) {
        setSelectorTo("toStop", name);
    }

    public void setTime(String value) {
        WebElement time = findElementById("hour");
        time.clear();
        time.sendKeys(value);
    }

    public RouteDetailsPage submit() {
        WebElement plan = findElementById("plan");
        plan.click();
        return new RouteDetailsPage(driver);
    }

    public void waitForToStops() {
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        WebElement fromStopElement = findElementById("fromStop");
        wait.until(ExpectedConditions.textToBePresentInElement(fromStopElement, "Altrincham"));
    }
}
