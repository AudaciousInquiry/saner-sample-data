package com.ainq.fhir.saner.simulator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.BaseDateTimeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ainq.fhir.saner.sampledata.AddressGenerator;
import com.ainq.fhir.saner.sampledata.CsvResourceLoader;
import com.ainq.fhir.saner.sampledata.Generator;
import com.ainq.fhir.saner.sampledata.LocationGenerator;
import com.ainq.fhir.saner.sampledata.PatientGenerator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.FhirTerser;

/**
 * CaseSimulator is used to generate simulated data (individual or aggregate)
 * for hospital cases.
 *
 * @author Keith W. Boone
 *
 */
public class CaseSimulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseSimulator.class);
    public static final String  HSA = "http://terminology.hl7.org/codesystem/dartmouthatlas/HSA",
                                HRR = "http://terminology.hl7.org/codesystem/dartmouthatlas/HRR",
                                HSLOC = "https://www.cdc.gov/nhsn/cdaportal/terminology/codesystem/hsloc.html";
    private static final double RANDOM_VARIANCE = 0.05,
                                DAILY_VARIANCE = 0.15;

    /** The hospitals for which this case is simulated. */
    private final Set<Hospital> hospitals = new TreeSet<Hospital>(Comparator.comparing(h -> h.getName()));

    /** Location generator (for hospitals) */
    private static Generator<Location> locationGenerator;

    /** Patient Generator */
    private static Generator<Patient> patientGenerator;

    /** Generator for other data elements (e.g., risks, comorbidities, procedures, et cetera) */
    private static Generator<Address> addressGenerator;

    /** Date Formatter for date comparison */
    final static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");
    private static final File DATA_FOLDER = new File(".", "hospitalData");
    private static PrintWriter OUT = null;

    private static final Set<String> patients = new HashSet<>();

    /** The starting and ending counts for cases */
    private int startingCases = 3992,
                endingCases = 2467;
    /** Daily rate of change to apply to get from starting to ending case counts */
    private double dailyRateOfChange;
    /** Starting and ending dates for the simulation */
    private Date startDate, endDate;
    /** Used to find data fields of a given type in resources */
    private FhirTerser terser = new FhirTerser(FhirContext.forR4());
    /** For serializing the output data */
    private IParser parser = FhirContext.forR4().newJsonParser().setPrettyPrint(false);


    /**
     * Create a case simulator for the given time period that will generate
     * cases from start date to end date, with startingCases initial cases, and finishing
     * with endingCases when finished.
     *
     * The shape of the cases is a simple linear interpolation from start to end.
     *
     * @param start The starting date for the simulation.
     * @param end   The ending date for the simulation.
     * @param startingCases The initial number of cases at the start.
     * @param endingCases   The number of cases at the end of the simulation.
     */
    public CaseSimulator(Date start, Date end, int startingCases, int endingCases) {
        this.startDate = start;
        this.endDate = end;
        this.startingCases = startingCases;
        this.endingCases = endingCases;
        adjustRateOfChange();
        initializeHospitals();
        createCases(startingCases, start);
    }

    /**
     * @return the locationGenerator
     */
    public static Generator<Location> getLocationGenerator() {
        return locationGenerator;
    }
    /**
     * @param locationGenerator the locationGenerator to set
     */
    public static void setLocationGenerator(Generator<Location> locationGenerator) {
        CaseSimulator.locationGenerator = locationGenerator;
    }

    /**
     * @return the patientGenerator
     */
    public static Generator<Patient> getPatientGenerator() {
        return patientGenerator;
    }
    /**
     * @param patientGenerator the patientGenerator to set
     */
    public static void setPatientGenerator(Generator<Patient> patientGenerator) {
        CaseSimulator.patientGenerator = patientGenerator;
    }

    /**
     * @return the addressGenerator
     */
    public static Generator<Address> getAddressGenerator() {
        return addressGenerator;
    }
    /**
     * @param addressGenerator the AddressGenerator to set
     */
    public static void setAddressGenerator(Generator<Address> addressGenerator) {
        CaseSimulator.addressGenerator = addressGenerator;
    }
    /**
     * @return the startingCount
     */
    public int getStartingCount() {
        return startingCases;
    }

    /**
     * @param startingCount the startingCount to set
     */
    public void setStartingCount(int startingCount) {
        this.startingCases = startingCount;
    }
    /**
     * @return the endingCount
     */
    public int getEndingCount() {
        return endingCases;
    }
    /**
     * @param endingCount the endingCount to set
     */
    public void setEndingCount(int endingCount) {
        this.endingCases = endingCount;
    }

    /**
     * @return the sum of case counts for each hospital.
     */
    public int getTotalCases() {
        return hospitals.stream().collect(Collectors.summingInt(h -> h.getHospitalBedsUsed()));
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
        adjustRateOfChange();
    }
    /**
     * @return the endDate
     */
    public Date getEndDate() {
        return endDate;
    }
    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        adjustRateOfChange();
    }

    /**
     * Adjust the rate of change given the start and end date, and the
     * starting and ending number of cases.
     */
    private void adjustRateOfChange() {
        if (startDate == null || endDate == null) {
            return;
        }
        int days = lengthInDays(startDate, endDate);
        dailyRateOfChange = ((double)(endingCases - startingCases))/days;
    }

    public static int lengthInDays(Date start, Date end) {
        return (int) Duration.between(
            start.toInstant().atZone(ZoneId.systemDefault()),
            end.toInstant().atZone(ZoneId.systemDefault())).toDays();
    }

    public static int lengthInDays(DateTimeType start, DateTimeType end) {
        return lengthInDays(start.getValue(), end.getValue());
    }

    public static int lengthInDays(Period p) {
        return lengthInDays(p.getStart(), p.getEnd());
    }

    /**
     * @return the hospitals
     */
    public Set<Hospital> getHospitals() {
        return hospitals;
    }

    /**
     * Create the initial set of cases for the hospitals.
     */
    public void createCases(int total, Date day) {
        // For each hospital, create the initial cases.
        // The initial number of cases is set to startingCount to ensure the appropriate start value.
        int totalBeds = hospitals.stream().collect(Collectors.summingInt(h -> h.getHospitalBeds()));
        boolean isInitial = day.equals(startDate);

        Calendar cal = Calendar.getInstance();
        for (Hospital h: hospitals) {

            // Then determine number to create.
            int numCasesToCreate = (h.getHospitalBeds() * total) / totalBeds;
            for (int i = 0; i < numCasesToCreate; i++) {
                if (patients.size() == 3980) {
                    LOGGER.info("Got here!");
                }
                Case c = new Case(day, isInitial);
                if ("21a35eab-cafe-49df-917d-5ceee14f223b".equals(c.getPatientId())) {
                    LOGGER.info("Got here: {}", parser.encodeResourceToString(c.getPatient()));
                }
                if (!patients.add(c.getPatientId())) {
                    LOGGER.warn("Duplicate Patient Added");
                }
                cal.setTime(day);
                h.addCase(c);
            }
            // Recompute total and totalBeds to adjust for rounding
            // errors.
            total -= numCasesToCreate;
            totalBeds -= h.getHospitalBeds();
            LOGGER.debug("Hospital: {}\tCases Added: {}", h.getName(), numCasesToCreate);
        }
    }

    public void processActivity(Date day) {
        // For each case in each hospital, if it's end date is today, remove it
        int totalCases = getTotalCases();
        int netChange = -removeInactiveCases(day);

        // Compute the target number of cases for the given day
        int targetNumberOfCases = startingCases + (int)(lengthInDays(startDate, day) * this.dailyRateOfChange);
        // Adjust the number of cases based on the day of the week
        targetNumberOfCases = adjustForDayOfWeek(targetNumberOfCases, day);
        totalCases = getTotalCases();

        // If more cases are needed
        if (totalCases < targetNumberOfCases) {
            // Add more cases starting today.
            createCases(targetNumberOfCases - totalCases, day);
            netChange += targetNumberOfCases - totalCases;
        }
        LOGGER.info("Net Change: {}", netChange);
    }

    /**
     * Remove inactive cases for the given day
     * @param day   The day for which to remove cases
     * @return  The number of cases removed.
     */
    private int removeInactiveCases(Date day) {
        String today = SDF.format(day);
        int total = 0;

        // For each hospital
        for (Hospital h: hospitals) {
            List<Case> toRemove = new ArrayList<>();

            // For each hoospital case
            for (Iterator<Case> it = h.getCases().iterator(); it.hasNext();) {
                Case c = it.next();
                // If the end date is less than or equal to today
                if (SDF.format(c.getEndDate()).compareTo(today) <= 0) {
                    // Add the case to the list of cases to remove.
                    toRemove.add(c);
                }
            }
            // Remove each of the identified cases
            toRemove.forEach(c -> h.removeCase(c));
            total += toRemove.size();
            LOGGER.debug("Hospital: {}\tCases Removed: {}", h.getName(), toRemove.size());
        }
        return total;
    }

    /**
     * Adjust the number of cases based on the day of the week.
     * @param value The original target number of cases
     * @param date  The date being adjusted for
     * @return  The new target number of cases based on day oof week.
     */
    int adjustForDayOfWeek(int value, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        // Adjust target number of cases to day of week (to represent weekend variation)
        // Use a waveform to adjust up or down, with the upper half occuring during the
        // week and the lower half over the weekend.
        double index[] = { -0.85, 0.31, 0.81, 1.00, 0.81, 0.31, -0.85 };
        double maxAdjustmentAmount = value * DAILY_VARIANCE;
        double adjustmentAmount = maxAdjustmentAmount * index[dayOfWeek - Calendar.SUNDAY];

        // Add some random variance
        adjustmentAmount += (value * RANDOM_VARIANCE) * (Generator.RANDOM.nextFloat() - 0.5); // +/- random 2.5%


        return value + (int) Math.round(adjustmentAmount);
    }

    /**
     * Initial hospitals from a location generator
     */
    private void initializeHospitals() {
        Collection<Location> list = locationGenerator.getAll();
        for (Location l: list) {
            hospitals.add(new Hospital(l));
        }
    }

    /**
     * Run a simulation
     * @param args  The start and stop date
     * @throws ParseException
     * @throws IOException
     */
    public static void main(String args[]) throws ParseException, IOException {
        if (args.length == 0) {
            args = new String[]{ "20210101", "20210131", "3992", "2476" };
        } else if (args.length < 4) {
            System.out.println("Usage: CaseSimulator startDate stopDate startingCaseCount endingCaseCount");
            System.exit(1);
        }
        // Clean out an existing, or create a new storage location.
        if (DATA_FOLDER.exists()) {
            FileUtils.cleanDirectory(DATA_FOLDER);
        } else {
            DATA_FOLDER.mkdirs();
        }
        OUT = new PrintWriter(new File(DATA_FOLDER, "report.txt"));

        // Create and initialize data generators
        LocationGenerator l = new LocationGenerator();
        l.initialize();
        PatientGenerator p  = new PatientGenerator();
        p.initialize();
        AddressGenerator a = new AddressGenerator();
        a.initialize();
        CaseSimulator.setLocationGenerator(l);
        CaseSimulator.setPatientGenerator(p);
        CaseSimulator.setAddressGenerator(a);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        CaseSimulator sim = new CaseSimulator(sdf.parse(args[0]), sdf.parse(args[1]),  Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        sim.report(sim.startDate);

        Calendar day = Calendar.getInstance();
        day.setTime(sim.startDate);
        while (day.getTime().compareTo(sim.endDate) < 0) {
            day.add(Calendar.DATE, 1);
            sim.processActivity(day.getTime());
            sim.report(day.getTime());
        }
        sim.collectAndWriteClinicalData(DATA_FOLDER);
    }

    /**
     * For each case, collect the clinical data associated with the patient in it,
     * and write it to a file associated with the patient.
     * @throws IOException  If there is an error writing the data.
     */
    private void collectAndWriteClinicalData(File storageLocation) throws IOException {

        Set<String> allPatients = new HashSet<>();

        // For each hospital
        int totalPatients = 0;
        for (Hospital h: hospitals) {
            // Create a folder for the cases for a given hospital location
            File hDir = new File(storageLocation, h.getLocation().getIdElement().getIdPart());
            if (!hDir.exists()) {
                hDir.mkdirs();
            }

            // For all cases that occurred at the hospital
            for (Case c: h.getAllCases()) {
                // Get the matching patient
                String patientId = c.getPatient().getIdElement().getIdPart();
                allPatients.add(patientId);
                Patient p = c.getPatient();
                //getPatientGenerator().generate(Collections.singletonMap("id", c.getPatientId()));

                // Set the file location for the data for this patient.
                File destination = new File(hDir, patientId + ".ndjson");
                p.setUserData("file", destination);

                // Select an address for the patient appropriate to the location
                adjustPatientAddressForLocation(h.getLocation(), p);

                // Write the patient record to the file
                writeData(p, p);
            }
            printf("%-64s%6d\n", h.getName(), h.getAllCases().size());
            totalPatients += h.getAllCases().size();
        }
        printf("%-64s%6d %6d\n", "Total", allPatients.size(), totalPatients);

        // Collect and store additional clinical data for each patient.
        storeResourcesForPatients(Encounter.class, "encounters.csv", allPatients);
        storeResourcesForPatients(Condition.class, "conditions.csv", allPatients);
        storeResourcesForPatients(AllergyIntolerance.class, "allergies.csv", allPatients);
        storeResourcesForPatients(ImagingStudy.class, "imaging_studies.csv", allPatients);
        storeResourcesForPatients(MedicationStatement.class, "medications.csv", allPatients);
        storeResourcesForPatients(Observation.class, "observations.csv", allPatients);
        storeResourcesForPatients(Procedure.class, "procedures.csv", allPatients);
    }

    /**
     * Given a hospital location and a patient, set the patient address such
     * that 50% of patients are within the Hospital Service Area, and 100%
     * are within the Hospital Referral Region.
     *
     * @param hospitalLocation  The location of the hospital associated with the patient.
     * @param p The patient
     */
    private void adjustPatientAddressForLocation(Location hospitalLocation, Patient p) {
        String zip = hospitalLocation.getAddress().getPostalCode();

        Address newLoc = getAddressGenerator().generate(Collections.singletonMap("zip", zip));
        p.getAddress().clear();
        p.getAddress().add(newLoc);
    }

    /**
     * Store the clinical resources associated with the patient.
     * @param <T>   The resource type.
     * @param type  The class representing the resource type.
     * @param file  The file where the resource is loaded.
     * @param patients  The patients for whom data should be stored.
     */
    private <T extends Resource> void storeResourcesForPatients(Class<T> type, String file, Set<String> patients) {
        int count[] = new int[2];
        printf("Reading %s Resources\n", type.getSimpleName());
        Set<String> foundPatients = new HashSet<String>();
        CsvResourceLoader.createResources(
            type, PatientGenerator.DATA_URL + file, CsvResourceLoader.getMap(type.getSimpleName()),
            r -> {
                Reference pat = getPatientReference(r);
                int scale = (r instanceof Observation) ? 1000 : 100;
                if (pat != null) {
                    Patient patient = getPatientByReference(pat);
                    if (patient != null) {
                        try {
                            writeData(patient, r);
                            foundPatients.add(pat.getReferenceElement().getIdPart());
                        } catch (DataFormatException | IOException e) {
                            LOGGER.error("Unexcpected exception writing resource {}", r.getId(), e);
                        }
                        updateCountAndStatus(count, scale);
                    } else {
                        LOGGER.error("Could not find {}", pat);
                    }
                } else {
                    LOGGER.error("No Patient Reference in {}", r.getId());
                }
                return true;
            }, "PATIENT", p -> {
                count[1]++;
                return patients.contains(p);
            }, 0);
        println();
        printf("Selected/Total %s records: %d/%d\n", type.getSimpleName(), count[0], count[1]);
        printf("Selected/Total patients: %d/%d\n", foundPatients.size(), patients.size());
    }

    /**
     * Get the reference to the patient for the provided clinical resource.
     * @param r The resource to get the patient from.
     * @return  A reference to the patient.
     */
    private Reference getPatientReference(Resource r) {
        Reference pat = null;
        switch (r.fhirType()) {
        case "Encounter":           pat = ((Encounter)r).getSubject(); break;
        case "Condition":           pat = ((Condition)r).getSubject(); break;
        case "AllergyIntolerance":  pat = ((AllergyIntolerance)r).getPatient(); break;
        case "ImagingStudy":        pat = ((ImagingStudy)r).getSubject(); break;
        case "MedicationStatement": pat = ((MedicationStatement)r).getSubject(); break;
        case "Observation":         pat = ((Observation)r).getSubject(); break;
        case "Procedure":           pat = ((Procedure)r).getSubject(); break;
        }
        return pat;
    }

    /**
     * Update the referenced count variable, and report status
     * every scale events.
     * @param count The count to update.
     * @param scale The scale for status updates.
     */
    private static void updateCountAndStatus(int[] count, int scale) {
        if ((++count[0]) % scale == 0) {
            printf(".");
            if (count[0]/scale % 100 == 0) {
                println();
            }
        }
    }

    /**
     * Get patient by reference.
     * @param pat   The referenced patient.
     * @return  The matching patient from the Patient generator
     */
    private Patient getPatientByReference(Reference pat) {
        String patId = pat.getReferenceElement().getIdPart();
        Patient patient = getPatientGenerator().generate(
                Collections.singletonMap("id", patId)
            );
        return patient;
    }

    /**
     * Write a resource record for a given patient.
     * @param patient   The patient whose data is being writteen
     * @param r The resource to store
     * @throws DataFormatException  If there is an error encoding the resource
     * @throws IOException  If there is an error writing the string to the file.
     */
    private void writeData(Patient patient, Resource r) throws DataFormatException, IOException {
        // Get the date offset for patient related events
        int dateOffset = patient.getUserInt("offset");
        Calendar cal = Calendar.getInstance();
        // Get the file where this patient's data is to be written.
        File f = (File) patient.getUserData("file");

        // If this resource is an encounter, see if it's "the COVID-19 encounter"
        if (r instanceof Encounter) {
            Encounter   enc = (Encounter)patient.getUserData("encounter"),
                        icu = (Encounter)patient.getUserData("icu"),
                        encounter = (Encounter) r;

            String eid = r.getIdElement().getIdPart();
            // If it's the initial COVID-19 encounter
            if (eid.equals(enc.getIdElement().getIdPart())) {
                // Adjust bed locations (e.g., ICU vs. Medical Ward) and combine icu stay
                // with initial encounter
                adjustEncounterLocationsAndDates(enc, icu, encounter);
            } else if (icu != null && eid.equals(icu.getIdElement().getIdPart())) {
                // Don't report ICU Admissions separately, as these will be merged with the
                // main encounter as different locations.
                return;
            }
        }
        for (BaseDateTimeType t: terser.getAllPopulatedChildElementsOfType(r, BaseDateTimeType.class)) {
            // Shift events in time to match the encounter date
            cal.setTime(t.getValue());
            cal.add(Calendar.DATE, dateOffset);
            t.setValue(cal.getTime());
        }
        FileUtils.writeStringToFile(f, parser.encodeResourceToString(r) + "\n", StandardCharsets.UTF_8, true);
    }

    /**
     * Given an encounter with inpatient stay defined by enc, and an optional icu stay defined by icu,
     * set the codes for the locations and dates of stay for the encounter.
     * @param enc   The non-ICU portion of the inpatient stay
     * @param icu   The ICU portion of the inpatient stay (if present)
     * @param encounter The encounter to adjust
     */
    private void adjustEncounterLocationsAndDates(Encounter enc, Encounter icu, Encounter encounter) {
        EncounterLocationComponent comp = encounter.addLocation();
        comp.setLocation(new Reference().setDisplay("Hospital Bed"));
        comp.setPhysicalType(new CodeableConcept().addCoding(
            new Coding(HSLOC, "1060-3", "Medical Ward")));
        // Use copies of the period so that the original period is not later modified.
        comp.setPeriod(enc.getPeriod().copy());
        if (icu != null) {
            comp = encounter.addLocation();
            comp.setLocation(new Reference().setDisplay("ICU Bed"));
            comp.setPhysicalType(new CodeableConcept().addCoding(
                new Coding(HSLOC, "1027-2", "Medical Critical Care")));
            // Use copies of the period so that the original period is not later modified.
            comp.setPeriod(icu.getPeriod().copy());
            encounter.getPeriod().setEnd(icu.getPeriod().getEnd());
        }
    }

    /**
     * Report on hospital and ICU bed utization for each hospital on the given date.
     * This data can be used to validate a measure counting hospital and ICU Bed Utilization
     * @param day   The date for which the report is generated.
     */
    private void report(Date day) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String reportDate = sdf.format(day);
        printf("--------%s---------\n", reportDate);
        for (Hospital h: hospitals) {
            printf("%s\t%s\t%d\t%d\n", reportDate, h.getName(), h.getHospitalBedsUsed(), h.getIcuBedsUsed(day));
        }
    }

    private static void println() {
        System.out.println();
        OUT.println();
    }
    private static void printf(String fmt, Object ... args) {
        System.out.printf(fmt, args);
        OUT.printf(fmt, args);
    }
}
