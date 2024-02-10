package be.nabu.eai.module.http.server.error;

import java.net.URI;

// https://www.rfc-editor.org/rfc/rfc9457.html
public interface StandardizedError {
	// it _can_ be a relative uri which will be resolved to the endpoint where the error occurred
	// but in most cases it should be an absolute URI
	public URI getType();
	
	// the http response code
	public Integer getStatus();
	
	/**
	 * The "title" member is a JSON string containing a short, human-readable summary of the problem type.
	 * It SHOULD NOT change from occurrence to occurrence of the problem, except for localization (e.g., using proactive content negotiation; see [HTTP], Section 12.1).
	 */
	public String getTitle();
	
	/**
	 * The "detail" member is a JSON string containing a human-readable explanation specific to this occurrence of the problem.
	 * The "detail" string, if present, ought to focus on helping the client correct the problem, rather than giving debugging information.
	 */
	public String getDetail();
	
	/**
	 * The "instance" member is a JSON string containing a URI reference that identifies the specific occurrence of the problem.
	 * When the "instance" URI is dereferenceable, the problem details object can be fetched from it. It might also return information about the problem occurrence in other formats through use of proactive content negotiation (see [HTTP], Section 12.5.1).
	 * When the "instance" URI is not dereferenceable, it serves as a unique identifier for the problem occurrence that may be of significance to the server but is opaque to the client.
	 */
	public URI getInstance();
}
