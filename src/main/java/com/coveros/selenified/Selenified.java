/*
 * Copyright 2017 Coveros, Inc.
 * 
 * This file is part of Selenified.
 * 
 * Selenified is licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on 
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations 
 * under the License.
 */

package com.coveros.selenified;

import com.coveros.selenified.Browser.BrowserName;
import com.coveros.selenified.OutputFile.Result;
import com.coveros.selenified.application.App;
import com.coveros.selenified.exceptions.InvalidBrowserException;
import com.coveros.selenified.services.Call;
import com.coveros.selenified.services.HTTP;
import com.coveros.selenified.utilities.Sauce;
import com.coveros.selenified.utilities.TestSetup;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Level;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Selenified contains all of the elements to setup the test suite, and to start
 * and finish your tests. The site under test should either be set in
 * the @BeforeClass, as there is no default site set for testing. If not, this
 * site can be passed in as a system parameter. Before each suite is run, the
 * system variables are gathered, to set the browser, test site, proxy, hub,
 * etc. This class should be extended by each test class to allow for simple
 * execution of tests.
 * <p>
 * By default each test run will launch a selenium browser, and open the defined
 * test site. If no browser is needed for the test, override the startTest
 * method. Similarly, if you don't want a URL to initially load, override the
 * startTest method.
 *
 * @author Max Saperstone
 * @version 3.0.0
 * @lastupdate 8/13/2017
 */
@Listeners({com.coveros.selenified.utilities.Listener.class, com.coveros.selenified.utilities.Transformer.class})
public class Selenified {

    private static final Logger log = Logger.getLogger(Selenified.class);

    // if were services calls are bring used, are there usernames and passwords
    // used for credentials
    protected static String servicesUser = "";
    protected static String servicesPass = "";
    protected static Map<String, String> extraHeaders = new HashMap<>();

    // any additional browser capabilities that might be necessary
    protected static DesiredCapabilities extraCapabilities = null;

    // some passed in system params
    private static List<Browser> browsers;
    protected static final List<DesiredCapabilities> capabilities = new ArrayList<>();

    // for individual tests
    protected final ThreadLocal<Browser> browser = new ThreadLocal<>();
    private final ThreadLocal<DesiredCapabilities> capability = new ThreadLocal<>();
    private final ThreadLocal<OutputFile> files = new ThreadLocal<>();
    protected final ThreadLocal<App> apps = new ThreadLocal<>();
    protected final ThreadLocal<Call> calls = new ThreadLocal<>();

    // constants
    private static final String APP_INPUT = "appURL";
    private static final String BROWSER_INPUT = "browser";
    private static final String INVOCATION_COUNT = "InvocationCount";
    private static final String ERRORS_CHECK = " errors";

    /**
     * Obtains the application under test, as a URL. If the site was provided as
     * a system property, that value will override whatever was set in the
     * particular test suite. If no site was set, null will be returned, which
     * will causes the tests to error out
     *
     * @param clazz   - the test suite class, used for making threadsafe storage of
     *                application, allowing suites to have independent applications
     *                under test, run at the same time
     * @param context - the TestNG context associated with the test suite, used for
     *                storing app url information
     * @return String: the URL of the application under test
     */
    protected String getTestSite(String clazz, ITestContext context) {
        if (System.getProperty(APP_INPUT) == null) {
            return (String) context.getAttribute(clazz + APP_INPUT);
        } else {
            return System.getProperty(APP_INPUT);
        }
    }

    /**
     * Sets the URL of the application under test. If the site was provided as a
     * system property, this method ignores the passed in value, and uses the
     * system property.
     *
     * @param clazz   - the test suite class, used for making threadsafe storage of
     *                application, allowing suites to have independent applications
     *                under test, run at the same time
     * @param context - the TestNG context associated with the test suite, used for
     *                storing app url information
     * @param siteURL - the URL of the application under test
     */
    protected static void setTestSite(Selenified clazz, ITestContext context, String siteURL) {
        if (System.getProperty(APP_INPUT) == null) {
            String testSuite = clazz.getClass().getName();
            context.setAttribute(testSuite + APP_INPUT, siteURL);
        }
    }

