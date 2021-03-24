package com.ainq.fhir.saner.sampledata.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.ainq.fhir.saner.sampledata.CsvResourceLoader;
import com.ainq.fhir.saner.sampledata.PatientGenerator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.FhirTerser;

class CsvResourceLoaderTest {
    private static IParser jp = FhirContext.forR4().newJsonParser().setPrettyPrint(true);
    private FhirContext ctx = FhirContext.forR4();

    @ParameterizedTest
    @CsvSource( {
        PatientGenerator.LOCAL_URL + "patients.csv, Patient, 100",
        PatientGenerator.LOCAL_URL + "encounters.csv, Encounter, 100",
        PatientGenerator.LOCAL_URL + "conditions.csv, Condition, 100",
        PatientGenerator.LOCAL_URL + "allergies.csv, AllergyIntolerance, 100",
        PatientGenerator.LOCAL_URL + "conditions.csv, Condition, 100",
        PatientGenerator.LOCAL_URL + "imaging_studies.csv, ImagingStudy, 100",
        PatientGenerator.LOCAL_URL + "medications.csv, MedicationStatement, 100",
        PatientGenerator.LOCAL_URL + "observations.csv, Observation, 100",
        PatientGenerator.LOCAL_URL + "providers.csv, Practitioner, 100",
        PatientGenerator.LOCAL_URL + "procedures.csv, Procedure, 100"

    })
    void testCsvResourceLoader(String url, String resourceType, int max) {
        String map[] = CsvResourceLoader.getMap(resourceType);

        assertNotNull(map, "No Map for " + resourceType);
        FhirContext ctx = FhirContext.forR4();
        @SuppressWarnings("unchecked")
        Class<? extends Resource> type = (Class<? extends Resource>) ctx.getResourceDefinition(resourceType).getImplementingClass();

        boolean hasField[] = new boolean[map.length / 2];
        CsvResourceLoader.createResources(type, url, map, t -> testResource(t, map, hasField), null, null, max);

        for (int i = 0; i < hasField.length; i++) {
            assertTrue(hasField[i], "Missing value in " + map[i + i]);
        }
    }

    private <T extends Resource> boolean testResource(T r, String map[], boolean[] fields) {
        System.out.println(jp.encodeResourceToString(r));

        FhirTerser t = ctx.newTerser();
        for (int i = 0; i < fields.length; i++) {
            String field = map[i + i];
            try {
                if (!t.getValues(r, field).isEmpty()) {
                    fields[i] = true;
                }
            } catch (Exception e) {
                // System.err.printf("Exception testing %s: %s\n", field, e);
            }
        }
        return true;
    }

}
