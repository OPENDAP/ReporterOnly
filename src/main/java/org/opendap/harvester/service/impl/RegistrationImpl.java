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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendap.harvester.service.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.boot.context.event.ApplicationStartingEvent;
//import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.opendap.harvester.ReporterApplication;
import org.opendap.harvester.config.ConfigurationExtractor;

@Component
public class RegistrationImpl implements Registration {
	//private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	
	@Autowired
	private ConfigurationExtractor configurationExtractor;
	
	/**
	 * onApplicationEvent method
	 * 		called automatically as soon as application is started
	 */
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
	// 4/23/19 - SBL - initial code
		//log.info("event.1) application started");
		init();	
	}//end onApplicationEvent()
		
	/**
	 * init method
	 * 		starts the registration process
	 */
	public void init() {
	// 2/7/19 - SBL - initial code
	// 4/23/19 - SBL - removed @PostConstruct, was causing race condition
		//log.info("init.1) init entry checkpoint"); // <---
		registerationCall();
		//log.info("init.2) after registeration call"); // <---
	}//end init()
	
	/**
	 * registrationCall method
	 * 		used on startup of Reporter to register with Collector.
	 * 		builds registration url from config files and calls collector using url.
	 */
	public void registerationCall() {
	// 1/31/19 - SBL - initial code
		//log.info("registration entry checkpoint");
		//log.info("registerCall.1) registration entry checkpoint"); // <---
		if(configurationExtractor == null) {
			//log.info("registerCall.1e) config extractor is null"); // <---
			//log.info("config extractor is null");
		}
		URL registrationUrl = buildUrl();
		//log.info("registerCall.2) url : "+ registrationUrl); // <---
		if(registrationUrl != null) {
			//log.info("registerCall.3) url not null"); // <---
			callCollector(registrationUrl);
		}
		else {
			//log.info("registration url is null"); // <---
		}
	}//end registerationCall()
	
	/**
	 * buildUrl method 
	 * 		builds registration url from config files and returns it to caller.
	 * @return registration URL built from config files.
	 */
	private URL buildUrl() {
	// 1/31/19 - SBL - initial code
		//log.info("build.1) buildUrl entry checkpoint"); // <---
		String collectorUrl = configurationExtractor.getCollectorUrl();
		//log.info("build.2) collector : "+collectorUrl); // <---
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
	 */
	private void callCollector(URL registrationUrl) {
	// 1/31/19 - SBL - initial code
		//log.info("call.1) call collector entry checkpoint"); // <---
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection)registrationUrl.openConnection();
			//log.info("call.2) connection made"); // <---
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();
			//log.info("call.3) called collector with: "+registrationUrl); // <---
			//log.info("call.4) response code: "+responseCode); // <---
		} catch (MalformedURLException e){
			e.printStackTrace();
		} catch (ConnectException e) {
			//log.info("call.1e) "+e.getMessage()); // <---
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (connection != null) {
				connection.disconnect();
				//log.info("call.1f) disconnected"); // <---
			}//end if 
		}//end finally
	}//end callCollector()
	
}//end class RegistrationImpl