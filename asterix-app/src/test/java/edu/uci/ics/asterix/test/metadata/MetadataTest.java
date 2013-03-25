/*
 * Copyright 2009-2012 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.asterix.test.metadata;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.uci.ics.asterix.api.common.AsterixHyracksIntegrationUtil;
import edu.uci.ics.asterix.common.config.GlobalConfig;
import edu.uci.ics.asterix.test.aql.TestsUtils;
import edu.uci.ics.asterix.testframework.context.TestCaseContext;
import edu.uci.ics.asterix.testframework.xml.TestCase.CompilationUnit;

/**
 * Executes the Metadata tests.
 */
@RunWith(Parameterized.class)
public class MetadataTest {

    private TestCaseContext tcCtx;

    private static final Logger LOGGER = Logger.getLogger(MetadataTest.class.getName());
    private static final String PATH_ACTUAL = "mdtest/";
    private static final String PATH_BASE = "src/test/resources/metadata/";
    private static final String TEST_CONFIG_FILE_NAME = "test.properties";
    private static final String WEB_SERVER_PORT = "19002";

    public MetadataTest(TestCaseContext tcCtx) {
        this.tcCtx = tcCtx;
    }

    @Test
    public void test() throws Exception {
        List<CompilationUnit> cUnits = tcCtx.getTestCase().getCompilationUnit();
        for (CompilationUnit cUnit : cUnits) {
            File testFile = tcCtx.getTestFile(cUnit);
            
            /*****************
            if (!testFile.getAbsolutePath().contains("meta09.aql")) {
                System.out.println(testFile.getAbsolutePath());
                continue;
            }
            System.out.println(testFile.getAbsolutePath());
            *****************/
            
            
            File expectedResultFile = tcCtx.getExpectedResultFile(cUnit);
            File actualFile = new File(PATH_ACTUAL + File.separator
                    + tcCtx.getTestCase().getFilePath().replace(File.separator, "_") + "_" + cUnit.getName() + ".adm");

            File actualResultFile = tcCtx.getActualResultFile(cUnit, new File(PATH_ACTUAL));
            actualResultFile.getParentFile().mkdirs();
            try {
                TestsUtils.runScriptAndCompareWithResult(AsterixHyracksIntegrationUtil.getHyracksClientConnection(),
                        testFile, new PrintWriter(System.err), expectedResultFile, actualFile);
            } catch (Exception e) {
                LOGGER.severe("Test \"" + testFile + "\" FAILED!");
                e.printStackTrace();
                if (cUnit.getExpectedError().isEmpty()) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!", e);
                }
            }
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(GlobalConfig.CONFIG_FILE_PROPERTY, TEST_CONFIG_FILE_NAME);
        System.setProperty(GlobalConfig.WEB_SERVER_PORT_PROPERTY, WEB_SERVER_PORT);
        File outdir = new File(PATH_ACTUAL);
        outdir.mkdirs();

        File log = new File("asterix_logs");
        if (log.exists())
            FileUtils.deleteDirectory(log);
        File lsn = new File("last_checkpoint_lsn");
        lsn.deleteOnExit();

        AsterixHyracksIntegrationUtil.init();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        AsterixHyracksIntegrationUtil.deinit();
        File outdir = new File(PATH_ACTUAL);
        File[] files = outdir.listFiles();
        if (files == null || files.length == 0) {
            outdir.delete();
        }

        // clean up the files written by the ASTERIX storage manager
        for (String d : AsterixHyracksIntegrationUtil.ASTERIX_DATA_DIRS) {
            TestsUtils.deleteRec(new File(d));
        }

        File log = new File("asterix_logs");
        if (log.exists())
            FileUtils.deleteDirectory(log);
        File lsn = new File("last_checkpoint_lsn");
        lsn.deleteOnExit();
    }

    @Parameters
    public static Collection<Object[]> tests() throws Exception {
        Collection<Object[]> testArgs = new ArrayList<Object[]>();
        TestCaseContext.Builder b = new TestCaseContext.Builder();
        for (TestCaseContext ctx : b.build(new File(PATH_BASE))) {
            testArgs.add(new Object[] { ctx });
        }
        return testArgs;
    }

}