    /**
     * Obtains the version of the current test suite being executed. If no
     * version was set, null will be returned
     *
     * @param clazz   - the test suite class, used for making threadsafe storage of
     *                application, allowing suites to have independent applications
     *                under test, run at the same time
     * @param context - the TestNG context associated with the test suite, used for
     *                storing app url information
     * @return String: the version of the current test being executed
     */
    protected String getVersion(String clazz, ITestContext context) {
        return (String) context.getAttribute(clazz + "Version");
    }

    /**
     * Sets the version of the current test suite being executed.
     *
     * @param clazz   - the test suite class, used for making threadsafe storage of
     *                application, allowing suites to have independent applications
     *                under test, run at the same time
     * @param context - the TestNG context associated with the test suite, used for
     *                storing app url information
     * @param version - the version of the test suite
     */
    protected static void setVersion(Selenified clazz, ITestContext context, String version) {
        String testSuite = clazz.getClass().getName();
        context.setAttribute(testSuite + "Version", version);
    }

    /**
     * Obtains the author of the current test suite being executed. If no author
     * was set, null will be returned
     *
     * @param clazz   - the test suite class, used for making threadsafe storage of
     *                application, allowing suites to have independent applications
     *                under test, run at the same time
     * @param context - the TestNG context associated with the test suite, used for
     *                storing app url information
     * @return String: the author of the current test being executed
     */
    protected String getAuthor(String clazz, ITestContext context) {
        return (String) context.getAttribute(clazz + "Author");
    }

    /**
     * Sets the author of the current test suite being executed.
     *
     * @param clazz   - the test suite class, used for making threadsafe storage of
     *                application, allowing suites to have independent applications
     *                under test, run at the same time
     * @param context - the TestNG context associated with the test suite, used for
     *                storing app url information
     * @param author  - the author of the test suite
     */
    protected static void setAuthor(Selenified clazz, ITestContext context, String author) {
        String testSuite = clazz.getClass().getName();
        context.setAttribute(testSuite + "Author", author);
    }

    protected static void addHeaders(Map<String, String> headers) {
        extraHeaders = headers;
    }

    /**
     * Runs once before any of the tests run, to parse and setup the static
     * passed information such as browsers, proxy, hub, etc
     *
     * @throws InvalidBrowserException If a browser that is not one specified in the
     *                                 Selenium.Browser class is used, this exception will be thrown
     */
    @BeforeSuite(alwaysRun = true)
    protected void beforeSuite() throws InvalidBrowserException {
        MasterSuiteSetupConfigurator.getInstance().doSetup();
    }

    /**
     * Before any tests run, setup the logging and test details. If a selenium
     * test is being run, it sets up the driver as well
     *
     * @param dataProvider - any objects that are being passed to the tests to loop
     *                     through as variables
     * @param method       - what is the method that is being run. the test name will be
     *                     extracted from this
     * @param test         - was the is context associated with this test suite. suite
     *                     information will be extracted from this
     * @param result       - where are the test results stored. browser information will
     *                     be kept here
     */
    @BeforeMethod(alwaysRun = true)
    protected void startTest(Object[] dataProvider, Method method, ITestContext test, ITestResult result) {
        startTest(dataProvider, method, test, result, DriverSetup.LOAD);
    }

