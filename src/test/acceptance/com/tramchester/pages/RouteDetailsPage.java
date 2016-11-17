package com.tramchester.pages;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RouteDetailsPage extends Page {


    public RouteDetailsPage(WebDriver driver) {
        super(driver);
    }

    public String getJourneyHeading(int index) {
        return getTextFor("journeyHeading", index);
    }

    public String getJourneyBegin(int index) {
        return getTextFor("journeyBegin", index);
    }

    public String getJourneyEnd(int index) {
        return getTextFor("journeyEnd", index);
    }

    public String getSummary(int index) {
        return getTextFor("journeySummary", index);
    }

    public boolean waitForRoutes() {
        return waitForElement("routes", timeOut).isEnabled();
    }

    public boolean journeyPresent(int index) {
        return waitForElement(formJourneyPanelId(index), timeOut).isDisplayed();
    }

    public JourneyDetailsPage getDetailsFor(int index) {
        WebElement panel = findElementById(formJourneyPanelId(index));
        panel.click();
        waitForElement("journeyHeader", timeOut);
        return new JourneyDetailsPage(driver);
    }

    private String formJourneyPanelId(int index) {
        return "journeyPanel" + Integer.toString(index);
    }

    public boolean waitForError() {
        return waitForElement("NoRoutes", 4*timeOut).isEnabled();
    }

    public String getNote(int index) {
        WebElement element = waitForElement("note" + index, timeOut);
        return element.getText();
    }

    public RoutePlannerPage planNewJourney() throws InterruptedException {
        findElementById("newJourney").click();
        return new RoutePlannerPage(driver);
    }

    public boolean notesPresent() {
        return driver.findElement(By.id("Notes")).isDisplayed();
    }
}
