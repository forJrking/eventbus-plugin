package com.forjrking.eventbus

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase

class LineMarkerTest : LightJavaCodeInsightFixtureTestCase() {

    fun test1() {
        myFixture.configureByText(
            "Sample.java", "package com.forjrking.eventbus;\n" +
                    "\n" +
                    "import org.greenrobot.eventbus.EventBus;\n" +
                    "\n" +
                    "public class Sample {\n" +
                    "    \n" +
                    "    public void something() {\n" +
                    "        EventBus.getDefault().post(new Object());\n" +
                    "    }\n" +
                    "}\n"
        )
        val findAllGutters = myFixture.findAllGutters()
        TestCase.assertEquals(3, findAllGutters.size)
    }

}

