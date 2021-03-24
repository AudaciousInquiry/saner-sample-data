package com.ainq.fhir.saner.sampledata.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.junit.jupiter.api.Test;

import com.ainq.fhir.saner.sampledata.AddressGenerator;

public class LoaderTests {
    private static final int COUNT = 100;
    @Test
    public void testAddressGenerator() {
        Map<String, Integer> cities = new TreeMap<>();
        Map<String, Integer> postalCodes = new TreeMap<>();
        Map<String, Integer> lines = new TreeMap<>();
        Map<?, ?>[] maps = { cities, postalCodes, lines };

        AddressGenerator generator = new AddressGenerator();

        for (int i = 0; i < COUNT; i++) {
            Address address = generator.generate(null);
            String city = address.hasCity() ? address.getCity() : "";
            String state = address.hasCity() ? address.getState() : "";
            String postalCode = address.hasPostalCode() ? address.getPostalCode() : "";
            String line = address.hasLine() ? address.getLine().get(0).asStringValue() : null;
            String values[] = { city, postalCode, line };

            for (int j = 0; j < values.length; j++) {
                @SuppressWarnings("unchecked")
                Map <String, Integer> map = (Map<String, Integer>) maps[j];
                Integer value = maps[j].containsKey(values[j]) ? map.get(values[j]) : 0;
                map.put(values[j], value + 1);
            }
            assertFalse(StringUtils.isBlank(line), "There should be an address line that is not blank, empty or null");
            assertFalse(StringUtils.isBlank(address.getCity()), "There should be a city that is not blank, empty or null");
            if (address.hasState()) {
                assertFalse(StringUtils.isBlank(state), "There should be a state that is not blank, empty or null");
                assertEquals("IL", state);
            }
            // assertFalse(StringUtils.isBlank(address.getPostalCode()), "There should be a zip code that is not blank, empty or null");
        }

//        System.out.println("Cities\n:" + cities);
//        System.out.println("Postal Codes\n:" + postalCodes);
//        System.out.println("Lines\n:" + lines);

        assertTrue(cities.size() > 4, "Insufficient diversity in city " + cities.size());
        assertTrue(postalCodes.size() > 3, "Insufficient diversity in postal code " + postalCodes.size());
        assertTrue(lines.size() > COUNT / 2, "Insufficient diversity in address line " + lines.size());
    }
}
