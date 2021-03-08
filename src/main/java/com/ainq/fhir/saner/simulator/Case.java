package com.ainq.fhir.saner.simulator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

public class Case {
    private static final boolean USE_UUID = false;

    private static int caseCount = 0;
    private final Set<String> riskFactor = new TreeSet<String>();
    private final String patientId = generateIdentifier();
    private String gender;
    private Set<String> raceAndEthnicity = new TreeSet<String>();
    private int age;
    private Date dob;
    private int hospLOS, icuLOS;
    private Date startDate, endDate, icuStart, icuEnd;

    private static String generateIdentifier() {
        return USE_UUID ? UUID.randomUUID().toString() : Integer.toString(++caseCount);
    }

    public Case(Date day, boolean isInitialCase) {
        generateRandomData(day, isInitialCase);
    }

    public List<Resource> asResources() {
        List<Resource> result = new ArrayList<>();
        Patient patient = new Patient();
        result.add(patient);
        patient.setId(patientId);
        patient.addIdentifier()
            .setValue((USE_UUID ? "urn:uuid:" : "") + patientId)
            .setSystem(USE_UUID ? "urn:ietf:rfc:3986" : "http://example.com/test/patient");

        return result;
    }

    /**
     * @return the patientId
     */
    public String getPatientId() {
        return patientId;
    }

    /**
     * @return the gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * @param gender the gender to set
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * @return the race
     */
    public Set<String> getRaceOrEthnicity() {
        return raceAndEthnicity;
    }

    /**
     * @return the true if race contains race
     * @param race The race to search for
     */
    public boolean hasRaceOrEthnicity(String race) {
        return this.raceAndEthnicity.contains(race);
    }

    /**
     * @param race the race or ethnicity to add
     */
    public void addRaceOrEthnicity(String race) {
        this.raceAndEthnicity.add(race);
    }

    /**
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * @param age the age to set
     */
    public void setAge(int age) {
        this.age = age;
        // Get today's date
        Calendar day = Calendar.getInstance();
        // Subtract the patient age
        day.add(Calendar.YEAR, -age);
        // Pick a random day in the previous year.
        day.add(Calendar.DAY_OF_YEAR, -(int) (Math.random() * 365));
    }

    /**
     * @return the date of birth.
     */
    public Date getDob() {
        return dob;
    }

    /**
     * @return the hasICUStay
     */
    public boolean hasICUStay() {
        return this.icuLOS != 0;
    }

    /**
     * @return the hospLOS
     */
    public int getHospLOS() {
        return hospLOS;
    }

    /**
     * @param hospLOS the hospLOS to set
     */
    public void setHospLOS(int hospLOS) {
        this.hospLOS = hospLOS;
    }

    /**
     * @return the icuLOS
     */
    public int getIcuLOS() {
        return icuLOS;
    }

    /**
     * @param icuLOS the icuLOS to set
     */
    public void setIcuLOS(int icuLOS) {
        this.icuLOS = icuLOS;
    }

    public int getLOS() {
        return icuLOS + hospLOS;
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DATE, getLOS());
        this.endDate = cal.getTime();
    }

    /**
     * @return the endDate
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * @return the icuStart
     */
    public Date getIcuStart() {
        return icuStart;
    }

    /**
     * @param icuStart the icuStart to set
     */
    public void setIcuStart(Date icuStart) {
        this.icuStart = icuStart;
        Calendar cal = Calendar.getInstance();
        cal.setTime(icuStart);
        cal.add(Calendar.DATE, getIcuLOS());
        this.icuEnd = cal.getTime();
    }

    /**
     * @return the icuEnd
     */
    public Date getIcuEnd() {
        return icuEnd;
    }

    /**
     * @param icuEnd the icuEnd to set
     */
    public void setIcuEnd(Date icuEnd) {
        this.icuEnd = icuEnd;
    }

    /**
     * @return the risk factors
     */
    public Set<String> getRiskFactor() {
        return riskFactor;
    }

    private void generateRandomData(Date day, boolean isInitialCase) {

        double variate = Math.random();
        variate = rankAndSet(variate, 0.654, "White", this::addRaceOrEthnicity);
        variate = rankAndSet(variate, 0.238, "Black", this::addRaceOrEthnicity);
        variate = rankAndSet(variate, 0.007, "Native American", this::addRaceOrEthnicity);
        variate = rankAndSet(variate, 0.079, "Asian", this::addRaceOrEthnicity);
        variate = rankAndSet(variate, 0.001, "Hawaiian", this::addRaceOrEthnicity);
        variate = rankAndSet(variate, 0.010, "Unknown", this::addRaceOrEthnicity);
        while (getRaceOrEthnicity().size() < 0) {
            variate = Math.random();
            variate = rankAndSet(variate, 0.654, "White", this::addRaceOrEthnicity);
            variate = rankAndSet(variate, 0.238, "Black", this::addRaceOrEthnicity);
            variate = rankAndSet(variate, 0.007, "Native American", this::addRaceOrEthnicity);
            variate = rankAndSet(variate, 0.079, "Asian", this::addRaceOrEthnicity);
            variate = rankAndSet(variate, 0.001, "Hawaiian", this::addRaceOrEthnicity);
        }

        variate = Math.random();
        variate = rankAndSet(variate, 0.256, "Hispanic", this::addRaceOrEthnicity);
        variate = rankAndSet(variate, 1.0, "Not Hispanic", this::addRaceOrEthnicity);

        variate = Math.random();
        variate = rankAndSet(variate, 0.060, (int) (Math.random() * 5) + 0, this::setAge);
        variate = rankAndSet(variate, 0.057, (int) (Math.random() * 5) + 5, this::setAge);
        variate = rankAndSet(variate, 0.063, (int) (Math.random() * 5) + 10, this::setAge);
        variate = rankAndSet(variate, 0.060, (int) (Math.random() * 5) + 15, this::setAge);
        variate = rankAndSet(variate, 0.063, (int) (Math.random() * 5) + 20, this::setAge);
        variate = rankAndSet(variate, 0.163, (int) (Math.random() * 10) + 25, this::setAge);
        variate = rankAndSet(variate, 0.136, (int) (Math.random() * 10) + 35, this::setAge);
        variate = rankAndSet(variate, 0.124, (int) (Math.random() * 10) + 45, this::setAge);
        variate = rankAndSet(variate, 0.062, (int) (Math.random() * 5) + 55, this::setAge);
        variate = rankAndSet(variate, 0.061, (int) (Math.random() * 5) + 60, this::setAge);
        variate = rankAndSet(variate, 0.087, (int) (Math.random() * 10) + 65, this::setAge);
        variate = rankAndSet(variate, 0.045, (int) (Math.random() * 10) + 75, this::setAge);
        variate = rankAndSet(variate, 0.020, (int) (Math.random() * 15) + 85, this::setAge);

        setGender(Math.random() < 0.479 ? "Male" : "Female");


    }

    private <T> double rankAndSet(double variable, double percentile, T value, Consumer<T> t) {
        if (variable > 0 && variable < percentile) {
            t.accept(value);
        }
        variable -= percentile;
        return variable;
    }

    static List<List<Prevalence<?>>> cartesianProduct(int index, List<List<Prevalence<?>>> sets) {
        List<List<Prevalence<?>>> ret = new ArrayList<>();
        if (index == sets.size()) {
            ret.add(Collections.emptyList());
        } else {
            for (Prevalence<?> obj : sets.get(index)) {
                for (List<Prevalence<?>> set : cartesianProduct(index+1, sets)) {
                    set.add(obj);
                    ret.add(set);
                }
            }
        }
        return ret;
    }

}
