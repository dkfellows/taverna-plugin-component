/**
 * 
 */
package net.sf.taverna.t2.component.profile;

import static java.util.Locale.UK;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.log4j.Logger.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import net.sf.taverna.t2.component.api.ComponentException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import uk.org.taverna.configuration.app.ApplicationConfiguration;

/**
 * @author alanrw
 * 
 */
public class BaseProfileLocator {
	private static final String BASE_PROFILE_PATH = "BaseProfile.xml";
	private static final String BASE_PROFILE_URI = "http://build.mygrid.org.uk/taverna/BaseProfile.xml";
	private static final int TIMEOUT = 5000;
	private static final String pattern = "EEE, dd MMM yyyy HH:mm:ss z";
	private static final SimpleDateFormat format = new SimpleDateFormat(
			pattern, UK);

	private Logger logger = getLogger(BaseProfileLocator.class);
	private ApplicationConfiguration appConfig;
	private ComponentProfileImpl profile;

	private void locateBaseProfile() {
		File baseProfileFile = getBaseProfileFile();
		@SuppressWarnings("unused")
		boolean load = false;
		Long remoteBaseProfileTime = null;
		long localBaseProfileTime = -1;

		HttpClientParams params = new HttpClientParams();
		params.setConnectionManagerTimeout(TIMEOUT);
		params.setSoTimeout(TIMEOUT);
		HttpClient client = new HttpClient(params);

		try {
			remoteBaseProfileTime = getRemoteBaseProfileTimestamp(client);
			logger.info("NoticeTime is " + remoteBaseProfileTime);
		} catch (URISyntaxException e) {
			logger.error("URI problem", e);
		} catch (IOException e) {
			logger.info("Could not read base profile", e);
		} catch (ParseException e) {
			logger.error("Could not parse last-modified time", e);
		}
		if (baseProfileFile.exists())
			localBaseProfileTime = baseProfileFile.lastModified();

		try {
			if ((remoteBaseProfileTime != null)
					&& (remoteBaseProfileTime > localBaseProfileTime)) {
				profile = new ComponentProfileImpl(null, new URL(BASE_PROFILE_URI),
						null);
				writeStringToFile(baseProfileFile, profile.getXML());
			}
		} catch (MalformedURLException e) {
			logger.error("URI problem", e);
			profile = null;
		} catch (ComponentException e) {
			logger.error("Component Registry problem", e);
			profile = null;
		} catch (IOException e) {
			logger.error("Unable to write profile", e);
			profile = null;
		}

		try {
			if ((profile == null) && baseProfileFile.exists())
				profile = new ComponentProfileImpl(null, baseProfileFile.toURI()
						.toURL(), null);
		} catch (Exception e) {
			logger.error("URI problem", e);
			profile = null;
		}
	}

	private long parseTime(String timestamp) throws ParseException {
		timestamp = timestamp.trim();
		if (timestamp.endsWith(" GMT"))
			timestamp = timestamp.substring(0, timestamp.length() - 3)
					+ " +0000";
		else if (timestamp.endsWith(" BST"))
			timestamp = timestamp.substring(0, timestamp.length() - 3)
					+ " +0100";
		return format.parse(timestamp).getTime();
	}

	private long getRemoteBaseProfileTimestamp(HttpClient client)
			throws URISyntaxException, IOException, HttpException,
			ParseException {
		URI baseProfileURI = new URI(BASE_PROFILE_URI);
		HttpMethod method = new GetMethod(baseProfileURI.toString());
		int statusCode = client.executeMethod(method);
		if (statusCode != SC_OK) {
			logger.warn("HTTP status " + statusCode + " while getting "
					+ baseProfileURI);
			return -1;
		}
		Header h = method.getResponseHeader("Last-Modified");
		if (h == null)
			return -1;
		return parseTime(h.getValue());
	}

	private File getBaseProfileFile() {
		File config = new File(appConfig.getApplicationHomeDir(), "conf");
		if (!config.exists())
			config.mkdir();
		return new File(config, BASE_PROFILE_PATH);
	}

	public synchronized ComponentProfileImpl getProfile() {
		if (profile == null)
			locateBaseProfile();
		return profile;
	}

	public void setAppConfig(ApplicationConfiguration appConfig) {
		this.appConfig = appConfig;
	}
}
