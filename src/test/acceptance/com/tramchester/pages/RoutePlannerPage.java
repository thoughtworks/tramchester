package com.tramchester.pages;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RoutePlannerPage extends Page {
    long timeoutInSeconds = 2;

    private final Select fromStop;
    private final Select toStop;
    private final WebElement time;
    private final WebElement plan;

    public RoutePlannerPage(WebDriver driver) throws InterruptedException {
        super(driver);
        fromStop = new Select(driver.findElement(By.id("fromStop")));
        toStop = new Select(driver.findElement(By.id("toStop")));
        time = driver.findElement(By.id("hour"));
        plan = driver.findElement(By.id("plan"));
    }

    public void setFromStop(String name) {
        fromStop.selectByVisibleText(name);
    }

    public void setToStop(String name) {
        toStop.selectByVisibleText(name);
    }

    public void setTime(String value) {
        time.clear();
        time.sendKeys(value);
    }

    public void submit() {
        plan.click();
    }

    public void waitForToStops() {
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        WebElement fromStopElement = driver.findElement(By.id("fromStop"));
        wait.until(ExpectedConditions.textToBePresentInElement(fromStopElement, "Altrincham"));
    }
}
