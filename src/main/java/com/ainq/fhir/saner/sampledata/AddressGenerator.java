package com.ainq.fhir.saner.sampledata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.hl7.fhir.r4.model.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AddressGenerator implements Generator<Address> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressGenerator.class);

    private static RandomAccessFile addresses;
    private static List<Long> addressPositions = new ArrayList<>();
    private static Map<String, HospitalRegion> hsaMap = new HashMap<>();
    private static Map<String, List<HospitalRegion>> hsaZips = new HashMap<>();
    private static Map<String, List<HospitalRegion>> hrrZips = new HashMap<>();

    static {
        File f = null;
        try {
            addresses = new RandomAccessFile(f = File.createTempFile("addresses", "geojson"), "rw");
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: {}", f, e);
            throw new RuntimeException("Cannot initialize AddressGenerator", e);
        } catch (IOException e) {
            LOGGER.error("Cannot read file: {}", f, e);
            throw new RuntimeException("Cannot initialize AddressGenerator", e);
        }
    }

    private static JsonParser parser = new JsonParser();

    public static class HospitalRegion implements Comparable<HospitalRegion> {
        String hsa;
        String hrr;
        List<Long>  positions = new ArrayList<>();
        public HospitalRegion(String hsa, String hrr) {
            this.hsa = hsa;
            this.hrr = hrr;
        }

        @Override
        public int compareTo(HospitalRegion r2) {
            int comp = hrr.compareTo(r2.hrr);
            if (comp != 0) return comp;
            return hsa.compareTo(r2.hsa);
        }

        public int hashCode() {
            return hsa.hashCode() * 31 + hrr.hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof HospitalRegion) {
                return compareTo((HospitalRegion) o) == 0;
            }
            return false;
        }
    }

    public AddressGenerator() {
        initialize();
    }

    @Override
    public void initialize() {
        synchronized (addressPositions) {
            if (addressPositions.size() == 0) {
                loadHospitalRegions();
                loadAddresses();
            }
        }
    }

    @Override
    public Address generate(Map<String, String> properties) {
        // Choose a random default address.
        int selector = RANDOM.nextInt(addressPositions.size());
        long position = addressPositions.get(selector);

        // Get the hospital zip code property
        String value = properties == null ? null : properties.get("zip");
        HospitalRegion hr = null;
        List<HospitalRegion> l = null;

        // If there is a hospital zip code
        if (value != null && (hr = hsaMap.get(value)) != null) {
            // Get zips for HSA (50% of the time) or HRR (remaining 50%)
            if (RANDOM.nextBoolean()) {
                l = hsaZips.get(hr.hsa);
            } else {
                l = hrrZips.get(hr.hrr);
            }
            position = getRegionalPosition(l, position);
        } else if ((value = properties == null ? null : properties.get("hsa")) != null &&
            (l = hrrZips.get(value)) != null) {
            position = getRegionalPosition(l, position);
        } else if ((value = properties == null ? null : properties.get("hrr")) != null &&
            (l = hrrZips.get(value)) != null) {
            position = getRegionalPosition(l, position);
        }
        return getGeoJsonAsAddress(getRandomAddress(position));
    }

    /**
     * Select an address from among those addresses within a given
     * list of zip codes.
     * @param l The list of positions at given zip codes.
     * @param position  The current random position.
     * @return  An address within the region.
     */
    private long getRegionalPosition(List<HospitalRegion> l, long position) {
        int selector;
        int maxValue = l.stream().collect(Collectors.summingInt(z -> z.positions.size()));
        selector = RANDOM.nextInt(maxValue);
        for (HospitalRegion r: l) {
            if (selector < r.positions.size()) {
                position = r.positions.get(selector);
                break;
            } else {
                selector -= r.positions.size();
            }
        }
        return position;
    }

    private static String getAddressAsGeoJson(Address address) {
        JsonObject properties = new JsonObject();
        if (address.hasLine()) {
            String line = address.getLine().get(0).asStringValue();
            String number = StringUtils.substringBefore(line, " ");
            String street = StringUtils.substringAfter(line, " ");
            if (number.matches("^[1-9][0-9]+[A-Za-z]*$")) {
                properties.addProperty("number", number);
            } else {
                properties.addProperty("number", "");
                street = line;
            }
            properties.addProperty("street", street);
        } else {
            properties.addProperty("number", "");
            properties.addProperty("street", "");
        }
        properties.addProperty("city", address.hasCity() ? address.getCity() : "");
        properties.addProperty("region", address.hasState() ? address.getState() : "");
        properties.addProperty("postcode", address.hasPostalCode() ? address.getPostalCode() : "");
        JsonObject geo = new JsonObject();
        geo.add("properties", properties);
        return geo.toString().replaceAll("\\s+", " ");
    }

    public static Address getGeoJsonAsAddress(String geojson) {
        // {"type":"Feature","properties":{"hash":"eb849e1a7384a62f","number":"2730","street":"CENTRAL ST","unit":"","city":"EVANSTON","district":"","region":"IL","postcode":"60201","id":""},"geometry":{"type":"Point","coordinates":[-87.7121846,42.0642026]}}
        try {
            JsonObject addr = parser.parse(geojson).getAsJsonObject().get("properties").getAsJsonObject();
            Address address = new Address();
            address.addLine((addr.get("number").getAsString() + " " + addr.get("street").getAsString()).trim());
            address.setCity(addr.get("city").getAsString());
            address.setState(addr.get("region").getAsString());
            if (addr.get("postcode") != null) {
                address.setPostalCode(addr.get("postcode").getAsString());
            }
            return address;
        } catch (JsonSyntaxException jsex) {
            LOGGER.error("{} generates {}", geojson, jsex, jsex);
            throw jsex;
        }
    }

    public static String getRandomAddress(long position) {
        byte data[] = new byte[(int) (position & 0x0FFFF)];
        try {
            addresses.seek(position >> 16);
            addresses.read(data);
            return new String(data);
        } catch (IOException e) {
            LOGGER.error("Error reading address data");
            return null;
        }
    }

    public static void loadAddresses() {

        File source;
        try (ZipFile zipFile = new ZipFile(source = new File(Loader.class.getClassLoader().getResource("addresses-geojson.zip").toURI()))) {
            System.out.println("Loading Addresses from " + source);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                System.out.println("> " + entry.getName());
                long start = addressPositions.size();
                BufferedReader r = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
                String defaultCity = entry.getName().startsWith("city_of_") ?
                    WordUtils.capitalizeFully(StringUtils.substringBefore(entry.getName(), "-").substring(8)) : null;
                String defaultState = "IL";
                String line;
                while ((line = r.readLine()) != null) {
                    Address address = getGeoJsonAsAddress(line);

                    boolean adjustmentNeeded = false;
                    if (!address.hasState() && defaultState != null) {
                        address.setState(defaultState);  // This is the state our data is in.
                        adjustmentNeeded = true;
                    }

                    if (!address.hasCity() && defaultCity != null) {
                        address.setCity(defaultCity);
                        adjustmentNeeded = true;
                    }
                    if (!address.hasLine() || !address.hasCity()) {
//                        LOGGER.warn(
//                            "Missing data: line {} city {} state {} postalCode {}\n\t{}",
//                            address.hasLine(), address.hasCity(), address.hasState(), address.hasPostalCode(), line);
                        continue; // Skip addresses with missing data
                    }
                    if (adjustmentNeeded) {
                        line = getAddressAsGeoJson(address);
                    }
                    byte[] data = line.getBytes();
                    long value = (addresses.getFilePointer() << 16) | (data.length & 0xFFFF);
                    addressPositions.add(value);
                    HospitalRegion hr = hsaMap.get(address.getPostalCode());
                    if (hr != null) {
                        hr.positions.add(value);
                    }

                    addresses.write(line.getBytes());
                    if (addressPositions.size() % 100000 == 0) {
                        System.out.print('.');
                        System.out.flush();
                    }
                }
                System.out.println("\n> " + entry.getName() + ": " + (addressPositions.size() - start));
            }
            System.out.println();
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("{} reading zip file", e.getClass().getSimpleName(), e);
            throw new RuntimeException("Error reading zip file", e);
        }
    }

    /**
     * Read information about hospital region mappings from ZipHsaHr18.csv
     */
    private static void loadHospitalRegions() {
        String file = "ZipHsaHrr18.csv";
        try (BufferedReader r =
            new BufferedReader(
                new InputStreamReader(
                    AddressGenerator.class.getClassLoader().getResourceAsStream(file)));
        ) {
            // Skip the line with the field names.
            r.readLine();

            String line;
            while ((line = r.readLine()) != null) {
                String parts[] = line.split(",");
                if (parts.length < 3) {
                    continue;
                }
                // Index zips by hsa and hrr
                HospitalRegion reg = new HospitalRegion(parts[1], parts[2]);
                hsaMap.put(parts[0], reg);
                List<HospitalRegion> l = hsaZips.get(reg.hsa);
                if (l == null) {
                    l = new ArrayList<HospitalRegion>();
                    hsaZips.put(reg.hsa, l);
                }
                l.add(reg);
                l = hrrZips.get(reg.hrr);
                if (l == null) {
                    l = new ArrayList<HospitalRegion>();
                    hrrZips.put(reg.hrr, l);
                }
                l.add(reg);
            }
        } catch (IOException e) {
            LOGGER.error("Cannot read {}", file);
        }
    }

    @Override
    public Collection<Address> getAll() {
        throw new UnsupportedOperationException("getAll is not supported on LocationGenerator");
    }

}
