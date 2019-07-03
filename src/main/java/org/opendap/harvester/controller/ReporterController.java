/**
 * Entry point for REST call. All controllers receives REST request outside and reroute
 * them to internal application services. After that it returns results.
 */
package org.opendap.harvester.controller;

import org.joda.time.LocalDateTime;
import org.opendap.harvester.ReporterApplication;
import org.opendap.harvester.config.ConfigurationExtractor;
import org.opendap.harvester.entity.LogData;
import org.opendap.harvester.entity.dto.LogDataDto;
import org.opendap.harvester.service.LogExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


/**
 * This annotation tells us about what type of Spring bean it is.
 * It is Controller. It means that it can receive REST requests.
 */
@Controller
@RequestMapping("/")
public class ReporterController {
	//private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	
    /**
     * Autowired automatically inject some of the HyraxInstanceRegisterService implementations to this
     * class field. It will happen on class instantiating stage.
     * After that it can be used in this class like service endpoint.
     */
    @Autowired
    private LogExtractionService logExtractionService;

    @Autowired
    private ConfigurationExtractor configurationExtractor;


    @RequestMapping(path = "/log", method = RequestMethod.GET)
    @ResponseBody
    public LogDataDto getLogsSince(@RequestParam(required = false) String since) throws Exception {
    	//log.info("/log.1/5) getLogsSince() entry");
        LogData logData;
        //log.info("/log.2/5) checking if 'since' is empty");
        if (!StringUtils.isEmpty(since)){
        	//log.info("/log.3/5) 'since' is not empty, extracting recent data ...");
            LocalDateTime localDateTime = LocalDateTime.parse(since);
            //log.info("/log.3.2) time : " + localDateTime);
            logData = logExtractionService.extractLogDataSince(localDateTime);
            //log.info("/log.3.3) log data :\n" + logData);
        } else {
        	//log.info("/log.3/5) 'since' is empty, extracting all data ...");
            logData = logExtractionService.extractAllLogData();
        }
        //log.info("/log.4/5) data extracted, checking data ..."); 
        if (logData == null){
        	//log.info("log.4e) data is null");
            throw new IllegalStateException("Log data is null");
        }
        //log.info("/log.5/5) kosher data, returning <<");
        return logExtractionService.buildDto(logData);
    }

    // 4/16/19 - SBL - Methods below are for testing, leave commented for release versions    
    /*
    @RequestMapping(path = "/defaultPing", method = RequestMethod.GET)
    @ResponseBody
    public long getDefaultPing() throws Exception {
       return configurationExtractor.getDefaultPing();
    }//end defaultPing()    
    */
    
    /*
    @RequestMapping(path = "/collectorUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getCollectorUrl() {
    	return configurationExtractor.getCollectorUrl();
    }*/
    
    /*
    @RequestMapping(path = "/serverUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getServerUrl() {
    	return configurationExtractor.getServerUrl();
    }
    */
    
    /*
    @RequestMapping(path = "/reporterUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getReporterUrl() {
    	return configurationExtractor.getReporterUrl();
    }
    */
    
    /*
    @RequestMapping(path = "/logNumber", method = RequestMethod.GET)
    @ResponseBody
    public Integer getLogNumber() {
    	return configurationExtractor.getLogNumber();
    }
    */

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleException(IllegalStateException e) {
    }

}
