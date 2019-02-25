package com.tramchester.acceptance.pages;

import org.openqa.selenium.WebDriver;

public class TramsNearMePage extends Page {

    public TramsNearMePage(WebDriver driver) {
        super(driver);
    }

    public boolean waitForStations() {
        return waitForElement("stationsList", 2*timeOut).isDisplayed();
    }

    public boolean showingDeparturesText() {
        return waitForElement("departuresText", timeOut).isDisplayed();
    }

    public void switchView() {
        super.waitForElement("switchView", timeOut).click();
    }

    public boolean waitForDepartures() {
        return waitForElement("departuresList", 2*timeOut).isDisplayed();
    }

    public boolean showingStationsText() {
        return waitForElement("stationsText", 2*timeOut).isDisplayed();
    }
}
