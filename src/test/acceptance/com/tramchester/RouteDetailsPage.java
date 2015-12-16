package com.tramchester;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RouteDetailsPage {
    private WebDriver driver;

    public RouteDetailsPage(WebDriver driver) {
        this.driver = driver;
    }

    public String getJourneyHeading(int index) {
        return getTextFor(index, "journeyHeading");
    }


    public String getJourneyBegin(int index) {
        return getTextFor(index, "journeyBegin");
    }

    public String getJourneyEnd(int index) {
        return getTextFor(index, "journeyEnd");
    }

    public String getSummary(int index) {
        return getTextFor(index, "journeySummary");
    }

    private String getTextFor(int index, String idPrefix) {
        WebElement element = driver.findElement(By.id(idPrefix + index));
        return element.getText();
    }
}
