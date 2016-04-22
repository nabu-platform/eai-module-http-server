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
