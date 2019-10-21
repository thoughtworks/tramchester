package com.tramchester.acceptance.pages.App;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.LocalTime;

public class Stage {
    private final WebElement row;

    public Stage(WebElement row) {

        this.row = row;
    }

    public LocalTime getDepartTime() {
        return LocalTime.parse(getFieldText("departTime"));
    }

    public String getAction() {
        return getFieldText("action");
    }

    public String getActionStation() {
        return getFieldText("actionStation");
    }

    public int getPlatform() {
        String platform = getFieldText("platform");
        if (platform.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(platform);
    }

    public String getHeadsign() {
        return getFieldText("headsign");
    }

    public String getLine(String lineClassName) {
        return getFieldText(lineClassName);
    }

    public int getPassedStops() {
        return Integer.parseInt(getFieldText("passedStops"));
    }

    private String getFieldText(String fieldName) {
        return row.findElement(By.className(fieldName)).getText();
    }
}
