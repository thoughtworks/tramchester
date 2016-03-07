package com.tramchester.pages;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

public class RoutePlannerPage extends Page {
    long timeoutInSeconds = 5;
    private String toStop = "toStop";
    private String fromStop = "fromStop";

    public RoutePlannerPage(WebDriver driver) throws InterruptedException {
        super(driver);
    }

    public void setFromStop(String name) {
        setSelectorTo(fromStop, name);
    }

    public void setToStop(String name) {
        setSelectorTo(toStop, name);
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
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(fromStop)));
        WebElement fromStopElement = findElementById(fromStop);
        wait.until(ExpectedConditions.textToBePresentInElement(fromStopElement, "Altrincham"));
    }

    public List<String> getToStops() {
        WebElement list = findElementById(toStop);
        Select select = new Select(list);
        List<WebElement> options = select.getOptions();
        return options.stream().map(option -> option.getText()).collect(Collectors.toList());
    }
}
