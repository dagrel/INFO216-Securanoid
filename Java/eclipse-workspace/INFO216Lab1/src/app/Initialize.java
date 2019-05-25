package app;

import java.util.HashMap;

public class Initialize {
	//Hashmap of all our namespaces
    private HashMap<String, String> nsPrefix = new HashMap<String, String>() {{
        put("event", "http://purl.org/NET/c4dm/event.owl#");
        put("dbr", "http://dbpedia.org/resource/");
        put("dbo", "http://dbpedia.org/ontology/");
        put("time", "http://www.w3.org/2006/time#");
        put("timeline", "http://purl.org/NET/c4dm/timeline.owl#");
        put("foaf", "http://xmlns.com/foaf/0.1/");
        put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        put("schema", "https://schema.org/");
        put("ex", "http://example.org/");
        put("tuw", "https://www.auto.tuwien.ac.at/downloads/thinkhome/ontology/WeatherOntology.owl#");
    }};

   

    public HashMap<String, String> getNsPrefixes() {
        return this.nsPrefix;
    }

}