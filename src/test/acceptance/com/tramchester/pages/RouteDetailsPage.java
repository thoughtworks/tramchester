package com.tramchester.pages;


import org.openqa.selenium.WebDriver;

public class RouteDetailsPage extends Page {

    private long timeOut = 3;

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
        return waitForElement("routes", timeOut).isDisplayed();
    }

    public boolean journeyPresent(int index) {
        String id = "journeyPanel" + Integer.toString(index);
        return waitForElement(id, timeOut).isDisplayed();
    }
}
