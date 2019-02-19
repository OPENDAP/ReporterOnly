/**
 * RegistrationImpl class
 * 		used to send registration data to collector 
 * 
 * 1/31/19 - SBL - initial code
 * 2/7/19 - SBL - added init() method
 * 		changed @Service to @Component on class
 * 		added @PostConstruct to init()
 */

package org.opendap.harvester.service.impl;

import org.opendap.harvester.service.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.opendap.harvester.config.ConfigurationExtractor;

@Component
public class RegistrationImpl implements Registration {

	@Autowired
	private ConfigurationExtractor configurationExtractor;
	
	/**
	 * init method
	 * 		called as soon as all components have been initialized
	 * 		starts the registration process
	 * 
	 * 2/7/19 - SBL - initial code
	 */
	@PostConstruct
	public void init() {
		//System.out.println("1) init entry checkpoint");
		registerationCall();
		//System.out.println("8) after registeration call");
	}//end init()
	
	
	/**
	 * registrationCall method
	 * 		used on startup of Reporter to register with Collector.
	 * 		builds registration url from config files and calls collector using url.
	 * 
	 * 1/31/19 - SBL - initial code
	 */
	public void registerationCall() {
		//log.info("registration entry checkpoint");
		//System.out.println("2)\tregistration entry checkpoint");
		if(configurationExtractor == null) {
			//System.out.println("2e)\tconfig extractor is null");
			//log.info("config extractor is null");
		}
		URL registrationUrl = buildUrl();
		//System.out.println("5)\turl : "+ registrationUrl);
		if(registrationUrl != null) {
			//System.out.println("6)\turl not null");
			callCollector(registrationUrl);
		}
		else {
			System.out.println("registration url is null");
		}
	}//end registerationCall()
	
	/**
	 * buildUrl method 
	 * 		builds registration url from config files and returns it to caller.
	 * @return registration URL built from config files.
	 * 
	 * 1/31/19 - SBL - initial code
	 */
	private URL buildUrl() {
		//System.out.println("3)\t\tbuildUrl entry checkpoint");
		String collectorUrl = configurationExtractor.getCollectorUrl();
		//System.out.println("4)\t\tcollector : "+collectorUrl);
		String serverUrl = configurationExtractor.getServerUrl();
		String reporterUrl = configurationExtractor.getReporterUrl();
		long ping = configurationExtractor.getDefaultPing();
		Integer logNumber = configurationExtractor.getLogNumber();
		
		URL url;
		try {
			url = new URL("http://"+collectorUrl+"serverUrl="+serverUrl+"&reporterUrl="+reporterUrl+"&ping="+ping+"&log="+logNumber);
			return url;
		} catch (MalformedURLException e) {
			url = null;
			e.printStackTrace();
		}
		return url;
	}//end buildUrl()
	
	/**
	 * callCollector method
	 * 		takes passed in registrationUrl and uses it to opens a connection 
	 * 		which registers (or updates) the reporter with the collector.
	 * @param registrationUrl of the collector application]
	 * 
	 * 1/31/19 - SBL - initial code
	 */
	private void callCollector(URL registrationUrl) {
		//System.out.println("1)call collector entry checkpoint");
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection)registrationUrl.openConnection();
			//System.out.println("2)\tconnection made");
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();
			//System.out.println("3)\t\tcalled collector with: "+registrationUrl);
			//System.out.println("4)\t\tresponse code: "+responseCode);
			
		} catch (MalformedURLException e){
			e.printStackTrace();
		} catch (ConnectException e) {
			//System.out.println("7e)\t\t"+e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (connection != null) {
				connection.disconnect();
			}
		}//end finally
	}//end callCollector()
	
}