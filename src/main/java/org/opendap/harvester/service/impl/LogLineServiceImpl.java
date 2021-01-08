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

package org.opendap.harvester.service.impl;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.opendap.harvester.ReporterApplication;
import org.opendap.harvester.entity.LinePatternConfig;
import org.opendap.harvester.entity.LogLine;
import org.opendap.harvester.entity.dto.LogLineDto;
import org.opendap.harvester.service.LogLineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

@Service
public class LogLineServiceImpl implements LogLineService {
	private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	private boolean logOutput = false;
	
    private static final String TIME_FIELD = "localDateTime";

    @Override
    public LocalDateTime getLocalDateTime(LogLine logLine) {
    	if (logOutput) { log.info("getLocalDateTime() | >>> function start"); } 
        Map<String, String> logLineValues = logLine.getValues();
        //log.info("time.2/2) time value : "+ logLineValues.get(TIME_FIELD).toString() + ", returning <<"); // <---
        return toGMT(logLineValues.get(TIME_FIELD));
    }

    @Override
    public LogLine parseLogLine(String line, LinePatternConfig config) {
    	//log.info("parse.1/5) parseLogLine() entry, checking for nulls ..."); // <---
        if (line == null || config == null) {
            return null;
        }

        //log.info("parse.2/5) not null, getting names and creating hashMap ..."); // <---
        String[] names = config.getNames();
        Map<String, String> logLine = new HashMap<>();

        //log.info("parse.3/5) creating matcher ..."); // <---
        Matcher matcher = config.getPattern().matcher(line.trim());
        //log.info("parse.4/5) matching ..."); // <---
        if (matcher.matches()) {
        	//log.info("parse.4.1) pattern matches, populating map<> ..."); // <---
            for (int i = 1; i <= matcher.groupCount(); i++) {
                logLine.put(names[i-1], matcher.group(i));
                //log.info("parse.4.2) values : "+ names[i-1] + " - "+logLine.get(names[i-1])); // <---
            }
        }
        else {
        	//log.info("parse.4.1) no match ..."); // <---
        }
        
        //log.info("parse.5/5) done, returning <<"); // <---
        return LogLine.builder().values(logLine).build();
    }

    @Override
    public LogLineDto buildDto(LogLine logLine) {
        return LogLineDto.builder().values(logLine.getValues()).build();
    }

    /**
     * Return the date/time information in GMT given that it has been recorded in the
     * local time and we know what time zone that is.
     *
     * @param zoneString Time zone for the log data, using the strings recognized by the
     *                   java DateTime class.
     * @return A new LocalDateTime instance.
     */
    private LocalDateTime toGMT(String zoneString){
        DateTime zonedDateTime =
                DateTime.parse(zoneString, DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS Z"));
        return zonedDateTime.toDateTime(DateTimeZone.UTC).toLocalDateTime();
    }
}
