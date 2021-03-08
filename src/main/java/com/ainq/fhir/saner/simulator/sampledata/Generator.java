package com.ainq.fhir.saner.simulator.sampledata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface Generator {
    public String generate(Map<String, String> properties);
    public default List<String> generate(int count, Map<String, String> properties) {
        List<String> results = new ArrayList<>();
        while (count-- > 0) {
            results.add(generate(properties));
        }
        return results;
    }

}
