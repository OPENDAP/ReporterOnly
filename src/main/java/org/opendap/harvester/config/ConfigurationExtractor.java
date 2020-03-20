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

package org.opendap.harvester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendap.harvester.entity.LinePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * @brief Read configuration information needed by the reporter service.
 *
 * Look for the "olfs.xml" file and use that as the primary source of configuration
 * information. If that cannot be found, then use the default values baked in from
 * the application.properties file. To look for the olfs.xml file, first check the
 * value of the OLFS_CONFIG_DIR environment variable and, if that does not name a
 * valid directory, fallback to checking "/etc/olfs/".
 *
 * @todo This class has methods that trap exceptions. Review that.
 * @todo Add a check of the webapps/opendap/WEB_INF/conf dir to the list of places for config info
 * 
 */
@Component
public class ConfigurationExtractor {
    private static final String ENV_VAR_NAME = "OLFS_CONFIG_DIR";
    private static final String DEFAULT_CONFIG_DIR = "/etc/olfs/";
    private static final String DEFAULT_CONFIG_FILE = "olfs.xml";
    private static final String WEB_INF = "WEB-INF";
    private static final String OPENDAP_APPLICATION_NAME = "opendap";

    @Autowired
    private ServletContext servletContext;

    @Value("${logfile.pattern.path:}")
    private String logfilePatternPathFromProperties;

    @Value("${logfile.pattern.names:}")
    private String logfilePatternNamesFromProperties;

    @Value("${logfile.pattern.regexp:}")
    private String logfilePatternRegexpFromProperties;

    @Value("${hyrax.logfile.path:}")
    private String hyraxLogfilePathFromProperties;

    @Value("${hyrax.default.ping:}")
    private Long hyraxDefaultPingFromProperties;
    
    //SBL - used for the call to collector/registration on startup
    @Value("${collector.url}")
    private String hyraxDefaultCollectorUrlFromProperties;
    
    @Value("${collector.log.number}")
    private Integer hyraxDefaultLogNumberFromProperties;
    
    @Value("${collector.server.url}")
    private String hyraxDefaultServerUrlFromProperties;
    
    @Value("${collector.reporter.url}")
    private String hyraxDefaultReporterUrlFromProperties;

    private String hyraxLogfilePath = null;
    private Long hyraxDefaultPing = null;
    private String linePatternPath = null;
    private LinePattern linePattern = null;
    
    //SBL - used for the call to collector/registration on startup
    private String hyraxDefaultCollectorUrl = null;
    private String hyraxDefaultServerUrl = null;
    private String hyraxDefaultReporterUrl = null;
    private Integer hyraxDefaultLogNumber = null;

    private LinePattern getLinePatternDirectly() {
        LinePattern linePattern = extractLinePatternFormOlfsXml();
        return !isEmpty(linePattern.getNames()) && !isEmpty(linePattern.getRegexp()) ?
                linePattern :
                extractLinePatternFormProperties();
    }

    private LinePattern extractLinePatternFormOlfsXml(){
        return LinePattern.builder()
                .names(extractDataFromOlfsXml("/OLFSConfig/LogReporter/LogFilePattern/names").trim())
                .regexp(extractDataFromOlfsXml("/OLFSConfig/LogReporter/LogFilePattern/regexp").trim())
                .build();
    }

    private LinePattern extractLinePatternFormProperties(){
        return LinePattern.builder()
                .names(logfilePatternNamesFromProperties)
                .regexp(logfilePatternRegexpFromProperties)
                .build();
    }

    /**
     * Get the pathname to the JSON file that contains the log file field regex and
     * names. Look in the configuration file (olfs.xml) and default to the 
     * application.properties.
     * 
     * @return Return the name of the JSON file or an empty string.
     */
    private String getLinePatternPath() {
        if (linePatternPath != null){
            return linePatternPath;
        }
        String linePatternPathFromConfig = extractDataFromOlfsXml("/OLFSConfig/LogReporter/LogFilePatternPath").trim();
        linePatternPath = !isEmpty(linePatternPathFromConfig)
                ?  linePatternPathFromConfig
                : logfilePatternPathFromProperties;
        return linePatternPath;
    }

