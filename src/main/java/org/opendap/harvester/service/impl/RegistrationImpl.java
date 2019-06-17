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
	// 4/23/19 - SBL - initial code
		//log.info("event.1/2) application started");
		init();
		//log.info("event.2/2) DONE");
	}//end onApplicationEvent()
		
	/**
	 * init method
	 * 		starts the registration process
	 */
	public void init() {
	// 2/7/19 - SBL - initial code
	// 4/23/19 - SBL - removed @PostConstruct, was causing race condition
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
	// 1/31/19 - SBL - initial code
		//log.info("registerCall.1/3) registration entry checkpoint"); // <---
		if(configurationExtractor == null) {
			//log.info("registerCall.1e) config extractor is null"); // <---
			//log.info("config extractor is null");
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
			//log.info("registration url is null"); // <---
		}
		
		//log.info("regCall.3/3) called collector, returning <<");
	}//end registerationCall()
	
	/**
	 * buildUrl method 
	 * 		builds registration url from config files and returns it to caller.
	 * @return registration URL built from config files.
	 */
	private URL buildUrl() {
	// 1/31/19 - SBL - initial code
		//log.info("build.1/3) buildUrl() entry checkpoint"); // <---
		String collectorUrl = configurationExtractor.getCollectorUrl();
		//log.info("build.2/3) collector : "+collectorUrl); // <---
		
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
		//log.info("build.3/3) url built, returning <<");
		return url;
	}//end buildUrl()
	
	/**
	 * 
	 * @return
	 */ //
	/*
	private URL buildPOSTUrl() {
	// 5/29/19 - SBL - initial code
		//log.info("buildPost.1/4) buildPOSTUrl entry ..."); // <---
		String collectorUrl = configurationExtractor.getCollectorUrl();
		//log.info("buildPost.2/4) collector : "+collectorUrl); // <---
		//String serverUrl = configurationExtractor.getServerUrl();
		//String reporterUrl = configurationExtractor.getReporterUrl();
		//long ping = configurationExtractor.getDefaultPing();
		//Integer logNumber = configurationExtractor.getLogNumber();
		
		//log.info("buildPost.3/4) building url ..."); // <---
		URL url;
		try {
			//url = new URL("http://"+collectorUrl+"serverUrl="+serverUrl+"&reporterUrl="+reporterUrl+"&ping="+ping+"&log="+logNumber);
			url = new URL("http://"+collectorUrl);
			return url;
		} catch (MalformedURLException e) {
			url = null;
			e.printStackTrace();
		}
		//log.info("buildPost.4/4) returning <<"); // <---
		return url;
	}// end buildPOSTUrl() 
	*/
	
	/**
	 * 
	 * @return
	 */ // 
	/*
	private URL buildPUTUrl() {
	// 5/29/19 - SBL - initial code
		//log.info("buildPut.1/4) buildPOSTUrl entry ..."); // <---
		String collectorUrl = configurationExtractor.getCollectorUrl();
		//log.info("buildPut.2/4) collector : "+collectorUrl); // <---
		String serverUrl = configurationExtractor.getServerUrl();
		String reporterUrl = configurationExtractor.getReporterUrl();
		long ping = configurationExtractor.getDefaultPing();
		Integer logNumber = configurationExtractor.getLogNumber();
		
		//log.info("buildPut.3/4) building url ..."); // <---
		URL url;
		try {
			url = new URL("http://"+collectorUrl+"serverUrl="+serverUrl+"&reporterUrl="+reporterUrl+"&ping="+ping+"&log="+logNumber+"&serverUUID=");
			return url;
		} catch (MalformedURLException e) {
			url = null;
			e.printStackTrace();
		}
		//log.info("buildPut.4/4) returning <<"); // <---
		return url;
	}// end buildPUTUrl() 
	*/
	
	/**
	 * callCollector method
	 * 		takes passed in registrationUrl and uses it to opens a connection 
	 * 		which registers (or updates) the reporter with the collector.
	 * @param registrationUrl of the collector application]
	 */
	private void callCollector(URL registrationUrl) {
	// 1/31/19 - SBL - initial code
		log.info("call.1/5) callCollector() entry checkpoint"); // <---
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuilder stringBuilder;
		
		try {
			connection = (HttpURLConnection)registrationUrl.openConnection();
			log.info("call.2/5) connection made"); // <---
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();
			log.info("call.3/5) called collector with: "+registrationUrl); // <---
			log.info("call.4/5) response code: "+responseCode); // <---
			
			
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			stringBuilder = new StringBuilder();
			
			String line = null;
			UUID uuid = null;
			int x = 0;
			while((line = reader.readLine()) != null) {
				log.info("call.4.1."+x+") line : "+line);
				stringBuilder.append(line);
				x++;
			}// end while
			
			try {
				JSONObject json = new JSONObject(stringBuilder.toString());
				uuid = UUID.fromString(json.getString("serverUUID"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//log.info("call.4.2) line substr : "+total.substring(240,276));
			log.info("call.4.2) id : "+uuid.toString());
			//uuid = UUID.fromString(line.substring(236,272));
			
			saveUUIDtoFile(uuid);
						
			log.info("call.4.3) done");
		} catch (MalformedURLException e){
			e.printStackTrace();
		} catch (ConnectException e) {
			log.info("call.1e) "+e.getMessage()); // <---
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (connection != null) {
				connection.disconnect();
				log.info("call.1f) disconnected"); // <---
			}//end if 
		}//end finally
		log.info("call.5/5) returning <<"); // <---
	}//end callCollector()
	
	private void saveUUIDtoFile(UUID uuid) {
	// 6/10/19 - SBL - initial code
		File file = new File("./reporter.uuid");
		
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		file.setReadable(true);
		file.setExecutable(true);
		FileWriter fw = null;
		
		try {
			fw = new FileWriter(file);
			fw.write(uuid.toString());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}//end saveUUIDtoFile()
	
	/**
	 * 
	 * @param registrationUrl
	 */ // 
	/*
	private void callPostCollector(URL registrationUrl){
	// 5/29/19 - SBL - initial code
		//log.info("callPost.1/7) callPostCollector() entry checkpoint"); // <---
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuilder stringBuilder;
		
		//log.info("callPost.2/7) setting params"); // <---
		Map<String,Object> params = new LinkedHashMap<>();
		params.put("serverUrl", configurationExtractor.getServerUrl());
		params.put("reporterUrl", configurationExtractor.getReporterUrl());
		params.put("ping", configurationExtractor.getDefaultPing());
		params.put("log", configurationExtractor.getLogNumber());
		
		//log.info("callPost.3/7) encoding params"); // <---
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            try {
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				//log.info("callPost.3.1e) postData encode failed");
				e.printStackTrace();
			}//end try/catch
            postData.append('=');
            try {
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				//log.info("callPost.3.2e) postData encode failed");
				e.printStackTrace();
			}//end try/catch
        }//end for
        
        byte[] postDataBytes = null;
		try {
			postDataBytes = postData.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			//log.info("callPost.3.3e) postDataBytes encode failed");
			e1.printStackTrace();
		}//end try/catch
		
		try {
			connection = (HttpURLConnection)registrationUrl.openConnection();
			//log.info("callPost.4/7) connection made"); // <---
			
			connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
	        connection.setDoOutput(true);
	        connection.getOutputStream().write(postDataBytes);
			
			int responseCode = connection.getResponseCode();
			//log.info("callPost.5/7) called collector with: "+registrationUrl); // <---
			//log.info("callPost.6/7) response code: "+responseCode); // <---
			
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			stringBuilder = new StringBuilder();
			
	        for (int c; (c = reader.read()) >= 0;)
	            stringBuilder.append((char)c);
	        String response = stringBuilder.toString();
			
			//log.info("callPost.6.1) line : "+response);
			
		} catch (MalformedURLException e){
			e.printStackTrace();
		} catch (ConnectException e) {
			//log.info("callPost.4e) "+e.getMessage()); // <---
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (connection != null) {
				connection.disconnect();
				//log.info("callPost.3f) disconnected"); // <---
			}// end if 
		}// end finally
		//log.info("callPost.7/7) returning <<"); // <---
	}// end callPostCollector() 
	*/
	
}//end class RegistrationImpl