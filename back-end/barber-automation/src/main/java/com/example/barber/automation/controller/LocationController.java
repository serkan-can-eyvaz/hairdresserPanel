package com.example.barber.automation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/locations")
@CrossOrigin(origins = "*")
public class LocationController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache for location data
    private Map<String, List<String>> cityDistricts = new HashMap<>();
    private Map<String, List<String>> districtNeighborhoods = new HashMap<>();
    private List<String> cities = new ArrayList<>();

    @GetMapping("/cities")
    public List<String> getCities() {
        if (cities.isEmpty()) {
            loadLocationData();
        }
        System.out.println("Şehir listesi döndü - count=" + cities.size());
        return cities;
    }

    @GetMapping("/cities/{city}/districts")
    public List<String> getDistricts(@PathVariable String city) {
        if (cityDistricts.isEmpty()) {
            loadLocationData();
        }
        return cityDistricts.getOrDefault(city, new ArrayList<>());
    }

    @GetMapping("/cities/{city}/districts/{district}/neighborhoods")
    public List<String> getNeighborhoods(@PathVariable String city, @PathVariable String district) {
        if (districtNeighborhoods.isEmpty()) {
            loadLocationData();
        }
        String key = city + "_" + district;
        return districtNeighborhoods.getOrDefault(key, new ArrayList<>());
    }

    private void loadLocationData() {
        try {
            System.out.println("Resources klasöründen Türkiye adres verileri yükleniyor...");
            loadFromResources();
        } catch (Exception e) {
            System.err.println("Veri yükleme hatası: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lokasyon verileri yüklenemedi: " + e.getMessage());
        }
    }

    private void loadFromResources() throws IOException {
        // Şehirleri yükle
        loadCities();
        
        // İlçeleri yükle
        loadDistricts();
        
        // Mahalleleri yükle
        loadNeighborhoods();
        
        System.out.println("Türkiye verileri yüklendi - Şehirler: " + cities.size() + 
                         ", İlçe eşleşmeleri: " + cityDistricts.size() +
                         ", Mahalle eşleşmeleri: " + districtNeighborhoods.size());
    }

    private synchronized void loadCities() throws IOException {
        ClassPathResource resource = new ClassPathResource("locations/sehirler.json");
        JsonNode citiesNode = objectMapper.readTree(resource.getInputStream());
        
        List<String> tempCities = new ArrayList<>();
        if (citiesNode.isArray()) {
            for (JsonNode cityNode : citiesNode) {
                String cityName = cityNode.get("sehir_adi").asText();
                if (cityName != null && !cityName.trim().isEmpty()) {
                    // Büyük harfleri düzelt
                    cityName = capitalizeFirstLetter(cityName.toLowerCase());
                    tempCities.add(cityName);
                }
            }
        }
        tempCities.sort(String.CASE_INSENSITIVE_ORDER);
        cities = tempCities;
    }

    private synchronized void loadDistricts() throws IOException {
        ClassPathResource resource = new ClassPathResource("locations/ilceler.json");
        JsonNode districtsNode = objectMapper.readTree(resource.getInputStream());
        
        Map<String, List<String>> tempCityDistricts = new HashMap<>();
        if (districtsNode.isArray()) {
            for (JsonNode districtNode : districtsNode) {
                String cityName = districtNode.get("sehir_adi").asText();
                String districtName = districtNode.get("ilce_adi").asText();
                
                if (cityName != null && districtName != null) {
                    // Büyük harfleri düzelt
                    cityName = capitalizeFirstLetter(cityName.toLowerCase());
                    districtName = capitalizeFirstLetter(districtName.toLowerCase());
                    
                    tempCityDistricts.computeIfAbsent(cityName, k -> new ArrayList<>()).add(districtName);
                }
            }
        }
        
        // İlçeleri alfabetik sırala
        for (List<String> districts : tempCityDistricts.values()) {
            districts.sort(String.CASE_INSENSITIVE_ORDER);
        }
        cityDistricts = tempCityDistricts;
    }

    private synchronized void loadNeighborhoods() throws IOException {
        Map<String, List<String>> tempDistrictNeighborhoods = new HashMap<>();
        
        String[] neighborhoodFiles = {
            "locations/mahalleler-1.json",
            "locations/mahalleler-2.json", 
            "locations/mahalleler-3.json",
            "locations/mahalleler-4.json"
        };
        
        for (String fileName : neighborhoodFiles) {
            try {
                ClassPathResource resource = new ClassPathResource(fileName);
                JsonNode neighborhoodsNode = objectMapper.readTree(resource.getInputStream());
                
                if (neighborhoodsNode.isArray()) {
                    for (JsonNode neighborhoodNode : neighborhoodsNode) {
                        String cityName = neighborhoodNode.get("sehir_adi").asText();
                        String districtName = neighborhoodNode.get("ilce_adi").asText();
                        String neighborhoodName = neighborhoodNode.get("mahalle_adi").asText();
                        
                        if (cityName != null && districtName != null && neighborhoodName != null) {
                            // Büyük harfleri düzelt
                            cityName = capitalizeFirstLetter(cityName.toLowerCase());
                            districtName = capitalizeFirstLetter(districtName.toLowerCase());
                            neighborhoodName = capitalizeFirstLetter(neighborhoodName.toLowerCase());
                            
                            String key = cityName + "_" + districtName;
                            tempDistrictNeighborhoods.computeIfAbsent(key, k -> new ArrayList<>()).add(neighborhoodName);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Mahalle dosyası yükleme hatası (" + fileName + "): " + e.getMessage());
            }
        }
        
        // Mahalleleri alfabetik sırala
        for (List<String> neighborhoods : tempDistrictNeighborhoods.values()) {
            neighborhoods.sort(String.CASE_INSENSITIVE_ORDER);
        }
        districtNeighborhoods = tempDistrictNeighborhoods;
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
