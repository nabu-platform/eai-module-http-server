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
