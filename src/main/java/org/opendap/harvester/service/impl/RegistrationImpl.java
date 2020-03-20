/**
 Copyright (c) 2019 OPeNDAP, Inc.
 Please read the full copyright statement in the file LICENSE.

 Authors: 
	James Gallagher	 <jgallagher@opendap.org>
    Samuel Lloyd	 <slloyd@opendap.org>

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

 You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
*/

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendap.harvester.ReporterApplication;
import org.opendap.harvester.config.ConfigurationExtractor;

@Component
public class RegistrationImpl implements Registration {
	private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	
	@Autowired
	private ConfigurationExtractor configurationExtractor;
	
	/**
	 * onApplicationEvent method
	 * 		called automatically as soon as application is started
	 */
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		//log.info("event.1/2) application started");
		init();
		//log.info("event.2/2) DONE");
	}//end onApplicationEvent()
		
	/**
	 * init method
	 * 		starts the registration process
	 */
	public void init() {
		//log.info("init.1/2) init entry checkpoint"); // <---
		registerationCall();
		//log.info("init.2/2) after registeration call, returning <<"); // <---
	}//end init()
	
	/**
	 * registrationCall method
	 * 		used on startup of Reporter to register with Collector.
	 * 		builds registration url from config files and calls collector using url.
	 */
	public void registerationCall() {
		//log.info("registerCall.1/3) registration entry checkpoint"); // <---
		try {
			if(configurationExtractor == null) {
				log.error("registerCall.1e) config extractor is null"); // <---
				//log.info("config extractor is null");
				configurationExtractor = new ConfigurationExtractor();
				if(configurationExtractor == null) {
					throw new NullPointerException(); 
				}
				else {
					log.info("registerCall.1e) config extractor null error recovered");
				}
			}
		}catch(NullPointerException e) {
			String error = "/!\\ RegistrationImpl.java - registrationCall() : "+ e.toString() +" /!\\";
			log.error(error);
		}
		
		URL registrationUrl = buildUrl();
		//URL registrationUrl = buildPOSTUrl();

		//log.info("registerCall.2/3) url : "+ registrationUrl); // <---
		if(registrationUrl != null) {
			//log.info("registerCall.2.1) url not null"); // <---
			callCollector(registrationUrl);
			//callPostCollector(registrationUrl);
		}
		else {
			String error = "/!\\ RegistrationImpl.java - registrationCall() : registration URL was null /!\\";
			log.error(error); // <---
		}
		//log.info("regCall.3/3) called collector, returning <<");
	}//end registerationCall()
	
	/**
	 * buildUrl method 
	 * 		builds registration url from config files and returns it to caller.
	 * @return registration URL built from config files.
	 */
	private URL buildUrl() {
		//log.info("build.1/3) buildUrl() entry checkpoint"); // <---
		String collectorUrl = configurationExtractor.getCollectorUrl();
		//log.info("build.2/3) collector : "+collectorUrl); // <---
		
		String serverUrl = configurationExtractor.getServerUrl();
		String reporterUrl = configurationExtractor.getReporterUrl();
		long ping = configurationExtractor.getDefaultPing();
		Integer logNumber = configurationExtractor.getLogNumber();
		
		URL url = null;
		try {
			url = new URL("http://"+collectorUrl+"serverUrl="+serverUrl+"&reporterUrl="+reporterUrl+"&ping="+ping+"&log="+logNumber);
			return url;
		} catch (MalformedURLException e) {
			String error = "/!\\ RegistrationImpl.java - BuildUrl() : "+ e.toString() +" /!\\";
			log.error(error);
			url = null;
			//e.printStackTrace();
		}
		//log.info("build.3/3) url built, returning <<");
		return url;
	}//end buildUrl()
	
	/**
	 * callCollector method
	 * 		takes passed in registrationUrl and uses it to opens a connection 
	 * 		which registers (or updates) the reporter with the collector.
	 * @param registrationUrl of the collector application]
	 */
	private void callCollector(URL registrationUrl) {
		//log.info("call.1/5) callCollector() entry checkpoint"); // <---
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuilder stringBuilder;
		
		try {			
			connection = (HttpURLConnection)registrationUrl.openConnection();
			//log.info("call.2/5) connection made with : "+ registrationUrl.toString()); // <---
			connection.setRequestMethod("GET");
			//int responseCode = connection.getResponseCode();
			//log.info("call.3/5) called collector with: "+registrationUrl.toString()); // <---
			//log.info("call.4/5) response code: "+responseCode); // <---
			
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			stringBuilder = new StringBuilder();
			
			String line = null;
			UUID uuid = null;
			//int x = 0; //used for debugging - SBL 7.2.19
			while((line = reader.readLine()) != null) {
				//log.info("call.4.1."+x+") line : "+line);
				stringBuilder.append(line);
				//x++; //used for debugging - SBL 7.2.19
			}// end while
			
			try {
				JSONObject json = new JSONObject(stringBuilder.toString());
				uuid = UUID.fromString(json.getString("serverUUID"));
			} catch (JSONException e) {
				String error = "/!\\ RegistrationImpl.java - callCollector() : "+ e.toString() +" /!\\";
				log.error(error);
				// e.printStackTrace();
			}
			//log.info("call.4.2) id : "+uuid.toString());
			
			saveUUIDtoFile(uuid);
						
			//log.info("call.4.3) done");
		} catch (MalformedURLException e){
			String error = "/!\\ RegistrationImpl.java - callCollector() : "+ e.toString() +" /!\\";
			log.error(error);
			//e.printStackTrace();
		} catch (ConnectException e) {
			//log.info("call.1e) "+e.getMessage()); // <---
			String error = "/!\\ RegistrationImpl.java - callCollector() : "+ e.toString() +" /!\\";
			log.error(error);
			//e.printStackTrace();
		} catch (IOException e) {
			String error = "/!\\ RegistrationImpl.java - callCollector() : "+ e.toString() +" /!\\";
			log.error(error);
			//e.printStackTrace();
		}finally {
			if (connection != null) {
				connection.disconnect();
				//log.info("call.1f) disconnected"); // <---
			}//end if 
		}//end finally
		//log.info("call.5/5) returning <<"); // <---
	}//end callCollector()
	
	
	private void saveUUIDtoFile(UUID uuid) {
		File file = new File("./reporter.uuid");
		
		try {
			file.createNewFile();
		} catch (IOException e) {
			String error = "/!\\ RegistrationImpl.java - saveUUIDtoFile() : "+ e.toString() +" /!\\";
			log.error(error);
			//e.printStackTrace();
		}
		
		file.setReadable(true);
		file.setExecutable(true);
		FileWriter fw = null;
		
		try {
			fw = new FileWriter(file);
			fw.write(uuid.toString());
			fw.close();
		} catch (IOException e) {
			String error = "/!\\ RegistrationImpl.java - saveUUIDtoFile() : "+ e.toString() +" /!\\";
			log.error(error);
			// e.printStackTrace();
		}
	}//end saveUUIDtoFile()
	
}//end class RegistrationImpl