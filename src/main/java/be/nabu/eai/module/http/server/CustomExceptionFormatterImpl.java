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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.module.http.server.RepositoryExceptionFormatter.ExceptionHTTPResponse;
import be.nabu.eai.module.http.server.RepositoryExceptionFormatter.TemplateMarshallableBinding;
import be.nabu.eai.module.http.server.StandardizedError400.ValidationProblem;
import be.nabu.eai.module.http.server.error.CustomExceptionFormatter;
import be.nabu.eai.module.http.server.error.StandardizedError;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.TypeBaseUtils;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.structure.StructureInstanceDowncastReference;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class CustomExceptionFormatterImpl implements CustomExceptionFormatter {

	@Override
	public HTTPResponse format(HTTPRequest request, HTTPException rootException, ServiceException serviceException, WhitelistLevel whitelist, int httpCode, String errorCode, String identifier) {
		StandardizedErrorBase standardized;
		// validation exceptions
		if (rootException.getCode() == 400 && serviceException != null && serviceException.getValidations() != null && !serviceException.getValidations().isEmpty()) {
			StandardizedError400 exception = new StandardizedError400();
			List<ValidationProblem> problems = new ArrayList<ValidationProblem>();
			for (Validation<?> validation : serviceException.getValidations()) {
				// we only care about the bad ones!
				if (validation.getSeverity() == null || validation.getSeverity().ordinal() < Severity.ERROR.ordinal()) {
					continue;
				}
				ValidationProblem problem = new ValidationProblem();
				if (validation.getContext() != null && !validation.getContext().isEmpty()) {
					StringBuilder builder = new StringBuilder();
					boolean first = true;
					for (int i = validation.getContext().size() - 1; i >= 0; i--) {
						Object object = validation.getContext().get(i);
						if (object instanceof Number) {
							builder.append("[" + object + "]");
						}
						else {
							if (first) {
								first = false;
							}
							else {
								builder.append("/");
							}
							builder.append(object);
						}
					}
					problem.setName(builder.toString());
				}
				problem.setType(validation.getCode());
//				problem.setReason(validation.getMessage());
				
				problems.add(problem);
			}
			exception.setValidationProblems(problems);
			if (errorCode == null) {
				errorCode = "VALIDATION-ERROR";
			}
			standardized = exception;
		}
		else {
			standardized = new StandardizedErrorBase();
		}
		
		if (identifier != null) {
			try {
				standardized.setInstance(new URI(identifier));
			}
			catch (URISyntaxException e) {
				// ignore
				e.printStackTrace();
			}
		}
		
		// set the code
		if (errorCode != null) {
			try {
				standardized.setType(new URI(errorCode));
			}
			catch (URISyntaxException e) {
				// ignore
				e.printStackTrace();
			}
		}
		
		// set the title
		if (whitelist.ordinal() >= WhitelistLevel.LIMITED.ordinal()) {
			// TODO: potentially translate?
			String plainMessage = serviceException.getPlainMessage();
			// the blox "throw" will set "no message" if there actually no message
			if (plainMessage != null && !plainMessage.equalsIgnoreCase("No message")) {
				standardized.setTitle(serviceException.getPlainMessage());
			}
		}
		else {
			standardized.setTitle(HTTPCodes.getMessage(httpCode));
		}
		
		// set the detail
		if (whitelist.ordinal() >= WhitelistLevel.FULL.ordinal()) {
			// TODO: potentially translate?
			standardized.setDetail(serviceException.getDescription());
		}
		else if (EAIResourceRepository.isDevelopment() && (serviceException != null || rootException != null)) {
			standardized.setDetail(RepositoryExceptionFormatter.stacktrace(serviceException == null ? rootException : serviceException));
		}
		
		// set the status
		standardized.setStatus(httpCode);

		ComplexContent responseContent = new BeanInstance<StandardizedError>(standardized);
		
		if (whitelist.ordinal() >= WhitelistLevel.LIMITED.ordinal() && serviceException != null && serviceException.getData() != null) {
			responseContent = enrichWithData(responseContent, serviceException);
		}
		
		return new ExceptionHTTPResponse(request, httpCode, HTTPCodes.getMessage(httpCode), serialize(request, responseContent), rootException, null);
	}
	
	private ModifiablePart serialize(HTTPRequest request, ComplexContent content) {
		List<String> requestedTypes = new ArrayList<String>();
		List<String> requestedCharsets = new ArrayList<String>();
		if (request != null && request.getContent() != null) {
			requestedTypes.addAll(MimeUtils.getAcceptedContentTypes(request.getContent().getHeaders()));
			requestedCharsets.addAll(MimeUtils.getAcceptedCharsets(request.getContent().getHeaders()));
		}
		Charset charset = Charset.defaultCharset();
		if (!requestedCharsets.isEmpty()) {
			try {
				charset = Charset.forName(requestedCharsets.get(0));
			}
			catch (Exception e) {
				// ignore
			}
		}
		MarshallableBinding binding = null;
		String contentType = "text/html";
		
		// a default browser will likely send both a request for HTML and XML but should (hopefully) be in the correct order
		// note that they do generally send a q but that requires further processing
		int htmlIndex = requestedTypes.indexOf("text/html");
		int xmlIndex = requestedTypes.indexOf("application/xml");
		int jsonIndex = requestedTypes.indexOf("application/json");
		// if you requested json and it is before the html (if any), use that
		if (jsonIndex >= 0 && (htmlIndex < 0 || jsonIndex < htmlIndex)) {
			binding = new JSONBinding(content.getType(), charset);
			contentType = "application/problem+json";
		}
		else if (xmlIndex >= 0 && (htmlIndex < 0 || xmlIndex < htmlIndex)) {
			binding = new XMLBinding(content.getType(), charset);
			contentType = "application/xml";
		}
		if (binding == null) {
			binding = new TemplateMarshallableBinding("<html><head><title>[${status}] ${title}</title></head><body><h1>[${status}] ${title} (${instance})</h1><pre>${detail}</pre></body></html>", charset);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			binding.marshal(output, content);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte[] bytes = output.toByteArray();
		PlainMimeContentPart part = new PlainMimeContentPart(null, IOUtils.wrap(bytes, true), 
			new MimeHeader("Connection", "close"),
			new MimeHeader("Content-Length", "" + bytes.length),
			new MimeHeader("Content-Type", contentType + "; charset=" + charset.displayName())
		);
		// we can reopen this!
		part.setReopenable(true);
		return part;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ComplexContent enrichWithData(ComplexContent responseContent, ServiceException serviceException) {
		Structure responseExtension = new Structure();
		responseExtension.setName("exception");
		responseExtension.setSuperType(responseContent.getType());
		
		ComplexContent newInstance = StructureInstanceDowncastReference.downcast(responseContent, responseExtension);
		
		DefinedSimpleType<? extends Object> wrap = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(serviceException.getData().getClass());
		// we have simple content
		if (wrap != null) {
			responseExtension.add(new SimpleElementImpl<String>("additional", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), responseExtension));
			if (wrap instanceof Marshallable) {
				newInstance.set("additional", ((Marshallable) wrap).marshal(serviceException.getData()));
			}
			else {
				newInstance.set("additional", (ConverterFactory.getInstance().getConverter().convert(serviceException.getData(), String.class)));
			}
		}
		else {
			ComplexContent data = (!(serviceException.getData() instanceof ComplexContent)) ? ComplexContentWrapperFactory.getInstance().getWrapper().wrap(serviceException.getData()) : (ComplexContent) serviceException.getData();
			if (data != null) {
				for (Element<?> child : TypeUtils.getAllChildren(data.getType())) {
					if (responseExtension.get(child.getName()) == null) {
						responseExtension.add(TypeBaseUtils.clone(child, responseExtension));
					}
				}
				for (Element<?> child : TypeUtils.getAllChildren(data.getType())) {
					newInstance.set(child.getName(), data.get(child.getName()));
				}
			}
		}
		return newInstance;
	}

}
