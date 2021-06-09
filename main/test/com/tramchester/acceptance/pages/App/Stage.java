package com.tramchester.acceptance.pages.App;

import com.tramchester.domain.time.TramTime;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class Stage {
    private final WebElement row;

    public Stage(WebElement row) {

        this.row = row;
    }

    public TramTime getDepartTime() {
        String fieldText = getFieldText("departTime");
        fieldText = fieldText.replace(" +1d","+24").trim();
        return TramTime.parse(fieldText).get();
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
