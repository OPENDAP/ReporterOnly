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

package org.opendap.harvester.controller;

import org.opendap.harvester.ReporterApplication;
import org.opendap.harvester.config.ConfigurationExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @todo I think this code can be merged into ReporterController. jhrg 10/8/17
 */
@RestController
@RequestMapping("/")
public class HealthCheckController {
	//private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	
    @Value("${reporter.version}")
    private String reporterVersion;

    @Autowired
    private ConfigurationExtractor configurationExtractor;

    @RequestMapping(path = "/healthcheck", method = RequestMethod.GET)
    public String healthCheck(){
    	//log.info("hc.1) /healthcheck checkpoint");
        return "Reporter Application, Version = " + reporterVersion;
    }

    /**
     * @todo Make this a more complete response. It could return the default and 
     * configured parameter values.
     * 
     * @return A string returned to the browser/client.
     */
    @RequestMapping(path = "/test", method = RequestMethod.GET)
    public String test(){
        try {
            return configurationExtractor.getHyraxLogfilePath();
        } catch (Exception e) {
        	    e.printStackTrace();
        }
        return "Error: Could not get the Hyrax log file path";
    }

}
