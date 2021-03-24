package com.ainq.fhir.saner.sampledata;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class LocationGenerator implements Generator<Location> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationGenerator.class);
    private static final IParser xp = FhirContext.forR4().newXmlParser();
    Map<String, Location> map = new HashMap<>();
    List<String> ids = new ArrayList<>();
    @Override
    public void initialize() {
        File f = null;
        URL url = null;
        try (ZipFile locations = new ZipFile(f = new File((url = LocationGenerator.class.getClassLoader().getResource("Locations.zip")).toURI()))) {
            Enumeration<? extends ZipEntry> e = locations.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                Resource r = (Resource) xp.parseResource(locations.getInputStream(entry));
                if (r instanceof Location) {
                    map.put(r.getIdElement().getIdPart(), (Location) r);
                    ids.add(r.getIdElement().getIdPart());
                } else if (r instanceof Organization) {
                    String id = "Loc-" + r.getIdElement().getIdPart();
                    Location l = map.get(id);
                    if (l != null) {
                        l.getManagingOrganization().setResource(r);
                    } else {
                        LOGGER.warn("Missing Location: {}", id);
                    }
                }
            }
        } catch (ZipException e1) {
            LOGGER.error("Exception reading Zip File: {}", f, e1);
        } catch (IOException e1) {
            LOGGER.error("Exception reading File: {}", f, e1);
        } catch (URISyntaxException e1) {
            LOGGER.error("Exception accessing resource: {}", url, e1);
        }
    }

    public Collection<Location> getAll() {
        return map.values();
    }

    @Override
    public Location generate(Map<String, String> properties) {
        int index = RANDOM.nextInt(ids.size());
        return map.get(ids.get(index));
    }

}
