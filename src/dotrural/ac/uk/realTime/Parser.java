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

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class Parser extends DefaultHandler {

	String xml = "";
	String service = "";
	ArrayList<String> parsingResult;
	String content = null;
	boolean desiredService = false;
	boolean aimedDepartureAdded =false;
	static private Logger log = Logger.getLogger(Parser.class);

	public Parser(String xml, String service) throws IOException {

		this.xml = xml;
		this.service = service;
		// System.out.println (xml);
		parsingResult = new ArrayList<String>();

	}

	public void runParsing() throws IOException {
		parseDocument();

	}

	public ArrayList<String> getParsingResult() {

		return parsingResult;
	}

	private void parseDocument() {

		// get a factory
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {

			// get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			sp.parse(new InputSource(new StringReader(xml)), this);

		} catch (SAXException se) {
			log.error("Problem with parsing doc returned by NextBus API - " + se.getLocalizedMessage(), se);
			// se.printStackTrace();
		} catch (ParserConfigurationException pce) {
			log.error("Problem with parsing doc returned by NextBus API - " + pce.getLocalizedMessage(), pce);
		} catch (IOException ie) {
			log.error("Problem with parsing doc returned by NextBus API - " + ie.getLocalizedMessage(), ie);
		}
	}

	// Event Handlers
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// reset

	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		content = String.copyValueOf(ch, start, length).trim();

	}

	public void endElement(String uri, String localName, String qName) throws SAXException {

		if (qName == "PublishedLineName") {
			if (content.equals(service)) {
				desiredService = true;
				aimedDepartureAdded = false;
			} else {
				desiredService = false;
			}
		}

		if (qName == "AimedDepartureTime") {
			if (desiredService) {
				if (!content.equals("0"))
					parsingResult.add(content);
				aimedDepartureAdded = true;

			}
		}

		if (qName == "ExpectedDepartureTime") {
			if (desiredService) {
				if (!content.equals("0")) {
					if (aimedDepartureAdded) {
						parsingResult.set(parsingResult.size() - 1, content);

					} else {
						parsingResult.add(content);
					}
				}

			}
		}
	}

}
