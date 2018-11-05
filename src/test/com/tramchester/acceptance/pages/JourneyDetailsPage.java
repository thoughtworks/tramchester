package com.tramchester.acceptance.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalTime;

public class JourneyDetailsPage extends Page {

    private ProvidesDateInput providesDateInput;

    public JourneyDetailsPage(WebDriver driver, ProvidesDateInput providesDateInput) {
        super(driver);
        this.providesDateInput = providesDateInput;
    }

    public String getSummary() {
        return findElementById("journeyHeader").getText();
    }

    public String getPrompt(int index) {
        return getTextFor("stagePrompt", index);
    }

    public String getInstruction(int index) {
        return getTextFor("stageInstruction", index);
    }

    public String getDuration(int index) {
        return getTextFor("stageDuration", index);
    }

    public String getArrive(int index) {
        return getTextFor("stageArrive", index);
    }

    public String getChange(int index) {
        return getTextFor("change", index);
    }

    public RoutePlannerPage planNewJourney() {
        findElementById("newJourney").click();
        return new RoutePlannerPage(driver, providesDateInput);
    }

    public RouteDetailsPage backToRouteDetails() {
        findElementById("backToRouteDetails").click();
        return new RouteDetailsPage(driver, providesDateInput);
    }

    public boolean laterTramEnabled() {
        return getLaterTram().isEnabled();
    }

    public  boolean earlierTramEnabled() {

        return getEarlierTram().isEnabled();
    }

    public void laterTram() {
        getLaterTram().click();
        waitForPage();
    }

    private JourneyDetailsPage waitForPage() {
        waitForElement("journeyHeader", timeOut);
        return new JourneyDetailsPage(driver, providesDateInput);
    }

    public void earlierTram() {
        getEarlierTram().click();
        waitForPage();
    }

    private WebElement getLaterTram() {
        return findElementById("laterTram");
    }

    private WebElement getEarlierTram() {
        return findElementById("earlierTram");
    }

    public LocalTime getTime() {
        String[] parts = getSummary().split(" ");
        return LocalTime.parse(parts[0]);
    }
}
