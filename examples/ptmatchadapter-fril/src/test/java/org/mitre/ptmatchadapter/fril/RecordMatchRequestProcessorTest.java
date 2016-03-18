/**
 * PtMatchAdapter - a patient matching system adapter
 * Copyright (C) 2016 The MITRE Corporation.  ALl rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.ptmatchadapter.fril;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.ptmatchadapter.fril.RecordMatchRequestProcessor;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class RecordMatchRequestProcessorTest {

  private static Path workDir;

  @BeforeClass
  public static void setupBeforeClass() throws IOException {
    // Create a temporary directory to act as a work folder
    workDir = Files.createTempDirectory("ptmatch-test-");
  }

  /**
   * Test method for
   * {@link org.mitre.ptmatchadapter.fril.RecordMatchRequestProcessor#loadTemplate(java.lang.String)}
   * .
   */
  @Test
  public void testLoadTemplate() throws FileNotFoundException{
    final RecordMatchRequestProcessor proc = new RecordMatchRequestProcessor();

    assertNotNull(proc.loadTemplate(
        "/templates/fril-dedupe-allFieldsNearlyEqualWeight-accept60.xml"));
    assertNotNull(proc.loadTemplate(
        "templates/fril-dedupe-allFieldsNearlyEqualWeight-accept60.xml"));
  }

  @Test(expected = FileNotFoundException.class) 
  public void testLoadTemplateBadPath() throws FileNotFoundException {
    final RecordMatchRequestProcessor proc = new RecordMatchRequestProcessor();

    assertNotNull(proc.loadTemplate("path/to/nowhere.xml"));
    fail("File Not Found Exception expected");
  }
  
  @Test
  public void testPrepareConfigFile() {
    try {
      final RecordMatchRequestProcessor proc = new RecordMatchRequestProcessor();
      proc.setDeduplicationTemplate(
          "templates/fril-dedupe-allFieldsNearlyEqualWeight-accept60.xml");
      final File jobDir = proc.newRunDir(workDir.toFile().getAbsolutePath());

      final File configFile = proc.prepareMatchingRuleConfiguration(true, jobDir);
      assertNotNull(configFile);
      assertTrue(configFile.exists());
      assertEquals(jobDir.getAbsolutePath(),
          configFile.getParentFile().getAbsolutePath());
      
      // verify we can read config file to read items of interest
      final File dupsFile = proc.getDuplicatesFile(configFile);
      assertNotNull(dupsFile);
      assertTrue(dupsFile.getAbsolutePath().endsWith("duplicates.csv"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testDeduplicate() {
    try {
      final RecordMatchRequestProcessor proc = new RecordMatchRequestProcessor();
      proc.setDeduplicationTemplate(
          "templates/fril-dedupe-allFieldsNearlyEqualWeight-accept60.xml");
      File jobDir = proc.newRunDir(workDir.toFile().getAbsolutePath());

      // Copy the data file to the job directory
      Path target = FileSystems.getDefault().getPath(jobDir.getAbsolutePath(),
          "master-data.csv");
      System.out.println("target path: " + target.toAbsolutePath());

      InputStream instream = this.getClass().getClassLoader()
          .getResourceAsStream("data/master-data-test-13dups.csv");
      assertNotNull(instream);
      try {
        Files.copy(instream, target);
      } finally {
        instream.close();
      }

      File configFile = proc.prepareMatchingRuleConfiguration(true, jobDir);
      int numMatches = proc.findMatches(true, configFile);
      assertEquals(13, numMatches);
      
    } catch (IOException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
  
  @Test
  public void testNoDeduplicate() {
    try {
      final RecordMatchRequestProcessor proc = new RecordMatchRequestProcessor();
      proc.setDeduplicationTemplate(
          "templates/fril-dedupe-allFieldsNearlyEqualWeight-accept60.xml");
      File jobDir = proc.newRunDir(workDir.toFile().getAbsolutePath());

      // Copy the data file to the job directory
      Path target = FileSystems.getDefault().getPath(jobDir.getAbsolutePath(),
          "master-data.csv");
      System.out.println("target path: " + target.toAbsolutePath());

      InputStream instream = this.getClass().getClassLoader()
          .getResourceAsStream("data/master-data-test-no-dups.csv");
      assertNotNull(instream);
      try {
        Files.copy(instream, target);
      } finally {
        instream.close();
      }

      File configFile = proc.prepareMatchingRuleConfiguration(true, jobDir);
      int numMatches = proc.findMatches(true, configFile);
      assertEquals(0, numMatches);
      
    } catch (IOException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
}
