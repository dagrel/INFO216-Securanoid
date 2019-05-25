package app;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.VCARD;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class Model {
	private static OntModel model;
	
	public void setUpModel() throws ParserConfigurationException, SAXException, IOException {
		
		//Creates model
		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		//Initialize was used more earlier. We have kept it as a storage place for our prefixes.
		Initialize init = new Initialize();
		model.setNsPrefixes(init.getNsPrefixes());
		
		// Custom properties we did not find any fitting resources on
		Property hasRad = model.createProperty("http://example.org/", "hasRadiation");
		Property hasShelters = model.createProperty("http://example.org/", "hasShelter");

		
		// Existing resources
        Resource place = model.createResource(model.getNsPrefixURI("dbo") + "Place");
        Resource settlement = model.createResource(model.getNsPrefixURI("dbo") + "Settlement");
        Resource village = model.createResource(model.getNsPrefixURI("dbo") + "Village");
        Resource populatedPlace = model.createResource(model.getNsPrefixURI("dbo") + "PopulatedPlace");
        Resource location = model.createResource(model.getNsPrefixURI("dbo") + "Location");
        Resource spatialThing = model.createResource(model.getNsPrefixURI("geo") + "SpatialThing");
        Resource norway = model.createResource(model.getNsPrefixURI("dbr") + "Norway");
        Resource temperature = model.createResource(model.getNsPrefixURI("dbo") + "datatype/Temperature");
		Resource radiation = model.createResource(model.getNsPrefixURI("dbr") + "Ionizing_Radiation");

	    Literal IonizingRadiation = model.createTypedLiteral("Ionizing_Radiation");	    
	    radiation.addProperty(RDFS.comment, IonizingRadiation); 
        
        // Existing properties
        Property country = model.createProperty(model.getNsPrefixURI("dbo") + "country");
        Property hasAgent = model.createProperty(model.getNsPrefixURI("event"), "hasAgent");
        Property lat = model.createDatatypeProperty(model.getNsPrefixURI("geo") + "lat");
        Property lng = model.createDatatypeProperty(model.getNsPrefixURI("geo") + "long");
        Property at = model.createDatatypeProperty(model.getNsPrefixURI("timeline") + "at");
		Property startDate = model.createProperty(model.getNsPrefixURI("schema"), "startDate");
		Property hasPrec = model.createProperty(model.getNsPrefixURI("tuw") + "hasPrecipitation");
		Property hasTemp = model.createProperty(model.getNsPrefixURI("tuw") + "hasExteriorTemperature");
		
		
		// Finds our dataset which should be located in src/dataset.xml		
		File file = new File("src/dataset.xml");
		
		// Prepping and parsing our XML
		DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dBF.newDocumentBuilder();
		Document document = db.parse(file);
		
		// Finding all cities in our dataset
		NodeList nList = document.getElementsByTagName("city");
		
		// Top loop goes through all cities in our dataset
		for(int i = 0; i < nList.getLength(); i++) {
			Element cityElement = (Element) nList.item(i);
			String cityNameString = cityElement.getAttributes().getNamedItem("name").getNodeValue();
			
			//Eliminating spaces in city names
			cityNameString = cityNameString.replace(" ", "_");
            Literal cityName = model.createTypedLiteral(cityNameString, XSDDatatype.XSDstring);
            
            // Creates city resource
			Resource city = model.createResource(cityNameString);
			
			// Adds information about city resource
            Literal cityComment = model.createTypedLiteral(cityName + " is a city located in Norway.", XSDDatatype.XSDstring);
			city.addProperty(RDF.type, OWL.Thing);
			city.addProperty(RDF.type, OWL.Thing);
            city.addProperty(RDF.type, place);
            city.addProperty(RDF.type, settlement);
            city.addProperty(RDF.type, spatialThing);
            city.addProperty(RDF.type, location);
            city.addProperty(RDF.type, village);
            city.addProperty(RDF.type, populatedPlace);
            city.addProperty(RDFS.label, cityName);
            city.addProperty(RDFS.comment, cityComment);
            city.addProperty(FOAF.name, cityName);
            city.addProperty(OWL.sameAs, city);
			
            // Try/catch to handle the fact that not all cities have shelters
			try {
				Element shelter = (Element) cityElement.getElementsByTagName("shelter").item(0);
				
				Resource shelters = model.createResource(cityNameString + "_shelter");
				
				// If the model already has the appropriate shelter resource, we get that instead
				if(model.containsResource(shelters)) {
					shelters = model.getResource(cityNameString + "_shelter");
				}
				else {
					// Adding the number of shelters to the resource, along with the data type
					shelters.addProperty(OWL.hasValue, shelter.getTextContent(), XSDDatatype.XSDint);
				}
				
				// The city gets the property hasShelters which points to our shelters resource
				city.addProperty(hasShelters, shelters);
			}
			catch(Exception e){
				
			}
			
			// Not all cities have weather
			try {
				Element weather = (Element) cityElement.getElementsByTagName("weather").item(0);
				NodeList weatherDates = weather.getElementsByTagName("date");
				
				// Here we go through all the dates on which weather occured
				for(int j = 0; j < weatherDates.getLength(); j++) {
					Element weatherDate = (Element) weatherDates.item(j);
					NodeList dateTimes = weatherDate.getElementsByTagName("time");
					
					Literal weatherDateLiteral  = model.createTypedLiteral(weatherDate.getAttribute("date-stamp"), XSDDatatype.XSDdate);	
					
					// If we've already made a resource with the date for our city we get that existing resource
					Resource date;
					try {
						date = model.getResource(cityName + "_" + weatherDate.getAttribute("date-stamp"));
					}
					catch(Exception e) {
						date = model.createResource(cityName + "_" + weatherDate.getAttribute("date-stamp"));
					}
			        
					// Adding the date to the date resource
			        date.addProperty(OWL.hasValue, weatherDateLiteral);    
			        					
			        // Going through all times/hours that the date may have
					for(int k = 0; j < dateTimes.getLength(); k++) {
						Element timeElement = (Element) dateTimes.item(k);
						String timeString = timeElement.getAttribute("time-stamp");
						
						Literal weatherTimeLiteral = model.createTypedLiteral(timeString, XSDDatatype.XSDtime);
						
						// Getting the existing time resource should it exist
						Resource weatherTime;
						try {
							weatherTime = model.getResource(cityName + "_" + timeString);
						}
						catch(Exception e) {
							weatherTime = model.createResource(cityName + "_" + timeString);
						} 
						
						weatherTime.addProperty(OWL.hasValue, weatherTimeLiteral);
						
						
						NodeList timeEvents = timeElement.getChildNodes();
						
						// Going through all the measurements for the time/instant
						for(int l = 0; l < timeEvents.getLength(); l++) {
							try {
								Element event = (Element) timeEvents.item(l);
								
								String tag = event.getTagName();
								String suffix = event.getTextContent().replace(".", "_");
								
								// We only have 2 types of events, so we only need to check for these
								if(tag == "air_temperature") {
									
									Resource temp = model.createResource(cityNameString + suffix);
									if(model.containsResource(temp)) {
										temp = model.getResource(cityNameString + suffix);
									}
									else {
										temp.addProperty(OWL.hasValue, event.getTextContent(), XSDDatatype.XSDfloat);
									}
									
									weatherTime.addProperty(hasTemp, temp);
								}
								else if(tag == "precipitation_amount") {
									Resource rain = model.createResource(cityNameString + suffix);
									
									if(model.containsResource(rain)) {
										rain = model.getResource(cityNameString + suffix);
									}
									else {
										rain.addProperty(OWL.hasValue, event.getTextContent(), XSDDatatype.XSDfloat);
									}
									weatherTime.addProperty(hasPrec, rain);
								}								
							}
							catch (Exception e) {
							}
						}
						// We then add our time to our date, and our date to our city
						date.addProperty(at, weatherTime);
						city.addProperty(startDate, date);
					}
				}
			}
			catch (Exception e) {
			}

			// Not all cities have radiation, so we do a try/catch
			try {
				Element radiationElement = (Element) cityElement.getElementsByTagName("radiation").item(0);
				
				NodeList radiationDates = radiationElement.getChildNodes();
				
				// Going through all the dates in the radiation for the city
				for(int j = 0; j < radiationDates.getLength(); j++) {
					Element radiationDateElement = (Element) radiationElement.getElementsByTagName("date").item(j);
					
					Literal radiationDateLiteral = model.createTypedLiteral(radiationDateElement.getAttribute("date-stamp"), XSDDatatype.XSDdate);
					
					// Same as before, we get the already existing date resource if we've already added it
					Resource radiationDate;
					try {
						radiationDate = model.getResource(cityName + "_" + radiationDateLiteral);
					}
					catch(Exception e) {
						radiationDate = model.createResource(cityName + "_" + radiationDateLiteral);
					}
					
					NodeList radiationTimes = radiationDateElement.getChildNodes();
					
					// Dates have times/hours
					for(int k = 0; k < radiationTimes.getLength(); k++) {
						Element radiationTimeElement = (Element) radiationDateElement.getElementsByTagName("time").item(j);
						String suffix = radiationTimeElement.getAttribute("time-stamp").replace(".", "_");
						
						Literal radiationTimeLiteral = model.createTypedLiteral(radiationTimeElement.getAttribute("time-stamp"), XSDDatatype.XSDtime);
						
						//There is only one measurement per radiation hour, we get that here and format it
						String radMesString = radiationTimeElement.getTextContent().replaceAll("\\s", "").replace(".", "_");
						Element radiationMeasurementElement = (Element) radiationTimeElement.getElementsByTagName("measurement").item(0);
						Literal radiationMeasurementLiteral = model.createTypedLiteral(radiationTimeElement.getTextContent().replaceAll("\\s", ""), XSDDatatype.XSDfloat);
						
						
						// If we already have an appropriate radiation for the city we just reuse it
						Resource radiationMeasurement = model.createResource(cityName + "_" + radMesString);						
						if(model.containsResource(radiationMeasurement)) {
							radiationMeasurement = model.getResource(cityName + "_" + radMesString);
						}
						else {
							radiationMeasurement.addProperty(RDF.type, radiation);
							radiationMeasurement.addProperty(OWL.hasValue, radiationMeasurementLiteral);
						}
						
						// Reusing of radiation time
						Resource radiationTime = model.createResource(cityName + "_" + radiationTimeLiteral);
						if(model.containsResource(radiationTime)) {
							radiationTime = model.createResource(cityName + "_" + radiationTimeLiteral);
						}
						else {
							radiationTime.addProperty(hasRad, radiationMeasurement);	
						}
												
						//Adding the radiation measurement to the radiation hour
					    radiationTime.addProperty(hasRad, radiationMeasurement);
					    //Adding radiation hour to radiation date
						radiationDate.addProperty(at, radiationTime);
						//Adding date to city
						city.addProperty(startDate, radiationDate);
					}
				}
			}
			catch(Exception e) {
				
			}
			
			
		}
	}
	
	
	//SPARQL for getting a specific city
	public ArrayList<String> selectCity(String cityName) {
        String queryString =
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>  " +
                "SELECT * WHERE { " +
                " ?cityName rdfs:label '%CITYNAME%' ." +
                "}";
        
        //Inserting the city name we want
        queryString = queryString.replace("%CITYNAME%", cityName);
        
        //We always execute the query the same, so we've put that in a seperate function 
		return executeQuery("selectCity", queryString);
    }	
	
	//Getting all cities and sorting by the most rain in total
	public ArrayList<String> selectCitiesByMostTotalRain() {
        String queryString =
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>  " +
                "PREFIX tuw: <https://www.auto.tuwien.ac.at/downloads/thinkhome/ontology/WeatherOntology.owl#> " +
                "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>" +
                "PREFIX schema: <https://schema.org/> " +
                "PREFIX owl:    <http://www.w3.org/2002/07/owl#>" +
                
                "SELECT ?cityName (SUM(?rainValue) as ?sum) WHERE { " +
                " ?city rdfs:label ?cityName ;" + 
                		"schema:startDate ?date ." +
                " ?date timeline:at ?time ." +
                "?time tuw:hasPrecipitation ?rain . " +
                "?rain owl:hasValue ?rainValue ." +
                "}" +
                "GROUP BY (?cityName) ORDER BY DESC(?sum) ASC(?cityName)";
        
		return executeQuery("selectCityWithMostTotalRain", queryString);
    }
	
	//Getting all cities/hours sorted by amount of radiation
	public ArrayList<String> selectCitiesByRadiation() {
		String queryString =
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>  " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                "PREFIX dbr: <http://dbpedia.org/resource/> " +
                "PREFIX ex:    <http://example.org/>" +
                "PREFIX schema: <https://schema.org/> " +
                "PREFIX owl:    <http://www.w3.org/2002/07/owl#>" +
                "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>" +

                "SELECT ?cityName ?radiation WHERE { " +
                " ?city rdfs:label ?cityName ;" + 
                		"schema:startDate ?date ." +
                "?date timeline:at ?time . " +
                "?time ex:hasRadiation ?rad . " +
                "?rad rdf:type dbr:Ionizing_Radiation ;" +
                	"owl:hasValue ?radiation ." +
                "}" +
                "ORDER BY DESC(?radiation) ASC(?cityName)";
		return executeQuery("selectCitiesByRadiation", queryString);
	}
	
	//Leaving in this experiment in combining multiple queries as to get a totaled 'noid rating'. We couldn't get this to work in time,
	//but the intent can perhaps be extracted.
	//We wanted to sort all cities based on all factors, ordered after a 'danger' priority.
	public ArrayList<String> selectCityWithHighestNoidRating() {

        String queryString =        		
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>  " +
                "PREFIX owl:    <http://www.w3.org/2002/07/owl#>" +
                "PREFIX schema: <https://schema.org/> " +
                "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>" +
                "PREFIX ex: <http://example.org/> " +
                "PREFIX tuw: <https://www.auto.tuwien.ac.at/downloads/thinkhome/ontology/WeatherOntology.owl#> " +
                
                "SELECT ?cityName (SUM(?rain) as ?totalRain) (AVG(?temperature) as ?avgTemp) WHERE" +                
                
                "{ ?city rdfs:label ?cityName . }" +
                "UNION {" +
                	//"SELECT ?cityName (AVG(?temperature as ?avgTemp) WHERE {" +
                		" ?city rdfs:label ?cityName ;" +
                				"schema:startDate ?date ." +
                		"?date timeline:at ?time . " + 
                		"?time tuw:hasExteriorTemperature ?tempElement . " +
                		"?tempElement owl:hasValue ?temperature . " +
                	//"}" +
                "}" +                
                	
				 "UNION {" +
					//"SELECT ?cityName (SUM(?rain as ?totalRain) WHERE {" +
						" ?city rdfs:label ?cityName ;" +
								"schema:startDate ?date ." +
						"?date timeline:at ?time . " + 
						"?time tuw:hasPrecipitation ?rainElement . " + 
						"?rainElement owl:hasValue ?rain . " +
					//"}" +
				"}" + 
                
				 "UNION {" +
					//"SELECT ?cityName (AVG(?radiation as ?avgRad) WHERE {" +
						" ?city rdfs:label ?cityName ;" +
								"schema:startDate ?date ." +
						"?date timeline:at ?time . " + 
						"?time ex:hasRadiation ?rad . " + 
						"?rad owl:hasValue ?radiation . " +
					//"}" +
				"}" + 

                "GROUP BY(?cityName) ORDER BY DESC(?avgRad) DESC(?shelters) DESC(?totalRain) DESC(?avgTemp) ASC(?cityName)";
        
		return executeQuery("selectCityWithHighestNoidRating", queryString);
    }
	
	//Getting cities by average temperature
	public ArrayList<String> selectCitiesByAverageTemperature(){
        String queryString =
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>  " +
                "PREFIX tuw: <https://www.auto.tuwien.ac.at/downloads/thinkhome/ontology/WeatherOntology.owl#> " +
                "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>" +
                "PREFIX schema: <https://schema.org/> " +
                "PREFIX owl:    <http://www.w3.org/2002/07/owl#>" +
                "SELECT ?cityName (ROUND(AVG(?tempValue)) as ?sum) WHERE { " +
                " ?city rdfs:label ?cityName ;" + 
                		"schema:startDate ?date ." +
                " ?date timeline:at ?time ." +
                "?time tuw:hasExteriorTemperature ?temp . " +
                "?temp owl:hasValue ?tempValue ." +
                "}" +
                "GROUP BY (?cityName) ORDER BY DESC(?sum) ASC(?cityName)";
        
		return executeQuery("selectCityWithMostTotalRain", queryString);
	}
	
	//Getting cities ordered by the amount of shelters
	public ArrayList<String> selectCitiesByAmountShelters(){
        String queryString =
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>  " +
                "PREFIX tuw: <https://www.auto.tuwien.ac.at/downloads/thinkhome/ontology/WeatherOntology.owl#> " +
                "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>" +
                "PREFIX schema: <https://schema.org/> " +
                "PREFIX ex: <http://example.org/>" +
                "PREFIX owl:    <http://www.w3.org/2002/07/owl#>" +
                "SELECT ?cityName ?shelterAmount WHERE { " +
                " ?city rdfs:label ?cityName ;" + 
                		"schema:startDate ?date ;" +
                		"ex:hasShelter ?shelter ." +
                "?shelter owl:hasValue ?shelterAmount . " +
                "}" +
                "ORDER BY DESC(?shelterAmount) ASC(?cityName)";
        
		return executeQuery("selectCityWithMostTotalRain", queryString);
	}
	
	//Getting cities by average radiation
	public ArrayList<String> selectCitiesByAverageRadiation(){
        String queryString =
                "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>  " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                "PREFIX tuw: <https://www.auto.tuwien.ac.at/downloads/thinkhome/ontology/WeatherOntology.owl#> " +
                "PREFIX timeline: <http://purl.org/NET/c4dm/timeline.owl#>" +
                "PREFIX schema: <https://schema.org/> " +
                "PREFIX ex: <http://example.org/>" +
                "PREFIX owl:    <http://www.w3.org/2002/07/owl#>" +
                "SELECT ?cityName (AVG(?radiation) as ?avgRad) WHERE { " +
                " ?city rdfs:label ?cityName ;" + 
        			"schema:startDate ?date ." +
        		"?date timeline:at ?time . " +
        		"?time ex:hasRadiation ?rad . " +
        		"?rad owl:hasValue ?radiation ." +
                "}" +
                "GROUP BY(?cityName) ORDER BY DESC(?avgRad) ASC(?cityName)";
        
		return executeQuery("selectCityWithMostTotalRain", queryString);
	}

	
	//Here we execute all queries. We get the results out as an array of strings that we read in the console.
	public ArrayList<String> executeQuery(String method, String queryString) {
		ArrayList<String> cityNames = new ArrayList<>();
		
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                cityNames.add(soln.toString());
            }
        } finally {
            qexec.close();
        }
        return cityNames;
	}
}