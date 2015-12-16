package com.tramchester;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class RoutePlannerPage {
    private final Select fromStop;
    private final Select toStop;
    private final WebElement time;
    private final WebElement plan;

    public RoutePlannerPage(WebDriver driver) throws InterruptedException {
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
}