    /**
     * If a path to the log file fields (filed regex and names) file (a json file) is
     * given, use that. If that is not set, look for regex and field names themselves
     * in the configuration or application.properties file.
     * 
     * @note I think the JSON file should be dropped - the regex and names should be 
     * read from the config file (olfs.xml) or the application.properties.
     * 
     * @todo Handle the error when the path is set, but the file is missing
     * @todo Handle the error when the pattern information cannot be found.
     * @todo getLinePatternDirectly() is never called if the value for the JSON file is set in the properties.
     *  
     * @return A LinePattern object or null if the information cannot be found.
     */
    public LinePattern getLinePattern() {
        if (linePattern != null) {
            return linePattern;
        }
        try {
            if (isEmpty(getLinePatternPath())) {
                linePattern = getLinePatternDirectly();
            } else {
                linePattern = new ObjectMapper().readValue(new File(getLinePatternPath()), LinePattern.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return linePattern;
    }

    /**
     * What is the 'ping' interval for the reporter? If the value cannot be read
     * from a configuration file, return the value from the application.properties
     * file.
     *
     * @return The ping interval, in seconds.
     */
    public Long getDefaultPing() {
        if (hyraxDefaultPing != null) {
            return hyraxDefaultPing;
        }
        
        String hyraxDefaultPingFromConfig = extractDataFromOlfsXml("/OLFSConfig/LogReporter/DefaultPing").trim();
        hyraxDefaultPing = !isEmpty(hyraxDefaultPingFromConfig)
                ?  Long.valueOf(hyraxDefaultPingFromConfig)
                : hyraxDefaultPingFromProperties;
        return hyraxDefaultPing;
    }
    
    /**
     * getCollectorUrl method
     * 		retrieves url of collector application from olfs.xml config file
     * 		or from application.properties file if olfs file cannot be found
     * @return string of the url of the collector.
     * 
     * 1/31/19 - SBL - initial code
     */
    public String getCollectorUrl() {
    	if (hyraxDefaultCollectorUrl != null) {
    		return hyraxDefaultCollectorUrl;
    	}
    	
    	String hyraxDefaultCollectorFromConfig = extractDataFromOlfsXml("/OLFSConfig/LogReporter/CollectorUrl").trim();
    	hyraxDefaultCollectorUrl = !isEmpty(hyraxDefaultCollectorFromConfig)
    			? hyraxDefaultCollectorFromConfig 
    			: hyraxDefaultCollectorUrlFromProperties;
    	
    	return hyraxDefaultCollectorUrl;
    }
    
    /**
     * getServerUrl method
     * 		retrieves url of server from olfs.xml config file
     * 		or from application.properties file if olfs file cannot be found
     * @return string of the url of the server
     * 
     */
    public String getServerUrl() {
    	if (hyraxDefaultServerUrl != null) {
    		return hyraxDefaultServerUrl;
    	}
    	
    	String hyraxDefaultServerUrlFromConfig = extractDataFromOlfsXml("/OLFSConfig/LogReporter/ServerUrl").trim();
    	hyraxDefaultServerUrl = !isEmpty(hyraxDefaultServerUrlFromConfig)
    			? hyraxDefaultServerUrlFromConfig
    			: hyraxDefaultServerUrlFromProperties;
    	return hyraxDefaultServerUrl;
    }
    
    /**
     * getReporterUrl method
     * 		retrieves url of reporter from olfs.xml config file
     * 		or from application.properties file if olfs file cannot be found
     * @return string of the url of the reporter
     * 
     */
    public String getReporterUrl() {
    	if (hyraxDefaultReporterUrl != null) {
    		return hyraxDefaultReporterUrl;
    	}
    	
    	String hyraxDefaultReporterUrlFromConfig = extractDataFromOlfsXml("/OLFSConfig/LogReporter/ReporterUrl").trim();
    	hyraxDefaultReporterUrl = !isEmpty(hyraxDefaultReporterUrlFromConfig)
    			? hyraxDefaultReporterUrlFromConfig
    			: hyraxDefaultReporterUrlFromProperties;
    	return hyraxDefaultReporterUrl;
    }
    
    /**
     * getLogNumber method
     * 		retrieves log number from olfs.xml config file
     * 		or from application.properties file if olfs file cannot be found
     * @return integer of the number of log to retrieve
     * 
     */
    public Integer getLogNumber() {
    	if (hyraxDefaultLogNumber != null) {
    		return hyraxDefaultLogNumber;
    	}
    	
    	String hyraxDefaultLogNumberFromConfig = extractDataFromOlfsXml("/OLFSConfig/LogReporter/LogNumber").trim();
    	hyraxDefaultLogNumber = !isEmpty(hyraxDefaultLogNumberFromConfig)
    			? Integer.valueOf(hyraxDefaultLogNumberFromConfig)
    			: hyraxDefaultLogNumberFromProperties;
    	return hyraxDefaultLogNumber;
    }

    /**
     * What is the pathname to the log file this reporter will read from?
     * If the name cannot be read from a configuration file, return the value
     * set in the application.properties file.
     *
     * @return The pathname to the log file.
     */
    public String getHyraxLogfilePath() {
        if (hyraxLogfilePath != null) {
            return hyraxLogfilePath;
        }

        String hyraxLogfilePathFromConfig = extractDataFromOlfsXml("/OLFSConfig/LogReporter/HyraxLogfilePath").trim();
        hyraxLogfilePath = !isEmpty(hyraxLogfilePathFromConfig)
                ? hyraxLogfilePathFromConfig
                : hyraxLogfilePathFromProperties;
        if (isEmpty(hyraxLogfilePath)){
            throw new IllegalStateException("Can not find HyraxLogfilePath property");
        }
        return hyraxLogfilePath;
    }

    /**
     * Read configuration information from the olfs.xml file.
     *
     * Read configuration information from the "olfs.xml" (or the DEFAULT_CONFIG_FILE).
     * If the configuration file cannot be found or does not contain the information,
     * return the empty string (not a null).
     *
     * @param xPathRoute The XPath to an element in the olfs.xml file.
     * @return The value of the element or the empty string.
     */
    private String extractDataFromOlfsXml(String xPathRoute) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String elementValue = null;
        try {
            String configDir = getConfigDir();
            if (configDir != null) {
                configDir += DEFAULT_CONFIG_FILE;
                // COMPLETED Add test that the file exists and is readable. jhrg 10/4/17
                File tmp = new File(configDir);
                if (tmp.exists() && tmp.canRead()) {
                	elementValue = xPath.compile(xPathRoute).evaluate(loadXMLFromFile(configDir));
                }
            }
            /* Removed because XPath...evaluate() might return null.
            else {
                elementValue = "";
            } */
        } catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
            //e.printStackTrace();
            e.getMessage();
        }

        return elementValue != null ? elementValue : "";
    }

    /**
     * Read and parse an XML file.
     *
     * @param xmlFile The pathname to the file
     * @return A Document instance
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private Document loadXMLFromFile(String xmlFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new FileReader(xmlFile));
        return builder.parse(is);
    }

    /**
     * Look for the directory that holds the reporter's configuration information.
     *
     * First look in the directory named in the OLFS_CONFIG_DIR environment
     * variable, otherwise look in "/etc/olfs/", otherwise look in the 'opendap'
     * web service's "WEB_INF/conf/" directory. Return null as a fallback.
     *
     * @return The pathname or null if no suitable directory can be found.
     * @note Uses pathIsGood() to determine if the given directory is a config
     * directory for the reporter.
     */
    private String getConfigDir() {
        String configDirName = System.getenv(ENV_VAR_NAME);
        if (configDirName == null) {
            configDirName = DEFAULT_CONFIG_DIR;
            if (pathIsGood(configDirName)) {
                return configDirName;
            }
        } else {
            if (!configDirName.endsWith("/")) {
                configDirName += "/";
            }
            if (pathIsGood(configDirName)) {
                return configDirName;
            }
        }

        // Trick: the reporter web service directory is probably in the same place
        // as the 'opendap' directory. Get 'reporter/WEB_INF", go up two levels and
        // then descend into "opendap/WEB_INF/conf"
        configDirName = servletContext.getRealPath(WEB_INF);
        File cf = new File(configDirName);
        try {
            File webappsFolder = cf.getParentFile().getParentFile();
            String configPath = webappsFolder.getCanonicalPath() +
                    File.separator + OPENDAP_APPLICATION_NAME +
                    File.separator + WEB_INF +
                    File.separator + "conf";
            return pathIsGood(configPath) ? configPath : null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Is the given pathname a directory from which this process can read?
     *
     * @param path The pathname
     * @return True is the directory exists and is readable, False otherwise.
     */
    private boolean pathIsGood(String path) {
        File confDirPath = new File(path);
        return confDirPath.exists() || confDirPath.canRead();
    }

}
