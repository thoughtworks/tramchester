package com.tramchester.acceptance.pages;

import org.openqa.selenium.WebDriver;

public class MapPage extends Page {


    public MapPage(WebDriver driver) {
        super(driver);
    }

    public String getTitle() {
        super.waitForElement("title", 10);
        return super.findElementById("title").getText();
    }
}
