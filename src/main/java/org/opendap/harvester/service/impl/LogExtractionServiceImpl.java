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
	//private static final Logger log = LoggerFactory.getLogger(ReporterApplication.class);
	
    @Autowired
    private LogLineService logLineService;

    @Autowired
    private ConfigurationExtractor configurationExtractor;

    @Override
    public LogData extractLogDataSince(LocalDateTime time) throws IOException {
    	//log.info("extract.1/2) extractLogDataSince() entry, building ..."); // <---
    	LogData logData = LogData.builder()
                .lines(getLogLines(time))
                .build();
    	
    	//log.info("extract.2/2) log data built, returning <<"); // <---
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
    	//log.info("getLL.1/5) getLogLines() entry, getting pattern ..."); // <---
        LinePattern linePattern = configurationExtractor.getLinePattern();
        //log.info("getLL.2/5) pattern retrieved, building config ..."); // <---
        LinePatternConfig config = LinePatternConfig.builder()
                .pattern(Pattern.compile(linePattern.getRegexp()))
                .names(linePattern.getNames().split(";"))
                .build();

        //log.info("getLL.3/5) pattern built, reading lines ..."); 
        //log.info("getLL.3.1) filepath : "+ configurationExtractor.getHyraxLogfilePath()); 
        List<String> allLines = Files.readAllLines(Paths.get(configurationExtractor.getHyraxLogfilePath()), Charset.defaultCharset());
        //log.info("getLL.4/5) lines read : "+allLines.size()+", value of 0 : "+ allLines.get(0)); 
        //log.info("getLL.4.0.1) parsing lines ..."); 
        List<LogLine> parsedLines = new ArrayList<>();
        //int x = 1; // <-- used in debugging, SBL - 7.2.19
        for (String line : allLines){
        	
        	if (line.trim().isEmpty()) { // <--- check for blank line before parsing
        		//log.info("getLL.4."+x+") blank line, skipping");
        		//x++; // <-- used in debugging, SBL - 7.2.19
        		continue;
        	}//end if - blank line test
        	
            LogLine parsedLogLine = logLineService.parseLogLine(line, config); //parse line
            //log.info("getLL.4."+x+") line : "+ parsedLogLine.getValues().toString()); //output values 
            boolean matched = !parsedLogLine.getValues().isEmpty(); //check if line was a match or not
            
            if (matched && (since == null || logLineService.getLocalDateTime(parsedLogLine).isAfter(since))){ 
            	//log.info("getLL.4."+x+") adding parsed line"); 
                parsedLines.add(parsedLogLine);
            }//end if - kosher line
            else if(!matched) { // <--- if not a match
            	//TODO output parse error to log file. sbl 7.2.19
            	//log.info("getLL.4."+x+") line did not match pattern"); 
            }//end if - non kosher line
            //x++; // <-- used in debugging, SBL - 7.2.19
        }//end for loop
        
        //log.info("getLL.5/5) lines parsed, returning <<"); 
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
