package com.ainq.fhir.saner.simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Location;

public class Hospital {
    private static final boolean USE_UUID = false;
    private static int hospitalCount = 0;

    /** Cases assigned to this facility */
    private List<Case> currentCases = new ArrayList<>(), allCases = new ArrayList<>();
    /** Name used for the hospital location */
    private String  name;
    /** Identifier used for the hospital location */
    private String  identifier;
    /** Total beds and ventilators for the facility */
    private int icuBeds, hospitalBeds, emergencyBeds, ventilators;
    private Location location;

    public Hospital() {
        identifier = generateIdentifier();
    }

    public Hospital(Location l) {
        location = l;
        identifier = l.getId();
        name = l.getName();
        icuBeds = ((IntegerType) l.getExtensionByUrl("http://test.sanerproject.org/icubeds").getValue()).getValue();
        hospitalBeds = ((IntegerType) l.getExtensionByUrl("http://test.sanerproject.org/beds").getValue()).getValue();;
    }

    private static String generateIdentifier() {
        return USE_UUID ? UUID.randomUUID().toString() : Integer.toString(++hospitalCount);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the icuBeds
     */
    public int getIcuBeds() {
        return icuBeds;
    }

    /**
     * @param icuBeds the icuBeds to set
     */
    public void setIcuBeds(int icuBeds) {
        this.icuBeds = icuBeds;
    }

    /**
     * @return the hospitalBeds
     */
    public int getHospitalBeds() {
        return hospitalBeds;
    }

    /**
     * @param hospitalBeds the hospitalBeds to set
     */
    public void setHospitalBeds(int hospitalBeds) {
        this.hospitalBeds = hospitalBeds;
    }

    /**
     * @return the emergencyBeds
     */
    public int getEmergencyBeds() {
        return emergencyBeds;
    }

    /**
     * @param emergencyBeds the emergencyBeds to set
     */
    public void setEmergencyBeds(int emergencyBeds) {
        this.emergencyBeds = emergencyBeds;
    }

    /**
     * @return the ventilators
     */
    public int getVentilators() {
        return ventilators;
    }

    /**
     * @param ventilators the ventilators to set
     */
    public void setVentilators(int ventilators) {
        this.ventilators = ventilators;
    }

    /**
     * @return the list of current cases
     */
    public List<Case> getCases() {
        return Collections.unmodifiableList(currentCases);
    }

    /**
     * @return the list of all cases that occured at the hospital
     */
    public List<Case> getAllCases() {
        return Collections.unmodifiableList(allCases);
    }

    /**
     * Add a case
     * @param theCase The case to add.
     * @param day
     */
    public void addCase(Case theCase) {
        currentCases.add(theCase);
        allCases.add(theCase);
    }

    /**
     * Remove a case
     * @param theCase The case to remove.
     * @param day
     */
    public void removeCase(Case theCase) {
        currentCases.remove(theCase);
    }

    /**
     * @param day The day to check at.
     * @return the icuBedsUsed
     */
    public int getIcuBedsUsed(Date day) {
        String today = CaseSimulator.SDF.format(day);
        return (int) currentCases.stream()
            .filter(
                c -> c.hasICUStay() &&
                CaseSimulator.SDF.format(c.getIcuStart()).compareTo(today) >= 0)
            .count();
    }

    /**
     * @return the number of hospital beds used
     */
    public int getHospitalBedsUsed() {
        return currentCases.size();
    }

    /**
     * @return the location
     */
    public Location getLocation() {
        return location;
    }
}
