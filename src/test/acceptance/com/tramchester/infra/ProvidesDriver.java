package com.tramchester.infra;

import com.tramchester.pages.RoutePlannerPage;
import com.tramchester.pages.WelcomePage;
import org.junit.rules.TestName;
import org.openqa.selenium.Cookie;

public interface ProvidesDriver {
    void init();

    void commonAfter(TestName testName);

    WelcomePage getWelcomePage();
    Cookie getCookieNamed(String name);
    RoutePlannerPage getRoutePlannerPage() throws InterruptedException;
}
