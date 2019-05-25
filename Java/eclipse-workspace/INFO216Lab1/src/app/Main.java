package app;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class Main {
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		
		//Starts a new model
		Model model = new Model();
		
		//Sets up model based on dataset
		model.setUpModel();
		
		//Examples of queries:
		
		//Average radiation
		System.out.println("--Cities by average radiation--");
		for(String res : model.selectCitiesByAverageRadiation()) {
			System.out.println(res);
		}
		System.out.println();
		
		//Amount of shelters
		System.out.println("--Cities by amount of shelters--");
		for(String res : model.selectCitiesByAmountShelters()) {
			System.out.println(res);
		}
		System.out.println();
		
		//Total rain
		System.out.println("--Cities by total amount of rain--");
		for(String res : model.selectCitiesByMostTotalRain()) {
			System.out.println(res);
		}
		System.out.println();
		
		//Average temperature
		System.out.println("--Cities by average temperature");
		for(String res : model.selectCitiesByAverageTemperature()) {
			System.out.println(res);
		}
		System.out.println();
		
		
		
	}

}
