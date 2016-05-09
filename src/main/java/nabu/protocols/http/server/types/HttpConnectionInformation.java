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
