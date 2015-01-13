/**
 * 
 */
package org.apache.taverna.component.profile;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Locale.UK;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.taverna.component.api.ComponentException;
import org.slf4j.Logger;

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

		try {
			remoteBaseProfileTime = getRemoteBaseProfileTimestamp();
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

	private long getRemoteBaseProfileTimestamp() throws URISyntaxException,
			IOException, ParseException {
		URL baseProfileUrl = new URL(BASE_PROFILE_URI);
		HttpURLConnection c = (HttpURLConnection) baseProfileUrl
				.openConnection();
		c.setRequestMethod("HEAD");
		c.setConnectTimeout(TIMEOUT);
		c.setReadTimeout(TIMEOUT);
		c.connect();
		try {
			int statusCode = c.getResponseCode();
			if (statusCode != HTTP_OK) {
				logger.warn("HTTP status " + statusCode + " while getting "
						+ baseProfileUrl);
				return -1;
			}
			String lastModified = c.getHeaderField("Last-Modified");
			if (lastModified == null)
				return -1;
			return parseTime(lastModified);
		} finally {
			c.disconnect();
		}
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
