package com.tramchester.acceptance.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Page {
    protected WebDriver driver;
    protected long timeOut = 4;

    public Page(WebDriver driver) {
        this.driver = driver;
    }

    protected String getTextFor(String idPrefix, int index) {
        WebElement element = driver.findElement(By.id(idPrefix + index));
        return element.getText();
    }

    protected WebElement waitForElement(String elementId, long timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.id(elementId)));
    }

    protected void setSelectorTo(String id, String name) {
        Select selector = new Select(driver.findElement(By.id(id)));
        selector.selectByVisibleText(name);
    }

    public WebElement findElementById(String id) {
        waitForElement(id, timeOut);
        return driver.findElement(By.id(id));
    }

    public List<String> getAllNotes() {
        WebElement listElement;
        try {
            listElement = waitForElement("NotesList", timeOut);
            waitForElement("NoteItem", timeOut);
        }
        catch (TimeoutException timedOut) {
            // legit, may not be any notes.....
            return new ArrayList<>();
        }
        List<WebElement> listItems = listElement.findElements(By.id("NoteItem"));
        return listItems.stream().map(WebElement::getText).collect(Collectors.toList());
    }

}
