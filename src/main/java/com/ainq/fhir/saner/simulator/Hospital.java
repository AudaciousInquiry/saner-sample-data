package com.ainq.fhir.saner.simulator;

import java.util.List;
import java.util.UUID;

public class Hospital {
    private static final boolean USE_UUID = false;
    private static int hospitalCount = 0;
    /** Geocoordinates used for the hospital location */
    private double longitude, latitude;
    /** Name used for the hospital location */
    private String  name;
    /** Identifier used for the hospital location */
    private String  identifier;
    /** Total beds and ventilators for the facility */
    private int icuBeds, hospitalBeds, emergencyBeds, ventilators;
    int icuBedsUsed;
    int hospitalBedsUsed;

    public Hospital() {
        identifier = generateIdentifier();
    }

    private static String generateIdentifier() {
        return USE_UUID ? UUID.randomUUID().toString() : Integer.toString(++hospitalCount);
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude the longitude to set
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude the latitude to set
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
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
     * @return the nonICUBeds
     */
    public int getNonICUBeds() {
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
     * @return the cases
     */
    public List<Case> getCases() {
        return cases;
    }

    /**
     * @param cases the cases to set
     */
    public void setCases(List<Case> cases) {
        this.cases = cases;
    }

    /** Cases assigned to this facility */
    private List<Case> cases;

}
