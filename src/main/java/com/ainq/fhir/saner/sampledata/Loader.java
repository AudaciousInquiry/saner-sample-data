package com.ainq.fhir.saner.sampledata;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load sample data
 * @author Keith W. Boone
 *
 *  Clears any previously loaded data.
 *
 *  Creates Location, Organization, Practitioner resources in a destination FHIR Server, tagging them
 *  as sample data.
 *
 *  Creates a dummy Location and Organization to associate with patients and other clinical sample data.
 *
 *  Creates a population of sample Patients assigned to the dummy Location and Organization:
 *      Merges SyntheticMass patients with addresses randomly selected from addresses-geojson, and assigns them
 *      as patients in the dummy location and organization.
 *
 *      Reports on the list of created patients.
 *
 *  Creates data resources associated with sample patients.
 *      Reports on the list of COVID-19 encounters associated with those patients.
 *
 */
public class Loader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);

    private static final AddressGenerator addressGenerator = new AddressGenerator();
    private static final LocationGenerator  locationGenerator = new LocationGenerator();
    /**
     * Create Location and Organization resources from Hospital-Locations.xml
     */
    public static void createLocationsAndOrganizations() {
        List<Location> l = new ArrayList<>(locationGenerator.getAll());
    }

 }

