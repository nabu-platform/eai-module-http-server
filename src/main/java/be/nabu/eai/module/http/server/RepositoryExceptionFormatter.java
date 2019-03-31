package be.nabu.eai.module.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.Notification;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPFormatter;
import be.nabu.libs.nio.api.ExceptionFormatter;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

/**
 * Retroactively adjusted to support spec: https://tools.ietf.org/html/rfc7807#page-9
 * There are some legacy fields that should not be used and removed
 */
public class RepositoryExceptionFormatter implements ExceptionFormatter<HTTPRequest, HTTPResponse> {

	private Boolean useProblemJson = Boolean.parseBoolean(System.getProperty("problemJson", "true"));
	private Logger logger = LoggerFactory.getLogger(getClass());
	private Map<Integer, String> errorTemplates = new HashMap<Integer, String>();
	private String defaultErrorTemplate = "<html><head><title>${code}: ${message}</title></head><body><h1>${code}: ${message}</h1><pre>${description}</pre></body></html>";
	
	private List<String> whitelistedCodes = new ArrayList<String>();
	private Map<String, List<String>> artifactCodes = new HashMap<String, List<String>>();
	private HTTPServerArtifact server;
	
	public RepositoryExceptionFormatter(HTTPServerArtifact server) {
		this.server = server;
	}
	
	private Device getDevice(Throwable e) {
		if (e instanceof HTTPException) {
			Device device = ((HTTPException) e).getDevice();
			if (device != null) {
				return device;
			}
		}
		if (e.getCause() != null) {
			return getDevice(e.getCause());
		}
		else {
			return null;
		}
	}
	
	private Token getToken(Throwable e) {
		Token token = null;
		if (e instanceof HTTPException) {
			token = ((HTTPException) e).getToken();
		}
		else if (e instanceof ServiceException) {
			token = ((ServiceException) e).getToken();
		}
		if (token != null) {
			return token;
		}
		else if (e.getCause() != null) {
			return getToken(e.getCause());
		}
		else {
			return null;
		}
	}
	
	private String stringify(HTTPRequest request) {
		// ignore internal headers, they might contain identifiable information
		HTTPFormatter formatter = new HTTPFormatter(true);
		// do not allow binary, we are stringifying the request
		formatter.getFormatter().setAllowBinary(false);
		// do not allow cookies to be stored, for GDPR reasons (they might contain identifiable information)
		formatter.getFormatter().ignoreHeaders("Cookie");
		formatter.getFormatter().ignoreHeaders("Authorization");
		ByteBuffer byteBuffer = IOUtils.newByteBuffer();
		try {
			formatter.formatRequest(request, byteBuffer);
			return new String(IOUtils.toBytes(byteBuffer), "UTF-8");
		}
		catch (Exception e) {
			logger.warn("Can not serialize request input", e);
			return null;
		}
	}
	
	private void getContext(Throwable exception, List<String> context) {
		if (exception instanceof HTTPException) {
			for (String single : ((HTTPException) exception).getContext()) {
				if (!context.contains(single)) {
					context.add(single);
				}
			}
		}
		if (exception instanceof ServiceException) {
			for (String single : ((ServiceException) exception).getServiceStack()) {
				// they are ordered in reverse
				if (!context.contains(single)) {
					context.add(0, single);
				}
			}
		}
		if (exception.getCause() != null) {
			getContext(exception.getCause(), context);
		}
	}
	
