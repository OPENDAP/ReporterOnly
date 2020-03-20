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

package org.opendap.harvester.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Data transfer object for returning results form BE to FE.
 * 
 * @note The Anonymous log lines have nine fields each enclosed in []
 * (line breaks added for sanity's sake):
 * [Mozilla/5.0 ...] [366DFB37E0D4D83BE7C70266B74F267D] [2016-06-23T17:50:27.468 +0100] [   19 ms] 
 * [200] [      12] [GET] [/opendap/hyrax/data/nc/coads_climatology.nc.dods] 
 * [COADSX[0:1:179],COADSY[0:1:89],TIME[0:1:11]] 
 */

@Builder
@Getter @Setter
public class LogLineDto {
    private Map<String, String> values;
}
