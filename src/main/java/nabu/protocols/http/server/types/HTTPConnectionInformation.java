package nabu.protocols.http.server.types;

import java.util.List;

public class HTTPConnectionInformation extends ConnectionInformation {
	private List<HTTPRequestSummary> requests;
	private List<HTTPResponseSummary> responses;
	
	public List<HTTPRequestSummary> getRequests() {
		return requests;
	}
	public void setRequests(List<HTTPRequestSummary> requests) {
		this.requests = requests;
	}
	public List<HTTPResponseSummary> getResponses() {
		return responses;
	}
	public void setResponses(List<HTTPResponseSummary> responses) {
		this.responses = responses;
	}
}