	@Override
	public HTTPResponse format(HTTPRequest request, Exception originalException) {
		Token token = getToken(originalException);
		Device device = getDevice(originalException);
		
		List<String> context = new ArrayList<String>();
		getContext(originalException, context);
		// we want most specific to least
		Collections.reverse(context);
		// add the server context
		context.add(server.getId());
		
		Notification notification = new Notification();
		
		HTTPException exception = originalException instanceof HTTPException ? (HTTPException) originalException : new HTTPException(500, originalException);
		ServiceException serviceException = getServiceException(exception);
		
		ExceptionSummary exceptionSummary = new ExceptionSummary();
		// get the full stack trace
		exceptionSummary.setStacktrace(stacktrace(exception));
		// but then we start digging for the actual http exception
		exception = getHTTPException(exception);
		
		exceptionSummary.setStatus(exception.getCode());
		exceptionSummary.setIdentifier(notification.getIdentifier());
		
		boolean isHTTPCode = serviceException != null && serviceException.getCode().matches("^4[0-9]{2}$");
		int httpCode = exception.getCode() == 500 && isHTTPCode ? Integer.parseInt(serviceException.getCode()) : exception.getCode();
		if (serviceException != null) {
			exceptionSummary.setCode(serviceException.getCode());
			exceptionSummary.setMessage(serviceException.getMessage());
			exceptionSummary.setDescription(serviceException.getDescription());
			exceptionSummary.setServiceStack(serviceException.getServiceStack());
		}
		else {
			exceptionSummary.setCode("HTTP-" + httpCode);
			exceptionSummary.setMessage(HTTPCodes.getMessage(exception.getCode()) + ": " + exception.getMessage());
		}
		
		if (server.getConfig().getErrorInstanceUri() != null) {
			try {
				exceptionSummary.setInstance(new URI(URIUtils.encodeURI(URIUtils.decodeURI(server.getConfig().getErrorInstanceUri().toString())
					.replace("{code}", exceptionSummary.getCode())
					.replace("{status}", Integer.toString(exceptionSummary.getStatus())
					.replace("{identifier}", notification.getIdentifier())
				))));
			}
			catch (Exception e) {
				logger.warn("Could not set exception instance", e);
			}
		}
		
		if (server.getConfig().getErrorTypeUri() != null) {
			try {
				exceptionSummary.setType(new URI(URIUtils.encodeURI(URIUtils.decodeURI(server.getConfig().getErrorTypeUri().toString())
					.replace("{code}", exceptionSummary.getCode())
					.replace("{status}", Integer.toString(exceptionSummary.getStatus())
				))));
			}
			catch (Exception e) {
				logger.warn("Could not set exception type", e);
			}
		}
		
		try {
			HTTPErrorNotification content = new HTTPErrorNotification();
			content.setCreated(request instanceof DefaultHTTPRequest ? ((DefaultHTTPRequest) request).getCreated() : new Date());
			content.setMethod(request.getMethod());
			content.setTarget(request.getTarget());
			content.setVersion(request.getVersion());
			
			// set the user agent if possible
			Header userAgent = request.getContent() != null ? MimeUtils.getHeader("User-Agent", request.getContent().getHeaders()) : null;
			if (userAgent != null) {
				content.setUserAgent(MimeUtils.getFullHeaderValue(userAgent));
			}
			Header referer = request.getContent() != null ? MimeUtils.getHeader("Referer", request.getContent().getHeaders()) : null;
			if (referer != null) {
				content.setReferer(MimeUtils.getFullHeaderValue(referer));
			}
			Header host = request.getContent() != null ? MimeUtils.getHeader("Host", request.getContent().getHeaders()) : null;
			if (host != null) {
				content.setHost(MimeUtils.getFullHeaderValue(host));
			}
			
			content.setExceptionSummary(exceptionSummary);
			
			// only capture the entire request in case of these generic error codes
			if (exception.getCode() == 400 || exception.getCode() == 500) {
				content.setRequest(stringify(request));
			}
			
			// fire a notification
			notification.setContext(context);
			notification.setType("http.server");
			notification.setCode(exception.getCode());
			notification.setProperties(content);
			notification.setMessage("Request failed");
			notification.setDescription(Notification.format(originalException));
			notification.setSeverity(Severity.ERROR);
			if (token != null) {
				notification.setRealm(token.getRealm());
				notification.setAlias(token.getName());
			}
			if (device != null) {
				notification.setDeviceId(device.getDeviceId());
			}
			server.getRepository().getEventDispatcher().fire(notification, server);
		}
		catch (Exception e) {
			logger.error("Could not send notification", e);
		}
		
		StructuredResponse response = new StructuredResponse();
		response.setStatus(httpCode);
		// an identifier that we send to the client and that we also send in the notification
		response.setIdentifier(notification.getIdentifier());
		response.setType(exceptionSummary.getType());
		response.setInstance(exceptionSummary.getInstance());
		
		// we have a service exception that can be reported
		if (serviceException != null && whitelistedCodes.contains(serviceException.getCode())) {
			response.setCode(serviceException.getCode());
			if (useProblemJson) {
				response.setTitle(serviceException.getPlainMessage());
			}
			else {
				response.setMessage(serviceException.getPlainMessage());
			}
			response.setDetail(serviceException.getDescription());
			if (EAIResourceRepository.isDevelopment()) {
				response.setDescription(serviceException.getServiceStack() + "\n\n" + stacktrace(exception));
			}
		}
		else {
			response.setCode("HTTP-" + httpCode);
			if (useProblemJson) {
				response.setTitle(HTTPCodes.getMessage(httpCode));
			}
			else {
				response.setMessage(HTTPCodes.getMessage(httpCode));
			}
			if (EAIResourceRepository.isDevelopment()) {
				response.setDescription(stacktrace(exception));
			}
		}
		
		logger.error("HTTP Exception " + exception.getCode(), exception);
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
		ComplexType resolved = (ComplexType) BeanResolver.getInstance().resolve(StructuredResponse.class);
		// a default browser will likely send both a request for HTML and XML but should (hopefully) be in the correct order
		// note that they do generally send a q but that requires further processing
		int htmlIndex = requestedTypes.indexOf("text/html");
		int xmlIndex = requestedTypes.indexOf("application/xml");
		int jsonIndex = requestedTypes.indexOf("application/json");
		// if you requested json and it is before the html (if any), use that
		if (jsonIndex >= 0 && (htmlIndex < 0 || jsonIndex < htmlIndex)) {
			binding = new JSONBinding(resolved, charset);
			contentType = useProblemJson ? "application/problem+json" : "application/json";
		}
		else if (xmlIndex >= 0 && (htmlIndex < 0 || xmlIndex < htmlIndex)) {
			binding = new XMLBinding(resolved, charset);
			contentType = "application/xml";
		}
		if (binding == null) {
			binding = new TemplateMarshallableBinding(errorTemplates.containsKey(exception.getCode()) ? errorTemplates.get(exception.getCode()) : defaultErrorTemplate, charset);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			binding.marshal(output, new BeanInstance<StructuredResponse>(response));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte [] bytes = output.toByteArray();
		return new ExceptionHTTPResponse(request, httpCode, HTTPCodes.getMessage(exception.getCode()), new PlainMimeContentPart(null, IOUtils.wrap(bytes, true), 
			new MimeHeader("Connection", "close"),
			new MimeHeader("Content-Length", "" + bytes.length),
			new MimeHeader("Content-Type", contentType + "; charset=" + charset.displayName())
		), exception, response);
	}

	private String stacktrace(HTTPException exception) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printer = new PrintWriter(stringWriter);
		exception.printStackTrace(printer);
		printer.flush();
		return stringWriter.toString();
	}
	
