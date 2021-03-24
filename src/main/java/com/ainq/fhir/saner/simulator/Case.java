package com.ainq.fhir.saner.simulator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ainq.fhir.saner.sampledata.Generator;

public class Case {
    private static final Logger LOGGER = LoggerFactory.getLogger(Case.class);
    private static final boolean USE_UUID = false;
    private static int caseCount = 0;
    private final Set<String> riskFactor = new TreeSet<String>();
    private String patientId = generateIdentifier();
    private String gender;
    private Set<String> race = new TreeSet<String>();
    private String ethnicity;
    private int age;
    private Date dob;
    private int hospLOS, icuLOS;
    private Date startDate, endDate, icuStart, icuEnd;
    private Patient patient;
    private Encounter encounter;
    private List<Resource> data;

    private static String generateIdentifier() {
        return USE_UUID ? UUID.randomUUID().toString() : Integer.toString(++caseCount);
    }

    public Case(Date day, boolean isInitialCase) {
        generateRandomData(day, isInitialCase);
        patientId = patient.getIdElement().getIdPart();
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
    public Set<String> getRace() {
        return race;
    }

    /**
     * @return the true if race contains race
     * @param race The race to search for
     */
    public boolean hasRace(String race) {
        return this.race.contains(race);
    }

    /**
     * @param race the race or ethnicity to add
     */
    public void addRace(String race) {
        this.race.add(race);
    }

    /**
     * @return the ethnicity
     */
    public String getEthnicity() {
        return ethnicity;
    }

    /**
     * @param ethnicity the ethnicity to set
     */
    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
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
        endDate = cal.getTime();
        if (icuLOS > 0) {
            // TODO: Adjust it backwards a little bit.  One typically
            // isn't discharged straight from ICU except for death.
            icuEnd = endDate;
            cal.add(Calendar.DATE, - icuLOS);
            icuStart = cal.getTime();
        }
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
     * @return the icuEnd
     */
    public Date getIcuEnd() {
        return icuEnd;
    }

    /**
     * @return the risk factors
     */
    public Set<String> getRiskFactor() {
        return riskFactor;
    }

    private void generateRandomData(Date day, boolean isInitialCase) {
        int errors = 0;
        boolean wasReset = false;
        do {
            double variate = Math.random();
            variate = rankAndSet(variate, 0.654, "2106-3", this::addRace);   // White
            variate = rankAndSet(variate, 0.238, "2054-5", this::addRace);   // Black or African American
            variate = rankAndSet(variate, 0.007, "1002-5", this::addRace);   // American Indian or Alaska Native
            variate = rankAndSet(variate, 0.079, "2028-9", this::addRace);   // Asian
            variate = rankAndSet(variate, 0.001, "2076-8", this::addRace);   // Native Hawaiian or Other Pacific Islander
            if (getRace().size() == 0) {
                // Multiple races for the remainder.
                while (getRace().size() <  2) {
                    variate = Math.random();
                    variate = rankAndSet(variate, 0.654, "2106-3", this::addRace);   // White
                    variate = rankAndSet(variate, 0.238, "2054-5", this::addRace);   // Black or African American
                    variate = rankAndSet(variate, 0.007, "1002-5", this::addRace);   // American Indian or Alaska Native
                    variate = rankAndSet(variate, 0.079, "2028-9", this::addRace);   // Asian
                    variate = rankAndSet(variate, 0.001, "2076-8", this::addRace);   // Native Hawaiian or Other Pacific Islander
                }
            }

            variate = Math.random();
            variate = rankAndSet(variate, 0.256, "2135-2", this::setEthnicity);   // Hispanic
            variate = rankAndSet(variate, 0.744, "2186-5", this::setEthnicity);   // Non-Hispanic

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

            setGender(Math.random() < 0.479 ? "male" : "female");

            Map<String, String> properties = new HashMap<>();
            int count = 0;
            for (String race: getRace()) {
                properties.put("race" + count++, race);
            }
            properties.put("ethnicity", getEthnicity());
            properties.put("age", Integer.toString(getAge()));
            properties.put("gender", getGender());

            patient = CaseSimulator.getPatientGenerator().generate(properties);
            if (patient == null) {
                LOGGER.debug("Error: could not generate patient with {}", properties);
                if (++errors > 10) {
                    if (wasReset) {
                        LOGGER.error("Error: could not generate patient after reset with {}", properties);
                        throw new RuntimeException("Error: could not generate patient with " + properties);
                    }
                    CaseSimulator.getPatientGenerator().reset();
                    wasReset = true;
                    errors = 0;
                }
            }
        } while (patient == null);  // Try another case on failure
        encounter = (Encounter) patient.getUserData("encounter");
        Encounter   icu = (Encounter) patient.getUserData("icu");

        int los = CaseSimulator.lengthInDays(encounter.getPeriod());
        int iculos = icu == null ? 0 : CaseSimulator.lengthInDays(icu.getPeriod());
        int offset = CaseSimulator.lengthInDays(encounter.getPeriod().getStart(), day);
        patient.setUserData("offset", offset);
        setHospLOS(los + iculos);
        setIcuLOS(iculos);

        Calendar cal = Calendar.getInstance();
        cal.setTime(day);

        int pos = 0;
        if (isInitialCase) {
            // Back up the start day to a random position within the stay.
            pos = Generator.RANDOM.nextInt(getLOS());
            cal.add(Calendar.DATE, -pos);
        }
        setStartDate(cal.getTime());
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

    /**
     * @return the patient
     */
    public Patient getPatient() {
        return patient;
    }

    /**
     * @return the encounter
     */
    public Encounter getEncounter() {
        return encounter;
    }
}
