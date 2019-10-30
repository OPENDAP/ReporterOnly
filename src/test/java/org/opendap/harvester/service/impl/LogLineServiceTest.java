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


import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendap.harvester.ReporterApplicationTest;
import org.opendap.harvester.entity.LinePatternConfig;
import org.opendap.harvester.service.LogLineService;
//import org.opendap.harvester.entity.LoConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ReporterApplicationTest.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebAppConfiguration
public class LogLineServiceTest {
    private static final String INCORRECT = "[0:0:0:0:0:0:0:1] [-] [-] [2016-03-29T11:43:43.422 -0700] [   13 ms] [200] [       8] [HTTP-GET] [/opendap/hyrax/data/nc/fnoc1.nc.dods] []";
    private static final String CORRECT = "[0:0:0:0:0:0:0:1] [-] [2016-03-29T11:43:43.422 -0700] [   13 ms] [200] [       8] [HTTP-GET] [/opendap/hyrax/data/nc/fnoc1.nc.dods] []";

    @Autowired
    private LogLineService logLineService;
    private LinePatternConfig logPatternConfig;


    @Test
    public void testThatCanNotParseNullLogLine() {
        assertNull(logLineService.parseLogLine(null, null));
    }

    @Test
    public void testThatCanNotParseEmptyLogLine() {
        assertNull(logLineService.parseLogLine(null, logPatternConfig));
    }

    @Test
    public void testThatCanNotParseIncorrectStructuredLogLine() {
        assertNull(logLineService.parseLogLine(INCORRECT, logPatternConfig));
    }

}
