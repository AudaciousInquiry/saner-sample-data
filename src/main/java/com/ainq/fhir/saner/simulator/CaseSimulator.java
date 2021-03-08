package com.ainq.fhir.saner.simulator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Location.LocationPositionComponent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * CaseSimulator is used to generate simulated data (individual or aggregate)
 * for hospital cases.
 *
 * @author Keith W. Boone
 *
 */
public class CaseSimulator {
    /** The hospitals for which this case is simulated. */
    private final Set<Hospital> hospitals = new TreeSet<Hospital>(Comparator.comparing(h -> h.getName()));
    {
        initializeHospitalsFromResource("CookCountyHospitals-Locations.xml");
    }
    /** Date Formatter for date comparison */
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    /** The starting and ending counts for cases */
    private int startingCases = 3992,
                endingCases = 2467,
                totalCases;
    /** Daily rate of change to apply to get from starting to ending case counts */
    private double dailyRateOfChange;
    /** Starting and ending dates for the simulation */
    private Date startDate, endDate;

    public CaseSimulator(Date start, Date end, int startingCases, int endingCases) {
        this.startDate = start;
        this.endDate = end;
        this.startingCases = startingCases;
        this.endingCases = endingCases;
        createCases(startingCases, start);
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

    private void adjustRateOfChange() {
        if (startDate == null || endDate == null) {
            return;
        }
        int days = daysDifference(startDate, endDate);
        dailyRateOfChange = ((double)(startingCases - endingCases))/days;
    }

    private static int daysDifference(Date start, Date end) {
        double daysDiff = (end.getTime() - start.getTime()) / (1000.0 * 60 * 60 * 24);
        return (int) Math.ceil(daysDiff);
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
        int totalBeds = hospitals.stream().collect(Collectors.summingInt(h -> h.getNonICUBeds()));

        Calendar cal = Calendar.getInstance();

        for (Hospital h: hospitals) {
            int numCasesToCreate = (h.getNonICUBeds() * startingCases) / totalBeds;
            for (int i = 0; i < numCasesToCreate; i++) {
                Case c = new Case(day, day.equals(startDate));
                cal.setTime(day);
                h.getCases().add(c);
            }
        }
    }

    public void processActivity(Date day) {
        // For each case in each hospital, if it's end date is today, remove it
        removeInactiveCases(day);
        int targetNumberOfCases = startingCases + (int)(daysDifference(startDate, day) * this.dailyRateOfChange);
        targetNumberOfCases = adjustForDayOfWeek(targetNumberOfCases, day);
        if (totalCases < targetNumberOfCases) {
            // Add more cases starting today.
            createCases(targetNumberOfCases - totalCases, day);
        }
    }

    private void removeInactiveCases(Date day) {
        String today = sdf.format(day);
        for (Hospital h: hospitals) {
            // Remove discharged cases.
            for (Iterator<Case> it = h.getCases().iterator(); it.hasNext();) {
                Case c = it.next();
                String caseEndDate = sdf.format(c.getEndDate());
                if (caseEndDate.compareTo(today) <= 0) {
                    it.remove();
                    totalCases--;
                    h.hospitalBedsUsed --;
                }
                if (c.hasICUStay()) {
                    if (sdf.format(c.getIcuStart()).equals(today)) {
                        h.icuBedsUsed++;
                    }
                    if (sdf.format(c.getIcuEnd()).equals(today)) {
                        h.icuBedsUsed--;
                    }
                }
            }
        }
    }

    int adjustForDayOfWeek(int value, Date date) {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        // Adjust target number of cases to day of week (to represent weekend variation)
        // Use a modified sin() waveform to adjust up or down, with the upper half occuring
        // during the week and the lower half over the weekend.
        double index[] = { 1.667, 0.1, 0.3, 0.5, 0.7, 0.9, 1.333 };
        double maxAdjustmentAmount = value / 40.0; // (+/- 2.5% of total value)
        double adjustmentAmount = maxAdjustmentAmount * Math.sin(index[dayOfWeek - Calendar.SUNDAY] * Math.PI);
        return value + (int) Math.round(adjustmentAmount);
    }

    private void initializeHospitalsFromResource(String name) {
        IParser xp = FhirContext.forR4().newXmlParser();
        Bundle b = xp.parseResource(Bundle.class, getClass().getClassLoader().getResourceAsStream(name));
        for (Bundle.BundleEntryComponent comp: b.getEntry()) {
            Location l = (Location) comp.getResource();

            Hospital h = new Hospital();
            hospitals.add(h);

            h.setName(l.getName());
            h.setHospitalBeds(
                b.castToInteger(l.getExtensionByUrl("http://test.sanerproject.org/beds").getValue()).getValue()
            );
            h.setIcuBeds(
                b.castToInteger(l.getExtensionByUrl("http://test.sanerproject.org/icubeds").getValue()).getValue()
            );

            LocationPositionComponent pos = l.getPosition();
            h.setLatitude(pos.getLatitude().doubleValue());
            h.setLongitude(pos.getLongitude().doubleValue());
        }
    }

    public static void main(String args[]) throws ParseException {
        if (args.length == 0) {
            args = new String[]{ "20210101", "20210131" };
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        CaseSimulator sim = new CaseSimulator(sdf.parse(args[0]), sdf.parse(args[1]),  3992, 2467);
        sim.report(sim.startDate);

        Calendar day = Calendar.getInstance();
        day.setTime(sim.startDate);
        while (day.getTime().compareTo(sim.endDate) < 0) {
            day.add(Calendar.DATE, 1);
            sim.processActivity(day.getTime());
            sim.report(day.getTime());
        }
    }
    private void report(Date day) {

    }
}
