package com.ainq.fhir.saner.simulator;

import java.util.function.BiFunction;
import java.util.function.Predicate;

class Prevalence<T> {
    private final String category;
    private final Predicate<Case> test;
    private final T value;
    private final float prevalence;

    Prevalence(String category, BiFunction<Case, T, Boolean> getter, T value, float prevalence) {
        this.category = category;
        this.test = s -> getter.apply(s, value);
        this.value = value;
        this.prevalence = prevalence;
    }

    /**
     * Return the demogoagraphic category for this rate.
     *
     * @return the demogoagraphic category for this rate
     */
    public String getCategory() {
        return category;
    }

    /**
     * Return the value.
     *
     * @return The value.
     */
    public T getValue() {
        return value;
    }

    public float getPrevalence() {
        return prevalence;
    }

    public boolean test(Case c) {
        return test.test(c);
    }
}