    /**
     * Gathers all of the testing information, and setup up the logging. If a
     * selenium test is running, also sets up the webdriver object
     *
     * @param dataProvider - any objects that are being passed to the tests to loop
     *                     through as variables
     * @param method       - what is the method that is being run. the test name will be
     *                     extracted from this
     * @param test         - was the is context associated with this test suite. suite
     *                     information will be extracted from this
     * @param result       - where are the test results stored. browser information will
     *                     be kept here
     * @param selenium     - is this a selenium test. if so, the webdriver content will
     *                     be setup
     */
    protected void startTest(Object[] dataProvider, Method method, ITestContext test, ITestResult result,
                             DriverSetup selenium) {
        String testName = TestSetup.getTestName(method, dataProvider);
        String outputDir = test.getOutputDirectory();
        String extClass = method.getDeclaringClass().getName();
        String description = "";
        String group = "";
        Test annotation = method.getAnnotation(Test.class);
        // set description from annotation
        if (annotation.description() != null) {
            description = annotation.description();
        }
        // adding in the group if it exists
        if (annotation.groups() != null) {
            group = Arrays.toString(annotation.groups());
            group = group.substring(1, group.length() - 1);
        }

        while (test.getAttribute(testName + INVOCATION_COUNT) == null) {
            test.setAttribute(testName + INVOCATION_COUNT, 0);
        }
        int invocationCount = (int) test.getAttribute(testName + INVOCATION_COUNT);

        Browser myBrowser = browsers.get(invocationCount);
        if (!selenium.useBrowser()) {
            myBrowser = new Browser(BrowserName.NONE);
        }
        DesiredCapabilities myCapability = capabilities.get(invocationCount);
        myCapability.setCapability("name", testName);
        this.capability.set(myCapability);

        OutputFile myFile =
                new OutputFile(outputDir, testName, myBrowser, getTestSite(extClass, test), test.getName(), group,
                        getAuthor(extClass, test), getVersion(extClass, test), description);
        if (selenium.useBrowser()) {
            App app = null;
            try {
                app = new App(myBrowser, myCapability, myFile);
            } catch (InvalidBrowserException | MalformedURLException e) {
                log.error(e);
            }
            this.apps.set(app);
            this.calls.set(null);
            myFile.setApp(app);
            if (selenium.loadPage()) {
                loadInitialPage(app, getTestSite(extClass, test), myFile);
            }
            if (Sauce.isSauce() && app != null) {
                result.setAttribute("SessionId", ((RemoteWebDriver) app.getDriver()).getSessionId());
            }
        } else {
            HTTP http = new HTTP(getTestSite(extClass, test), servicesUser, servicesPass);
            Call call = new Call(http, myFile, extraHeaders);
            this.apps.set(null);
            this.calls.set(call);
        }
        this.browser.set(myBrowser);
        result.setAttribute(BROWSER_INPUT, myBrowser);
        this.files.set(myFile);
    }

    /**
     * Loads the initial app specified by the url, and ensures the app loads
     * successfully
     */
    private void loadInitialPage(App app, String url, OutputFile file) {
        String startingPage = "The starting app <i>";
        String act = "Opening new browser and loading up starting app";
        String expected = startingPage + url + "</i> will successfully load";

        if (app != null) {
            try {
                app.getDriver().get(url);
                if (!app.get().location().contains(url)) {
                    file.recordAction(act, expected,
                            startingPage + app.get().location() + "</i> loaded instead of <i>" + url + "</i>",
                            Result.FAILURE);
                    file.addError();
                    return;
                }
                file.recordAction(act, expected, startingPage + url + "</i> loaded successfully", Result.SUCCESS);
            } catch (Exception e) {
                log.warn(e);
                file.recordAction(act, expected, startingPage + url + "</i> did not load successfully", Result.FAILURE);
                file.addError();
            }
        }
    }

    /**
     * After each test is completed, the test is closed out, and the test
     * counter is incremented
     *
     * @param dataProvider - any objects that are being passed to the tests to loop
     *                     through as variables
     * @param method       - what is the method that is being run. the test name will be
     *                     extracted from this
     * @param test         - was the is context associated with this test suite. suite
     *                     information will be extracted from this
     * @param result       - where are the test results stored. browser information will
     *                     be kept here
     */
    @AfterMethod(alwaysRun = true)
    protected void endTest(Object[] dataProvider, Method method, ITestContext test, ITestResult result) {
        String testName = TestSetup.getTestName(method, dataProvider);
        if (this.apps.get() != null) {
            this.apps.get().killDriver();
        }
        int invocationCount = (int) test.getAttribute(testName + INVOCATION_COUNT);
        test.setAttribute(testName + INVOCATION_COUNT, invocationCount + 1);
    }

