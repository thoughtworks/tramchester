package com.tramchester.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Page {
    protected WebDriver driver;

    public Page(WebDriver driver) {
        this.driver = driver;
    }

    protected String getTextFor(String idPrefix, int index) {
        WebElement element = driver.findElement(By.id(idPrefix + index));
        return element.getText();
    }

    protected WebElement waitForElement(String elementId, long timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(elementId)));
    }

    protected void setSelectorTo(String selector, String name) {
        Select fromStop = new Select(driver.findElement(By.id(selector)));
        fromStop.selectByVisibleText(name);
    }


    public WebElement findElementById(String id) {
        return driver.findElement(By.id(id));
    }
}
