package com.ainq.fhir.saner.simulator;

import java.util.ArrayList;
import java.util.List;

import com.ainq.fhir.saner.sampledata.Generator;

class Assigner {
    List<Prevalence<?>> category;

    double prevalence;
    private Assigner(List<Prevalence<?>> category, double prevalence) {
        this.category = category;
        this.prevalence = prevalence;
    }
    /**
     * Combine risk factors based on rates This function computes a rate table from
     * rates by taking the cross product of risk factors in different categories.
     * This assumes (incorrectly) that there is no correlation between risk factors.
     * This is sufficient for creating demonstration data, but not for disease
     * modelling purposes.
     *
     * @param rates The prevalence of disease for a specific demographic categories (risk factors, e.g. gender)
     * and subclassifications (e.g., Male, Female) within those categories.
     *
     * @return A list of Assigners that can be used to assign additional risk factors to a case
     * based on existing risk factors.
     */
    static List<Assigner> combineRiskFactors(Prevalence<?>[] rates) {
        String lastCategory = null;
        boolean skip = false;
        List<List<Prevalence<?>>> categories = new ArrayList<>();
        List<Prevalence<?>> set = null;

        for (Prevalence<?> rate : rates) {
            if (rate.getCategory().equals(lastCategory)) {
                if (skip) {
                    continue;
                }
            } else {
                lastCategory = rate.getCategory();
                set = new ArrayList<>();
                categories.add(set);
            }
            set.add(rate);
        }

        categories = Case.cartesianProduct(0, categories);
        List<Assigner> result = new ArrayList<>();
        for (List<Prevalence<?>> cat: categories) {
            double p = 1.0;
            for (Prevalence<?> rate: cat) {
                p *= rate.getPrevalence();
            }
            Assigner a = new Assigner(cat, p);
            result.add(a);
        }
        return result;
    }

    /**
     * Given a case c, determine if this assigner is applicable.
     * If it is, then set the value if appropriate.
     * @param c The case to test
     * @param setter    The setter to use to assign it.
     */
    boolean assign(Case c, Runnable setter) {
        // Compute a uniform random variable between 0 and 1.
        double variable = Generator.RANDOM.nextDouble();
        for (Prevalence<?> cat: category) {
            if (!cat.test(c)) {
                return false;
            }
        }
        // All matched
        if (variable < prevalence) {
            // Assign it to this case
            setter.run();
        }
        return true;
    }
}