/**
 * PtMatchAdapter - a patient matching system adapter
 * Copyright (C) 2016 The MITRE Corporation.  ALl rights reserved.
 *
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * </p>
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package org.mitre.ptmatchadapter.fril;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Bundle.BundleEntrySearchComponent;
import org.hl7.fhir.instance.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.instance.model.CodeType;
import org.hl7.fhir.instance.model.DecimalType;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.MessageHeader.ResponseType;
import org.hl7.fhir.instance.model.StringType;
import org.hl7.fhir.instance.model.UriType;

import org.mitre.ptmatchadapter.recordmatch.BasicRecordMatchResultsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michael Los, mel@mitre.org
 *
 */
public class RecordMatchResultsBuilder extends BasicRecordMatchResultsBuilder {
  private static final Logger LOG = LoggerFactory
      .getLogger(RecordMatchResultsBuilder.class);

  private File duplicatesFile;

  public RecordMatchResultsBuilder(Bundle requestMsg, ResponseType respCode) {
    super(requestMsg, respCode);
  }

  /**
   * @see BasicRecordMatchResultsBuilder#build()
   */
  public Bundle build() throws IOException {

    final Bundle resultMsg = super.build();

    // Add entries for the Linked Records
    addLinkedRecordEntries(resultMsg);

    return resultMsg;
  }

  private static final int DUPLICATE_ID_COL = 0;
  private static final int SCORE_COL = 1;
  private static final int FULL_URL_COL = 2;

  /**
   *
   * @param bundle
   *          Bundle to which an entry for each linked record will be added
   * @throws IOException
   *           thrown when the file containing the linked results is not found
   *           or could not be processed
   */
  private void addLinkedRecordEntries(Bundle bundle) throws IOException {
    final Reader in = new FileReader(duplicatesFile);
    try {
      final Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);

      String refRecordUrl = null;
      String curDupId = "0";

      // see https://www.hl7.org/fhir/valueset-patient-mpi-match.html
      final CodeType certain = new CodeType("certain");
      final CodeType probable = new CodeType("probable");
      final CodeType possible = new CodeType("possible");
      final CodeType certainlyNot = new CodeType("certainly-not");

      for (CSVRecord record : records) {
        String duplicateId = record.get(DUPLICATE_ID_COL);
        String scoreStr = record.get(SCORE_COL);
        String fullUrl = record.get(FULL_URL_COL);

        if (curDupId.equals(duplicateId)) {
          if (refRecordUrl == null) {
            LOG.warn("Unexpected condition, curDupId {}, duplicateId {}", curDupId,
                duplicateId);
            continue;
          }

          BundleEntryComponent entry = new BundleEntryComponent();
          entry.setFullUrl(refRecordUrl);

          BundleEntrySearchComponent search = new BundleEntrySearchComponent();
          // fril returns results 0 - 100; normalize to 0 - 1;
          double score = Double.valueOf(scoreStr).doubleValue() / 100.;
          search.setScoreElement(new DecimalType(score));

          // TODO Add Extension that maps score value to a term (e.g., probable)
          Extension searchExt = new Extension(new UriType(
              "http://hl7.org/fhir/StructureDefinition/patient-mpi-match"));
          if (score > 0.85) {
            searchExt.setValue(certain);
          } else if (score > 0.65) {
            searchExt.setValue(probable);
          } else if (score > .45) {
            searchExt.setValue(possible);
          } else {
            searchExt.setValue(certainlyNot);
          }
          search.addExtension(searchExt);
          entry.setSearch(search);

          // Add information about the resource type

          BundleLinkComponent link = new BundleLinkComponent(new StringType("type"),
              new UriType("http://hl7.org/fhir/Patient"));
          entry.addLink(link);

          // Add the link to the duplicate record
          link = new BundleLinkComponent(
              new StringType("related"), new UriType(fullUrl));
          entry.addLink(link);

          bundle.addEntry(entry);
        } else {
          // new set of duplicates
          curDupId = duplicateId;
          refRecordUrl = fullUrl;
        }
      }
    } finally {
      in.close();
    }
  }


  public RecordMatchResultsBuilder duplicates(File file) {
    duplicatesFile = file;
    return this;
  }
}