	private ServiceException getServiceException(Throwable throwable) {
		ServiceException serviceException = null;
		// the deepest service exception (if there are multiple) is what we are interested in
		while(throwable != null) {
			if (throwable instanceof ServiceException && ((ServiceException) throwable).getCode() != null) {
				serviceException = (ServiceException) throwable;
			}
			throwable = throwable.getCause();
		}
		return serviceException;
	}
	
	private HTTPException getHTTPException(Throwable throwable) {
		HTTPException deepest = null;
		while (throwable != null) {
			if (throwable instanceof HTTPException) {
				deepest = (HTTPException) throwable;
			}
			throwable = throwable.getCause();
		}
		return deepest;
	}
	
	public void register(String artifact, Collection<String> codes) {
		if (artifactCodes.containsKey(artifact)) {
			whitelistedCodes.removeAll(artifactCodes.get(artifact));
			artifactCodes.get(artifact).clear();
		}
		else {
			artifactCodes.put(artifact, new ArrayList<String>());
		}
		if (codes != null) {
			artifactCodes.get(artifact).addAll(codes);
			whitelistedCodes.addAll(codes);
		}
	}
	
	public void unregister(String artifact) {
		if (artifactCodes.containsKey(artifact)) {
			whitelistedCodes.removeAll(artifactCodes.get(artifact));
			artifactCodes.remove(artifact);
		}
	}
	
	@XmlRootElement(name = "exception")
	@XmlType(propOrder = { "type", "instance", "status", "title", "detail", "identifier", "code", "message", "description" })
	public static class StructuredResponse {
		// a url to a human readable description of the error
		private URI type;
		// a url where this particular instance of the error is explained
		private URI instance;
		// the http status
		private int status;
		private String title, detail;
		
		// not supported by spec: https://tools.ietf.org/html/rfc7807#page-9
		private String code, message, description, identifier;
		
		@NotNull
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		
		@NotNull
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		
		public URI getType() {
			return type;
		}
		public void setType(URI type) {
			this.type = type;
		}
		public URI getInstance() {
			return instance;
		}
		public void setInstance(URI instance) {
			this.instance = instance;
		}
		public int getStatus() {
			return status;
		}
		public void setStatus(int status) {
			this.status = status;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getDetail() {
			return detail;
		}
		public void setDetail(String detail) {
			this.detail = detail;
		}
		public String getIdentifier() {
			return identifier;
		}
		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}
		
	}
	
	public static class ExceptionHTTPResponse extends DefaultHTTPResponse {
		
		private StructuredResponse structured;
		private HTTPException exception;

		public ExceptionHTTPResponse(HTTPRequest request, int code, String message, ModifiablePart content, HTTPException exception, StructuredResponse response) {
			super(request, code, message, content);
			this.exception = exception;
			structured = response;
		}
		
		public ExceptionHTTPResponse(int code, String message, ModifiablePart content, HTTPException exception, StructuredResponse response) {
			this(null, code, message, content, exception, response);
		}
		
		public StructuredResponse getStructured() {
			return structured;
		}
		public HTTPException getException() {
			return exception;
		}
	}
	
	public static class TemplateMarshallableBinding implements MarshallableBinding {

		private String template;
		private Charset charset;
		
		public TemplateMarshallableBinding(String template, Charset charset) {
			this.template = template;
			this.charset = charset;
		}
		
		@Override
		public void marshal(OutputStream arg0, ComplexContent arg1, Value<?>... arg2) throws IOException {
			String content = template;
			for (Element<?> child : TypeUtils.getAllChildren(arg1.getType())) {
				Object object = arg1.get(child.getName());
				content = content.replace("${" + child.getName() + "}", object == null ? "" : object.toString());
			}
			arg0.write(content.getBytes(charset));
		}
	}
}
