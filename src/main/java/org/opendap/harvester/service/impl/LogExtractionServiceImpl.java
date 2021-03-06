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
 * Service implementation. All business logic should be here.
 * Call to db are initiating from this place via Repositories
 */
package org.opendap.harvester.service.impl;

import org.joda.time.LocalDateTime;
import org.opendap.harvester.ReporterApplication;
import org.opendap.harvester.config.ConfigurationExtractor;
import org.opendap.harvester.entity.LinePattern;
import org.opendap.harvester.entity.LinePatternConfig;
import org.opendap.harvester.entity.LogData;
import org.opendap.harvester.entity.LogLine;
import org.opendap.harvester.entity.dto.LogDataDto;
import org.opendap.harvester.entity.dto.LogLineDto;
import org.opendap.harvester.service.LogExtractionService;
import org.opendap.harvester.service.LogLineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class LogExtractionServiceImpl implements LogExtractionService {
	private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	private boolean logOutput = false;
	
    @Autowired
    private LogLineService logLineService;

    @Autowired
    private ConfigurationExtractor configurationExtractor;

    @Override
    public LogData extractLogDataSince(LocalDateTime time) throws IOException {
    	if(logOutput) { log.info("extractLogDataSince() | >>> function start"); }
    	LogData logData = LogData.builder()
                .lines(getLogLines(time))
                .build();
    	if(logOutput) { log.info("extractLogDataSince() | log data : size - "+logData.getLines().size()); }
    	if(logOutput) { log.info("extractLogDataSince() | returning <<<"); }
        return logData;
    }

    @Override
    public LogData extractAllLogData() throws IOException {
        return LogData.builder()
                .lines(getLogLines())
                .build();
    }
    private List<LogLine> getLogLines() throws IOException {
        return getLogLines(null);
    }

    /**
     * @todo This method will fail if the pattern doesn't match. In that case is returns
     * a record/line that is 'values:""' repeated N time  where N is the number of fields
     * in the pattern regex. There's no error message.
     * 
     * @param since
     * @return
     * @throws IOException
     */
    private List<LogLine> getLogLines(LocalDateTime since) throws IOException {
    // 5/2/19 - SBL - added test for blank lines and lines that do not match pattern
    	if(logOutput) { log.info("getLogLines() | >>> function start"); }
        LinePattern linePattern = configurationExtractor.getLinePattern();
        
        if(logOutput) { log.info("getLogLines() | building config ..."); }
        LinePatternConfig config = LinePatternConfig.builder()
                .pattern(Pattern.compile(linePattern.getRegexp()))
                .names(linePattern.getNames().split(";"))
                .build();

        if(logOutput) { log.info("getLogLines() | ... config built"); } 
        if(logOutput) { log.info("getLogLines() | filepath : "+ configurationExtractor.getHyraxLogfilePath()); } 
        List<String> allLines = Files.readAllLines(Paths.get(configurationExtractor.getHyraxLogfilePath()), Charset.defaultCharset());
        
        if(logOutput) { log.info("getLogLines() | lines read : "+allLines.size()+", value of 0 : "+ allLines.get(0)); } 
        if(logOutput) { log.info("getLogLines() | parsing lines ..."); }
        List<LogLine> parsedLines = new ArrayList<>();
        int x = 1; // <-- used in debugging, SBL - 7.2.19
        int y = 0;
        for (String line : allLines){
        	
        	if (line.trim().isEmpty()) { // <--- check for blank line before parsing
        		if(logOutput) { log.info("getLogLines() | 	- "+x+") blank line, skipping"); }
        		if(logOutput) { x++; } // <-- used in debugging, SBL - 7.2.19
        		continue;
        	}//end if - blank line test
        	
            LogLine parsedLogLine = logLineService.parseLogLine(line, config); //parse line
            //if(logOutput) { log.info("getLogLines() | 	- "+x+") line : "+ parsedLogLine.getValues().toString()); }//output values 
            boolean matched = !parsedLogLine.getValues().isEmpty(); //check if line was a match or not
            
            if (matched && (since == null || logLineService.getLocalDateTime(parsedLogLine).isAfter(since))){ 
            	if(logOutput) { log.info("getLogLines() | 	- "+x+") line : "+ parsedLogLine.getValues().toString()); }//output values
            	if(logOutput) { log.info("getLogLines() | 	- "+x+") adding parsed line"); } 
                parsedLines.add(parsedLogLine);
            }//end if - kosher line
            else if(!matched) { // <--- if not a match
            	//TODO output parse error to log file. sbl 7.2.19
            	String error = "/!\\ LogExtractionServiceImpl.java - getLogLines() : malformed log line - \""+ line +"\" /!\\";
            	if(logOutput) { log.error(error); }
            	y++;
            	if(logOutput) { log.info("getLogLines() | 	- "+x+") line did not match pattern"); } 
            }//end if - non kosher line
            x++; // <-- used in debugging, SBL - 7.2.19
        }//end for loop
        
        if(y != 0) {
        	String error = "/!\\ LogExtractionServiceImpl.java - getLogLines() : number of malformed log lines - "+ y +" lines /!\\";
        	if(logOutput) { log.error(error); }
        }//end if
        
        if(logOutput) { log.info("getLogLines() | ... lines parsed"); }
        if(logOutput) { log.info("getLogLines() | returning <<"); } 
        return parsedLines;
    }//end getLogLines

    @Override
    public LogDataDto buildDto(LogData logData) {
        List<LogLineDto> logLineDtos = new ArrayList<>();
        for (LogLine logLine : logData.getLines()) {
            logLineDtos.add(logLineService.buildDto(logLine));
        }
        return LogDataDto.builder()
                .lines(logLineDtos)
                .build();
    }
}