    /**
     * Concludes each test case. This should be run as the last time of
     * each @Test. It will close out the output logging file, and count any
     * errors that were encountered during the test, and fail the test if any
     * errors were encountered
     */
    protected void finish() {
        OutputFile myFile = this.files.get();
        myFile.finalizeOutputFile();
        assertEquals("Detailed results found at: " + myFile.getFileName(), "0 errors",
                Integer.toString(myFile.getErrors()) + ERRORS_CHECK);
    }

    /**
     * Concludes each test case. This should be run as the last time of
     * each @Test. It will close out the output logging file, and count any
     * errors that were encountered during the test, and assert that the number
     * of errors that occurred equals the provided number of errors.
     *
     * @param errors - number of expected errors from the test
     */
    protected void finish(int errors) {
        OutputFile myFile = this.files.get();
        myFile.finalizeOutputFile();
        assertEquals("Detailed results found at: " + myFile.getFileName(), errors + ERRORS_CHECK,
                Integer.toString(myFile.getErrors()) + ERRORS_CHECK);
    }

    /**
     * Setups up the initial system for test. As a singleton, this is only done
     * once per test suite.
     *
     * @author max
     */
    private static class MasterSuiteSetupConfigurator {
        private static MasterSuiteSetupConfigurator instance;
        private boolean wasInvoked = false;

        private MasterSuiteSetupConfigurator() {
        }

        /**
         * Runs once before any of the tests run, to parse and setup the static
         * passed information such as browsers, proxy, hub, etc
         *
         * @return null
         */
        static MasterSuiteSetupConfigurator getInstance() {
            if (instance != null) {
                return instance;
            }
            instance = new MasterSuiteSetupConfigurator();
            return instance;
        }

        /**
         * Runs once before any of the tests run, to parse and setup the static
         * passed information such as browsers, proxy, hub, etc
         *
         * @throws InvalidBrowserException If a browser that is not one specified in the
         *                                 Selenium.Browser class is used, this exception will be
         *                                 thrown
         */
        void doSetup() throws InvalidBrowserException {
            if (wasInvoked) {
                return;
            }
            initializeSystem();
            setupTestParameters();
            wasInvoked = true;
            //downgrade our logging
            java.util.logging.Logger.getLogger("io.github").setLevel(Level.SEVERE);
        }

        /**
         * Initializes the test settings by setting default values for the
         * browser, URL, and credentials if they are not specifically set
         */
        private static void initializeSystem() {
            // check the browser
            if (System.getProperty(BROWSER_INPUT) == null) {
                System.setProperty(BROWSER_INPUT, BrowserName.HTMLUNIT.toString());
            }
            if (System.getenv("SERVICES_USER") != null && System.getenv("SERVICES_PASS") != null) {
                servicesUser = System.getenv("SERVICES_USER");
                servicesPass = System.getenv("SERVICES_PASS");
            }
        }

        /**
         * Obtains passed in browser information, and sets up the required
         * capabilities
         *
         * @throws InvalidBrowserException If a browser that is not one specified in the
         *                                 Selenium.Browser class is used, this exception will be
         *                                 thrown
         */
        private static void setupTestParameters() throws InvalidBrowserException {
            browsers = TestSetup.setBrowser();

            for (Browser browser : browsers) {
                TestSetup setup = new TestSetup();
                // are we running remotely on a hub
                if (System.getProperty("hub") != null) {
                    setup.setupBrowserCapability(browser);
                }
                setup.setupProxy();
                setup.setupBrowserDetails(browser);
                DesiredCapabilities caps = setup.getDesiredCapabilities();
                if (extraCapabilities != null) {
                    caps = caps.merge(extraCapabilities);
                }
                capabilities.add(caps);
            }
        }
    }
}