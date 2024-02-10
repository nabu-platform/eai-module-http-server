package be.nabu.eai.module.http.server;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class StandardizedError400 extends StandardizedErrorBase {

	public static class ValidationProblem {
		private String name, reason, type;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getReason() {
			return reason;
		}
		public void setReason(String reason) {
			this.reason = reason;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
	}
	
	private List<ValidationProblem> validationProblems;

	@XmlElement(name = "invalid-params")
	public List<ValidationProblem> getValidationProblems() {
		return validationProblems;
	}
	public void setValidationProblems(List<ValidationProblem> validationProblems) {
		this.validationProblems = validationProblems;
	}
	
	
}
