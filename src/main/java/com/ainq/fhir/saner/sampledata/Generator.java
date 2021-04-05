package com.ainq.fhir.saner.sampledata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.hl7.fhir.r4.model.Base;

public interface Generator<B extends Base> {
    /**
     * A source of random information that can be used by all generators.
     */
    public static final Random RANDOM = new Random(0x53414E5221l);
    /**
     * Initialize the generator.
     */
    public void initialize();
    /**
     * Generate a random record matching properties
     * @param properties The properties to match.
     */
    public B generate(Map<String, String> properties);

    /**
     * Generate count random instances matching properties.
     * @param count The number of instances to generate
     * @param properties    Thee properties to match
     * @return  A list of the generated instances.
     */
    public default List<B> generate(int count, Map<String, String> properties) {
        List<B> results = new ArrayList<>();
        while (count-- > 0) {
            results.add(generate(properties));
        }
        return results;
    }

    public Collection<B> getAll();

    /** Reset the random sequence */
    public default void reset() {
        // Do nothing.
    }
}
