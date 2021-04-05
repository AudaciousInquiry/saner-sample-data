package com.ainq.fhir.saner.sampledata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.ZipException;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * This class provides methods to support loading of FHIR Resources
 * from a CSV File.
 *
 * @author Keith W. Boone
 *
 */
public class CsvResourceLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvResourceLoader.class);
    static {
        String pkgs = System.getProperty("java.protocol.handler.pkgs", "");
        if (!pkgs.contains("com.ainq.fhir.saner.sampledata")) {
            if (pkgs.length() != 0) {
                pkgs += "|";
            }
            pkgs += "com.ainq.fhir.saner.sampledata";
            System.setProperty("java.protocol.handler.pkgs", pkgs);
        }
    }

    public static String PATIENT_MAP[] = {
        "id", "%Id",
        "name.given", "%FIRST",
        "name.family", "%LAST",
        "birthDate", "%BIRTHDATE",
        "deceased[x]", "%DEATHDATE",
        "gender", "mapGender(%GENDER)",
        "maritalStatus.coding.code", "%MARITAL",
        "maritalStatus.coding.system", "mapMarital(%MARITAL)",
        "extension('http://hl7.org/fhir/us/core/StructureDefinition/us-core-race').extension('ombCategory').valueCoding.code", "mapRace(%RACE)",
        "extension('http://hl7.org/fhir/us/core/StructureDefinition/us-core-race').extension('ombCategory').valueCoding.system", "urn:oid:2.16.840.1.113883.6.238",
        "extension('http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity').extension('ombCategory').valueCoding.code", "mapEthnicity(%ETHNICITY)",
        "extension('http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity').extension('ombCategory').valueCoding.system", "urn:oid:2.16.840.1.113883.6.238",
        "address.line", "%ADDRESS",
        "address.city", "%CITY",
        "address.state", "%STATE",
        "address.district", "%COUNTY",
        "address.postalCode", "%ZIP",
        "address.country", "USA",
        "address.extension('http://hl7.org/fhir/StructureDefinition/geolocation').extension('latitude').valueDecimal", "%LAT",
        "address.extension('http://hl7.org/fhir/StructureDefinition/geolocation').extension('longitude').valueDecimal", "%LON",
    };

    public static String PRACTITIONER_MAP[] = {
        "id", "%Id",
        "name.given", "mapFirst(%NAME)",
        "name.family", "mapLast(%NAME)",
        "gender", "mapGender(%GENDER)",
        "address.line", "%ADDRESS",
        "address.city", "%CITY",
        "address.state", "%STATE",
        "address.postalCode", "%ZIP",
        "address.country", "USA",
        "address.extension('http://hl7.org/fhir/StructureDefinition/geolocation').extension('latitude').valueDecimal", "%LAT",
        "address.extension('http://hl7.org/fhir/StructureDefinition/geolocation').extension('longitude').valueDecimal", "%LON",
        "qualification.code.text", "%SPECIALITY"
    };

    public static String ENCOUNTER_MAP[] = {
        "id", "%Id",
        "period.start", "%START",
        "period.end", "%STOP",
        "subject.reference", "mapPatient(%PATIENT)",
        "participant.type.coding.code", "PPRF",
        "participant.type.coding.system", "http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
        "participant.individual.reference", "mapProvider(%PROVIDER)",
        "class.code", "mapEncClass(%ENCOUNTERCLASS)",
        "class.system", "http://terminology.hl7.org/ValueSet/v3-ActEncounterCode",
        "type.coding.code", "%CODE",
        "type.coding.system", "http://snomed.info/sct",
        "reasonCode.coding.code", "%REASONCODE",
        "reasonCode.coding.system", "http://snomed.info/sct",
        "reasonCode.text", "%REASONDESCRIPTION",
    };

    public static String ALLERGY_MAP[] = {
        "id", "genId()",
        "onset[x]", "%START",
        "patient.reference", "mapPatient(%PATIENT)",
        "encounter.reference", "mapEncounter(%ENCOUNTER)",
        "code.coding.code", "%CODE",
        "code.coding.system", "http://snomed.info/sct",
        "code.text", "%DESCRIPTION"
    };

    public static String CONDITION_MAP[] = {
        "id", "genId()",
        "onset[x]", "%START",
        "subject.reference", "mapPatient(%PATIENT)",
        "encounter.reference", "mapEncounter(%ENCOUNTER)",
        "code.coding.code", "%CODE",
        "code.coding.system", "http://snomed.info/sct",
        "code.text", "%DESCRIPTION"
    };

    public static String IMMUNIZATION_MAP[] = {
        "id", "genId()",
        "occurence[x]", "%DATE",
        "patient.reference", "mapPatient(%PATIENT)",
        "encounter.reference", "mapEncounter(%ENCOUNTER)",
        "vaccineCode.coding.code", "%CODE",
        "vaccineCode.coding.system", "http://hl7.org/fhir/sid/cvx",
        "vaccineCode.text", "%DESCRIPTION"
    };

    public static String IMAGING_STUDY_MAP[] = {
        "id", "%Id",
        "started", "%DATE",
        "subject.reference", "mapPatient(%PATIENT)",
        "encounter.reference", "mapEncounter(%ENCOUNTER)",
        "modality.code", "%MODALITY_CODE",
        "modality.system", "http://dicom.nema.org/medical/dicom/current/output/chtml/part16/sect_CID_29.html",
        "modality.display", "%MODALITY_DESCRIPTION",
        "series.bodySite.code", "%BODYSITE_CODE",
        "series.bodySite.system", "http://snomed.info/sct",
        "series.bodySite.display", "%BODYSITE_DESCRIPTION",
        "series.instance.sopClass.code", "%SOP_CODE",
        "series.instance.sopClass.system", "http://dicom.nema.org/medical/dicom/current/output/chtml/part04/sect_B.5.html#table_B.5-1",
        "series.instance.sopClass.display", "%SOP_DESCRIPTION"
    };

    public static String PROCEDURE_MAP[] = {
        "id", "genId()",
        "performed[x]", "%DATE",
        "subject.reference", "mapPatient(%PATIENT)",
        "encounter.reference", "mapEncounter(%ENCOUNTER)",
        "code.coding.code", "%CODE",
        "code.coding.system", "http://snomed.info/sct",
        "code.text", "%DESCRIPTION",
        "reasonCode.coding.code", "%REASONCODE",
        "reasonCode.coding.system", "http://snomed.info/sct",
        "reasonCode.text", "%REASONDESCRIPTION"
    };

    public static String MEDICATION_MAP[] = {
        "id", "genId()",
        "effectivePeriod.start", "%START",
        "effectivePeriod.end", "%STOP",
        "subject.reference", "mapPatient(%PATIENT)",
        "context.reference", "mapEncounter(%ENCOUNTER)",
        "medicationCodeableConcept.coding.code", "%CODE",
        "medicationCodeableConcept.coding.system", "http://www.nlm.nih.gov/research/umls/rxnorm",
        "medicationCodeableConcept.text", "%DESCRIPTION",
        "reasonCode.coding.code", "%REASONCODE",
        "reasonCode.coding.system", "http://snomed.info/sct",
        "reasonCode.text", "%REASONDESCRIPTION"
    };

    public static String OBSERVATION_MAP[] = {
        "id", "genId()",
        "effective[x]", "%DATE",
        "subject.reference", "mapPatient(%PATIENT)",
        "encounter.reference", "mapEncounter(%ENCOUNTER)",
        "code.coding.code", "%CODE",
        "code.coding.system", "http://loinc.org",
        "code.text", "%DESCRIPTION",
        "valueString", "mapString(%VALUE)",
        "valueQuantity.value", "mapValue(%VALUE)",
        "valueQuantity.unit", "mapValue(%UNITS)",
        "valueQuantity.code", "mapValue(%UNITS)",
        "valueQuantity.system", "mapValue(http://unitsofmeasure.org)"
    };

    public static String[] getMap(String resourceType) {
        switch (resourceType) {
        case "Patient":             return PATIENT_MAP;
        case "Encounter":           return ENCOUNTER_MAP;
        case "Condition":           return CONDITION_MAP;
        case "AllergyIntolerance":  return ALLERGY_MAP;
        case "ImagingStudy":        return IMAGING_STUDY_MAP;
        case "MedicationStatement": return MEDICATION_MAP;
        case "Observation":         return OBSERVATION_MAP;
        case "Practitioner":        return PRACTITIONER_MAP;
        case "Procedure":           return PROCEDURE_MAP;
        default:                    return null;
        }
    }

    /**
     * Create resources from data found in CSV files located at the specified URL
     * @param theUrl        A URL pointing to the source of CSV data to extract resources from.
     * @param fieldMapping  A mapping of FHIR fields to values.
     */
    public static <T extends Resource> void createResources(Class<T> type, String theUrl, String fieldMapping[], Predicate<T> consumer, String field, Predicate<String> test, int max) {
        try {
            URL url = new URL(theUrl);
            URLConnection con = url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(false);
            con.connect();
            Constructor<T> constructor = type.getConstructor();
            try (InputStream s = con.getInputStream();
                 CSVReader r = new CSVReader(new InputStreamReader(s));
                ) {
                String fieldNames[] = null;
                String fieldData[];
                Map<String, Integer> indexes = new HashMap<>();

                fieldNames = r.readNext();
                for (int i = 0; i < fieldNames.length; i++) {
                    indexes.put(fieldNames[i], i);
                }

                while (true) {
                    try {
                        fieldData = r.readNext();
                        if (fieldData == null) {
                            break;
                        }
                        Map<String, String> feildMap = createMap(fieldNames, indexes, fieldData);
                        if (field == null || test.test(feildMap.get(field))) {
                            if (consumer.test(processRow(constructor.newInstance(), feildMap, fieldMapping)) == false) {
                                break;
                            }
                            if (--max == 0) {
                                return;
                            }
                        }
                    } catch (CsvValidationException e) {
                        LOGGER.error("CSV Error in {}", theUrl, e);
                    } catch (InstantiationException e) {
                        LOGGER.error("Reflection Error creating {}", type.getName(), e);
                    } catch (IllegalAccessException e) {
                        LOGGER.error("Access Error creating {}", type.getName(), e);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Construction Error creating {}", type.getName(), e);
                    } catch (InvocationTargetException e) {
                        LOGGER.error("Exception creating {}", type.getName(), e);
                    }
                }

            }  catch (ZipException e) {
                LOGGER.error("ZIP file format error in {}", theUrl, e);
            } catch (IOException e) {
                LOGGER.error("IO error reading {}", theUrl, e);
            } catch (CsvValidationException e) {
                LOGGER.error("CSV file format error reading {}", theUrl, e);
            }

        } catch (NoSuchMethodException e) {
            LOGGER.error("Constructor not found for {}", type.getName(), e);
        } catch (SecurityException e) {
            LOGGER.error("Security error accessing constructor for {}", type.getName(), e);
        } catch (IOException e) {
            LOGGER.error("Cannot open {}", theUrl, e);
        }
    }

    private static Map<String, String> createMap(String fieldNames[], Map<String, Integer> index, String[] fieldData) {
        Map<String, String> map = new AbstractMap<String, String>() {

            @Override
            public String get(Object key) {
                Integer pos = index.get(key);
                return pos != null && pos < fieldData.length ? fieldData[pos] : null;
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                return new AbstractSet<Entry<String, String>>() {

                    public Iterator<Entry<String, String>> iterator() {
                        return new Iterator<Entry<String, String>> () {
                            int position = 0;
                            @Override
                            public boolean hasNext() {
                                return position <  fieldNames.length;
                            }

                            @Override
                            public Entry<String, String> next() {
                                return new Entry<String, String> () {
                                    int pos = position++;
                                    @Override
                                    public String getKey() {
                                        return fieldNames[pos];
                                    }

                                    @Override
                                    public String getValue() {
                                        return fieldData[pos];
                                    }

                                    @Override
                                    public String setValue(String value) {
                                        throw new UnsupportedOperationException();
                                    }};
                            }

                        };
                    }


                    @Override
                    public int size() {
                        return fieldNames.length;
                    }

                };
            }
        };
        return map;
    }

    /**
     * Process a row from the CSV File, collecting data from each field and populating a new resource.
     * @param <T>   The type of resource to update.
     * @param resource  The resource to be updated.
     * @param indexes   A map from field name to field location.
     * @param fieldData
     * @param fieldMapping
     * @return The modified resource.
     */
    private static <T extends Resource> T processRow(T resource, Map<String, String> fieldData, String fieldMapping[]) {
        for (int i = 0; i < fieldMapping.length; i += 2) {
            String key = fieldMapping[i];
            String value = fieldMapping[i+1];
            PrimitiveType<?> t = null;
            boolean isVariable = false;
            try {
                if (value.startsWith("%")) {
                    String key1 = value.substring(1);
                    if (fieldData.containsKey(key1)) {
                        value = fieldData.get(key1);
                        isVariable = true;
                    } else {
                        throw new RuntimeException("Unknown field " + key1);
                    }
                }
                if (!StringUtils.isBlank(value)) {
                    if (!isVariable && value.contains("(")) {
                        String function = StringUtils.substringBefore(value, "(");
                        value = value.substring(function.length() + 1, value.length() - 1);
                        if (value.startsWith("%")) {
                            value = fieldData.get(value.substring(1));
                        }
                        value = mapCode(function, value, fieldData.get("TYPE"));
                    }
                    if (!StringUtils.isBlank(value)) {
                        t = getElement(key, resource);
                        t.setValueAsString(value);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Cannot set {}({}) to {}", key, t == null ? "unknown" : t.getClass().getSimpleName(), fieldMapping[i+1], ex);
                throw ex;
            }
        }
        return resource;
    }

    private static String mapCode(String function, String value, String type) {
        switch (function) {
        case "mapRace":
            return mapRace(value);
        case "mapGender":
            return mapGender(value);
        case "mapEthnicity":
            return mapEthnicity(value);
        case "mapMarital":
            return StringUtils.isBlank(value) ? "" :
                "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus";
        case "mapFirst":
            return StringUtils.substringBefore(value, " ");
        case "mapLast":
            return StringUtils.substringAfterLast(value, " ");
        case "mapPatient":
            return "Patient/" + value;
        case "mapEncounter":
            return "Encounter/" + value;
        case "mapProvider":
            return "Practitioner/" + value;
        case "genId":
            return UUID.randomUUID().toString();
        case "mapString":
            if ("text".equals(type)) {
                return value;
            }
            return null;
        case "mapValue":
            if (!"text".equals(type)) {
                return value;
            }
            return null;
        case "mapEncClass":
            return mapEncClass(value);
        default:
            throw new RuntimeException("Unknown function " + function);
        }
    }

    private static String mapEthnicity(String value) {
        switch (value) {
        case "hispanic":    return "2135-2";
        case "nonhispanic": return "2186-5";
        }
        return null;
    }

    private static String mapGender(String value) {
        switch (value) {
        case "M": case "m":
        case "male":    return "male";
        case "F": case "f":
        case "female": return "female";
        }
        return null;
    }

    private static String mapRace(String value) {
        switch (value) {
        case "native":  return "1002-5";
        case "asian":   return "2028-9";
        case "black":   return "2054-5";
        case "other":   return "2076-8";
        case "white":   return "2106-3";
        }
        return null;
    }

    private static String mapEncClass(String value) {
        switch (value) {
        case "ambulatory":  return "AMB";
        case "inpatient":   return "IMP";
        case "outpatient":  return "AMB";
        case "emergency":   return "EMER";
        case "urgentcare":  return "AMB";
        case "homehealth":  return "AMB";
        case "wellness":    return "AMB";
        }
        return null;
    }

    /**
     * Given a Field name and a resource, return the primitive type
     * pointed to by the field name.
     *
     * @param <T>   The type of resource provided.
     * @param field The name of field in the resource, with subfields separated by .
     * @param resource  The resource to extract data from.
     * @return  The primitive type referenced in key
     */
    private static <T extends Resource> PrimitiveType<?> getElement(String field, T resource) {
        String parts[] = field.split("\\.");
        List<String> newParts = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].contains("(")) {
                String value = "";
                for (; i < parts.length; i++) {
                    if (value.length() != 0) {
                        value += ".";
                    }
                    value += parts[i];
                    if (parts[i].endsWith(")")) {
                        break;
                    }
                }
                newParts.add(value);
            } else {
                newParts.add(parts[i]);
            }
        }

        parts = newParts.toArray(new String[newParts.size()]);
        Base b = resource;
        Element elem = null;
        String found = "";
        for (String part: parts) {
            elem = getProperty(b, part);
            if (elem == null) {
                String msg = String.format("Cannot find %s%s in %s", found, part, resource.fhirType());
                LOGGER.error(msg);
                throw new RuntimeException(msg);
            }
            b = elem;
            found += part + ".";
        }
        if (!(elem instanceof PrimitiveType)) {
            String msg = String.format("%s.%s is not a primitive type (%s)", resource.fhirType(), found, elem.fhirType());
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        return (PrimitiveType<?>) elem;
    }

    /**
     * Get the element referenced by a field.  If the property
     * @param b     The Resource or FHIR type to access data from
     * @param part  The name of the field to access. If part references a
     * @return  A FHIR data type representing the named field.
     */
    private static Element getProperty(Base b, String part) {
        if (part.startsWith("extension('")) {
            String url = part.substring(11, part.length() - 2);
            Extension ex = null;
            if (b instanceof DomainResource) {
                ex = ((DomainResource) b).getExtensionByUrl(url);
            } else {
                ex = ((Element) b).getExtensionByUrl(url);
            }
            if (ex == null) {
                ex = (Extension) ((IBaseHasExtensions) b).addExtension().setUrl(url);
            }
            return ex;
        }
        Property p = b.getNamedProperty(part);
        if (p == null) {
            /*
             * At this stage, we might be looking at something like Observation.valueQuantity.
             * But getNamedProperty won't return it, b/c we did something like create valueString
             * already.
             */
            return null;
        }
        if (!p.hasValues()) {
            String typeCode = p.getTypeCode();
            if (typeCode.length() > 0 && Character.isLowerCase(typeCode.charAt(0))) {
                switch (p.getTypeCode()) {
                case "date":
                    b.setProperty(part, new DateType());
                    break;
                case "dateTime":
                    b.setProperty(part, new DateTimeType());
                    break;
                case "instant":
                    b.setProperty(part, new InstantType());
                    break;
                case "id":
                    return ((Resource)b).getIdElement();
                case "uri":
                    b.setProperty(part, new UriType(""));
                    break;
                case "string": case "code":
                    b.setProperty(part, new StringType(""));
                    break;
                default:
                    if (isComposite(typeCode, part)) {
                        return (Element) b.addChild(part);
                    } else if (typeCode.contains("dateTime")) {
                        b.setProperty(part, new DateTimeType());
                    } else if (typeCode.contains("date")) {
                        b.setProperty(part, new DateType());
                    } else {
                        b.setProperty(part, new IntegerType(0));
                    }
                }
                return (Element) b.getNamedProperty(part).getValues().get(0);
            } else {
                return (Element) b.addChild(part);
            }
        }
        List<Base> values = p.getValues();
        return (Element) values.get(values.size() - 1);
    }

    /**
     * Return true if the field being looked for is a composite.
     *
     * @param typeCode  The typeCode values available for this element.
     * @param part  The specific field being looked for.
     * @return True if the type being sought is a composite.
     */
    private static boolean isComposite(String typeCode, String part) {
        for (String type: typeCode.split("\\|")) {
            if (part.endsWith(type.trim())) {
                return true;
            }
        }
        return false;
    }
}
