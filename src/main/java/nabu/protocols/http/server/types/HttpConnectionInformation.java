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

import java.util.List;

public class HttpConnectionInformation extends ConnectionInformation {
	private List<HttpRequestSummary> requests;
	private List<HttpResponseSummary> responses;
	
	public List<HttpRequestSummary> getRequests() {
		return requests;
	}
	public void setRequests(List<HttpRequestSummary> requests) {
		this.requests = requests;
	}
	public List<HttpResponseSummary> getResponses() {
		return responses;
	}
	public void setResponses(List<HttpResponseSummary> responses) {
		this.responses = responses;
	}
}
