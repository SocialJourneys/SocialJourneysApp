package dotrural.ac.uk.realTime;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class Parser extends  DefaultHandler {
	
	
 String xml = "";
 String service ="";
 ArrayList <String> parsingResult; 
 String content = null;
 boolean desiredService =false;

	
	
	public Parser (String xml, String service) throws IOException{
		
		
		 this.xml = xml;
		 this.service = service;
       //System.out.println (xml);
       parsingResult = new ArrayList <String> ();
       
	}
	
	public void runParsing() throws IOException {
		parseDocument();
		 
	}
	
	public ArrayList <String> getParsingResult () {
		
		return parsingResult; 
	}
	

	private void parseDocument() {
		
		//get a factory
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
		
			//get a new instance of parser
			SAXParser sp = spf.newSAXParser();
		   
			sp.parse(new InputSource(new StringReader(xml)), this);
			
		}catch(SAXException se) {
			System.out.println ("problem with parsing");
			//se.printStackTrace();
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch (IOException ie) {
			ie.printStackTrace();
		}
	}

		

	//Event Handlers
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		//reset

	
	}
	

	public void characters(char[] ch, int start, int length) throws SAXException {
		 content = String.copyValueOf(ch, start, length).trim();
		
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
         
		 if (qName == "PublishedLineName") {
			 if (content.equals(service)) {
			 desiredService = true;
			 }
			 else {
				 desiredService = false;
			 }
		 }
        
        
		 
		 if (qName == "ExpectedDepartureTime") {
			 if (desiredService) { 
				 if (!content.equals("0"))
				 parsingResult.add(content);
			 
			 
			 }
		 }
	}
	
	
	
}
