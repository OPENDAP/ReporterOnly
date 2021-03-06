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
 * Entry point for REST call. All controllers receives REST request outside and reroute
 * them to internal application services. After that it returns results.
 */
package org.opendap.harvester.controller;

import java.io.File;

import org.joda.time.LocalDateTime;
import org.opendap.harvester.ReporterApplication;
import org.opendap.harvester.config.ConfigurationExtractor;
import org.opendap.harvester.entity.LogData;
import org.opendap.harvester.entity.dto.LogDataDto;
import org.opendap.harvester.service.LogExtractionService;
import org.opendap.harvester.service.impl.RegistrationImpl;
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
	private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	private boolean logOutput = false;
	
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
    	if(logOutput) { log.info("/log.1/5) getLogsSince() entry");}
        LogData logData;
        if(logOutput) { log.info("/log.2/5) checking if 'since' is empty");}
        if (!StringUtils.isEmpty(since)){
        	if(logOutput) { log.info("/log.3/5) 'since' is not empty, extracting recent data ...");}
            LocalDateTime localDateTime = LocalDateTime.parse(since);
            if(logOutput) { log.info("/log.3.2) time : " + localDateTime);}
            logData = logExtractionService.extractLogDataSince(localDateTime);
            if(logOutput) { log.info("/log.3.3) log data : size - "+logData.getLines().size()+"\n" + logData.toString());}
        } else {
        	if(logOutput) { log.info("/log.3/5) 'since' is empty, extracting all data ...");}
            logData = logExtractionService.extractAllLogData();
        }
        if(logOutput) { log.info("/log.4/5) data extracted, checking data ...");} 
        if (logData == null){
        	if(logOutput) { log.info("log.4e) data is null");}
            throw new IllegalStateException("Log data is null");
        }
        if(logOutput) { log.info("/log.5/5) kosher data, returning <<");}
        return logExtractionService.buildDto(logData);
    }
    
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    @ResponseBody
    public void registerReporter() {
        File tmp = new File("./reporter.uuid");
        if (tmp.exists() && tmp.canRead()) {
        	tmp.delete();
        }
    	
    	RegistrationImpl r = new RegistrationImpl();
    	r.init();
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
