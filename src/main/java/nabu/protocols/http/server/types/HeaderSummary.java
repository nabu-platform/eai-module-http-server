/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package nabu.protocols.http.server.types;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.utils.mime.api.Header;

@XmlRootElement
@XmlType(propOrder = { "name", "value", "comments" })
public class HeaderSummary implements Header {

	private String name, value;
	private String [] comments;
	
	public static HeaderSummary build(Header header) {
		HeaderSummary summary = new HeaderSummary();
		summary.setName(header.getName());
		summary.setValue(header.getValue());
		summary.setComments(header.getComments());
		return summary;
	}
	
	@Override
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String[] getComments() {
		return comments;
	}
	public void setComments(String[] comments) {
		this.comments = comments;
	}
	

}
