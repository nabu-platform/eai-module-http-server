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

package be.nabu.eai.module.http.server.error;

import javax.jws.WebParam;
import javax.jws.WebResult;

import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.services.api.ServiceException;

public interface CustomExceptionFormatter {
	
	public enum WhitelistLevel {
		NONE,
		LIMITED,
		FULL
	}
	
	@WebResult(name = "response")
	public HTTPResponse format(
		@WebParam(name = "request") HTTPRequest request, 
		@WebParam(name = "rootException") HTTPException rootException, 
		@WebParam(name = "serviceException") ServiceException serviceException,
		// whether or not this exception should reveal all or hide all
		@WebParam(name = "whitelist") WhitelistLevel whitelist,
		@WebParam(name = "httpCode") int httpCode,
		@WebParam(name = "errorCode") String errorCode,
		@WebParam(name = "identifier") String identifier);

}
