package com.nfcstudio.app.ui;
import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.nfcstudio.app.MainActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
@RunWith(AndroidJUnit4.class)
public class AutomationJourneyTest {
    @Before public void clearState(){
        Context context=ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("studio",Context.MODE_PRIVATE).edit().clear().commit();
    }
    @Test public void createPersistAndRunAutomation(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("Create automation")).perform(scrollTo(),click());
            onView(withHint("Profile name")).perform(scrollTo(),replaceText("E2E Profile"),closeSoftKeyboard());
            onView(withClassName(endsWith("Spinner"))).perform(scrollTo(),click());
            onData(is("Show message")).perform(click());
            onView(withHint("Message")).perform(scrollTo(),replaceText("Hello from E2E"),closeSoftKeyboard());
            onView(withText("Save automation")).perform(scrollTo(),click());
            onView(withText("E2E Profile")).check(matches(isDisplayed()));
        }
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("Saved automations (1)")).perform(scrollTo(),click());
            onView(withText("E2E Profile • 1 actions")).perform(scrollTo(),click());
            onView(withText("Run now")).perform(scrollTo(),click());
            onView(withText("Back")).perform(scrollTo(),click());
            onView(withText("Home")).perform(scrollTo(),click());
            onView(withText("Scan / execution history")).perform(scrollTo(),click());
            onView(withText(containsString("Started E2E Profile"))).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void scanCancellationReturnsHome(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("Read / inspect a tag")).perform(scrollTo(),click());
            onView(withText("Ready to scan")).check(matches(isDisplayed()));
            onView(withText("Cancel / Home")).perform(scrollTo(),click());
            onView(withText("NFC Studio")).check(matches(isDisplayed()));
        }
    }
}
