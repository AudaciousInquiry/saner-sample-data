package com.ainq.fhir.saner.sampledata;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ainq.fhir.saner.simulator.CaseSimulator;

public class PatientGenerator implements Generator<Patient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientGenerator.class);
    public final static String DATA_URL = "jar:https://storage.googleapis.com/synthea-public/100k_synthea_covid19_csv.zip!/100k_synthea_covid19_csv/";
    public final static String LOCAL_URL = "jar:classpath:synthetic-data.zip!/10k_synthea_covid19_csv/";

    Integer ageGroupBounds[] = { 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 110 };
    List<Set<String>> patientsByAge = new ArrayList<>(ageGroupBounds.length);
    {
        for (@SuppressWarnings("unused") int age: ageGroupBounds) {
            patientsByAge.add(new TreeSet<>());
        }
    }

    String genders[] = { "male", "female" };
    List<Set<String>> patientsByGender = new ArrayList<>(genders.length);
    {
        for (@SuppressWarnings("unused") String gender: genders) {
            patientsByGender.add(new TreeSet<>());
        }
    }

    String raceOrEthnicity[] = { "2135-2", "2186-5", "1002-5", "2028-9", "2054-5", "2076-8", "2106-3" };
    List<Set<String>> patientsByRaceOrEthnicity = new ArrayList<>(raceOrEthnicity.length);
    {
        for (@SuppressWarnings("unused") String re: raceOrEthnicity) {
            patientsByRaceOrEthnicity.add(new TreeSet<>());
        }
    }

    Map<String, Patient> patientMap = new TreeMap<>();
    Set<String> patients = new TreeSet<>();
    @Override
    public Patient generate(Map<String, String> properties) {
        Set<String> matches = patients;
        boolean fixGender = false, fixAge = false, fixRace = false, fixEthnicity = false;
        Set<String> add;

        if (properties.containsKey("id")) {
            String id = properties.get("id");
            return patientMap.get(id);
        }
        /**
         * Map the properties into appropriate values for query.
         */

        String  race1 = properties.get("race0"),
                race2 = properties.get("race1"),
                ethnicity = properties.get("ethnicity"),
                age = properties.get("age"),
                gender = properties.get("gender");

        if (gender != null) {
            add = findMatchingSet("gender", gender, genders, patientsByGender, f -> gender.equals(f));
            fixGender = add.isEmpty();
            if (!fixGender) {
                matches = merge(matches, add);
            }
        }

        if (age != null) {
            int ageValue = Integer.parseInt(age);
            add = findMatchingSet("age", age, ageGroupBounds, patientsByAge, f -> ageValue < f);
            fixAge = add.isEmpty();
            if (!fixAge) {
                matches = merge(matches, add);
            }
        }

        if (race1 != null) {
            add = findMatchingSet("race", race1, raceOrEthnicity, patientsByRaceOrEthnicity, f -> race1.equals(f));
            fixRace = add.isEmpty();
            if (!fixRace) {
                matches = merge(matches, add);
            }
        }

        // Ignore race2, we'll set it if necessary.
        if (ethnicity != null) {
            add = findMatchingSet("ethnicity", ethnicity, raceOrEthnicity, patientsByRaceOrEthnicity, f -> ethnicity.equals(f));
            fixEthnicity = add.isEmpty();
            if (!fixEthnicity) {
                matches = merge(matches, add);
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        Patient p = null;
        for (String id: matches) {
            p = patientMap.get(id);
            if (p != null) {
                break;
            }
        }

        // Adjust p if necessary
        if (fixRace) {
            Extension e = p.getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
            if (e == null) {
                e = p.addExtension().setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
                e = e.addExtension().setUrl("ombCategory");
            } else {
                e = e.getExtensionByUrl("ombCategory");
            }
            e.setValue(new Coding().setSystem("urn:oid:2.16.840.1.113883.6.238").setCode(race2));

        }
        if (!StringUtils.isEmpty(race2)) {
            Extension e = p.getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
            if (e == null) {
                e = p.addExtension().setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
            }
            e.addExtension("ombCategory", new Coding().setSystem("urn:oid:2.16.840.1.113883.6.238").setCode(race2));
        }
        if (fixEthnicity) {
            Extension e = p.getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
            if (e == null) {
                e = p.addExtension().setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
                e = e.addExtension().setUrl("ombCategory");
            } else {
                e = e.getExtensionByUrl("ombCategory");
            }
            e.setValue(new Coding().setSystem("urn:oid:2.16.840.1.113883.6.238").setCode(ethnicity));
        }
        if (fixAge) {
            p.setUserData("age", age);
            // This is icky, what do we do?
        }
        if (fixGender) {
            p.setGender(AdministrativeGender.fromCode(gender));
        }
        // Ensure this patient isn't selected again.
        patients.remove(p.getIdElement().getIdPart());
        if (patients.isEmpty()) {
            // start over again
            LOGGER.error("Ran out of patients");
            reset();
        }
        return p;
    }

    private Set<String> merge(Set<String> matches, Set<String> filter) {
        Set<String> smaller = matches.size() > filter.size() ? filter : matches,
                    larger =  matches.size() > filter.size() ? matches : filter;

        smaller = new HashSet<String>(smaller);
        smaller.retainAll(larger);
        return smaller;
    }

    private <T> Set<String> findMatchingSet(String fieldName, String field, T fieldValues[], List<Set<String>> list, Predicate<T> test) {
        if (!StringUtils.isEmpty(field)) {
            for (int i = 0; i < fieldValues.length; i++) {
                if (test.test(fieldValues[i])) {
                    return list.get(i);
                }
            }
        }
        return Collections.emptySet();
    }

    @Override
    public void initialize() {
        readPatients();
        getCovidEncounterData();
        index(patientMap.values());
    }

    private void index(Collection<Patient> patients) {
        Set<String> match = null;
        for (Patient p: patients) {
            int age = (int) p.getUserData("age");
            for (int i = 0; i < ageGroupBounds.length; i++) {
                if (age < ageGroupBounds[i]) {
                    match = patientsByAge.get(i);
                    match.add(p.getIdElement().getIdPart());
                    break;
                }
            }
            for (int i = 0; i < genders.length; i++) {
                if (genders[i].equals(p.getGender().toCode())) {
                    match = patientsByGender.get(i);
                    match.add(p.getIdElement().getIdPart());
                    break;
                }
            }

            int count = 2;
            for (int i = 0; i < raceOrEthnicity.length; i++) {
                Coding value1 =
                    (Coding) p
                        .getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race")
                        .getExtensionByUrl("ombCategory").getValue(),
                       value2 =
                           (Coding) p
                           .getExtensionByUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity")
                           .getExtensionByUrl("ombCategory").getValue();

                if (raceOrEthnicity[i].equals(value1.getCode()) || raceOrEthnicity[i].equals(value2.getCode())) {
                    match = this.patientsByRaceOrEthnicity.get(i);
                    match.add(p.getIdElement().getIdPart());
                    if (--count == 0) {
                        break;
                    }
                }
            }
        }
    }

    private void readPatients() {

        String map[] = CsvResourceLoader.getMap("Patient");
        int counter[] = new int[1];
        CsvResourceLoader.createResources(
            Patient.class, DATA_URL + "patients.csv", map,
            p -> {
                if (++counter[0] % 100 == 0) {
                    System.out.print(".");
                    if (counter[0]/100 % 100 == 0) {
                        System.out.println();
                    }
                }
                String id = p.getIdElement().getIdPart();
                patientMap.put(id, p);
                patients.add(id);
                return true;
            }, null, null, 0);
    }

    private void getCovidEncounterData() {
        String map[] = CsvResourceLoader.getMap("Encounter");
        int counter[] = new int[3];
        System.out.println();
        CsvResourceLoader.createResources(
            Encounter.class, DATA_URL + "encounters.csv", map,
            enc -> {
                Patient p = patientMap.get(enc.getSubject().getReferenceElement().getIdPart());
                if ("840539006".equals(enc.getReasonCode().get(0).getCoding().get(0).getCode()) &&
                    "1505002".equals(enc.getType().get(0).getCoding().get(0).getCode())) {
                    /*
                     * Check for a COVID Encounter
                     * inpatient_ids = encounters[(encounters.REASONCODE == 840539006) & (encounters.CODE == 1505002)].PATIENT
                     */

                    if (p != null) {
                        // Compute patient age at time of encounter
                        int age = java.time.Period.between(
                            p.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            enc.getPeriod().getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        ).getYears();
                        int los = CaseSimulator.lengthInDays(enc.getPeriod());
                        p.setUserData("age", age);
                        p.setUserData("encounter", enc);
                        p.setUserData("los", los);


                        if (++counter[0] % 100 == 0) {
                            System.out.print(".");
                            if ((counter[0] + counter[1])/100 % 100 == 0) {
                                System.out.println();
                            }
                        }
                    }
                } else if ("305351004".equals(enc.getType().get(0).getCoding().get(0).getCode())) {
                    /*
                     * icu_ids = encounters[encounters.CODE == 305351004].PATIENT
                     * cp['icu_admit'] = cp.Id.isin(icu_ids)
                     */
                    if (p != null) {
                        Encounter inp = (Encounter) p.getUserData("encounter");
                        if (inp != null) {
                            if (p.getUserData("icu") != null) {
                                LOGGER.error("Multiple ICU Stays");
                            }
                            if (overlaps(inp.getPeriod(), enc.getPeriod())) {
                                p.setUserData("icu", enc);
                                if (++counter[1] % 100 == 0) {
                                    System.out.print("*");
                                    if ((counter[0] + counter[1])/100 % 100 == 0) {
                                        System.out.println();
                                    }
                                }

                            } else if (!overlaps(inp.getPeriod(), enc.getPeriod())){
//                                LOGGER.error("ICU stay {}-{} not overlapping or adjacent to COVID-19 encounter {}-{}",
//                                    enc.getPeriod().getStartElement().asStringValue(),
//                                    enc.getPeriod().getEndElement().asStringValue(),
//                                    inp.getPeriod().getStartElement().asStringValue(),
//                                    inp.getPeriod().getEndElement().asStringValue());
                            }
                        }
                    }
                }

                return true;
            }, "PATIENT", p -> { counter[2]++; return patientMap.containsKey(p); } , 0);
        /* vent_ids = procedures[procedures.CODE == 26763009].PATIENT
         * cp['ventilated'] = cp.Id.isin(vent_ids)
         */

        List<String> idsToRemove = patientMap.values().stream()
            .filter(p -> p.getUserData("age") == null)
            .map(p -> p.getIdElement().getIdPart()).collect(Collectors.toList());

        int total = patientMap.size();
        idsToRemove.forEach(key -> patientMap.remove(key));
        System.out.printf("\nInitial Patients: %d\nPatients without Hospital Encounters: %d\nTotal Hospitalized: %d\nTotal Encounters: %d\n",
            total, idsToRemove.size(), patientMap.size(), counter[2]);
        reset();
    }

    private boolean overlaps(Period period, Period period2) {
        Date    p1Start = new DateType(period.getStart()).getValue(),
                p2Start = new DateType(period2.getStart()).getValue(),
                p1End = new DateType(period.getEnd()).getValue();

        if (p2Start.equals(p1End)) {
            return true;
        }
        if (p2Start.after(p1Start) && p2Start.before(p1End)) {
            return true;
        }
        return false;
    }


    @Override
    public Collection<Patient> getAll() {
        return patientMap.values();
    }

    @Override
    public void reset() {
        patients.clear();
        patients.addAll(patientMap.keySet());
    }
}
