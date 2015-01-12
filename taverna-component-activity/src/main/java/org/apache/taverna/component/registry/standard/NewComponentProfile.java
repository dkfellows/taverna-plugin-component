package org.apache.taverna.component.registry.standard;

import static org.apache.taverna.component.utils.SystemUtils.getElementString;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.taverna.component.api.ComponentException;
import org.apache.taverna.component.profile.BaseProfileLocator;
import org.apache.taverna.component.profile.ComponentProfileImpl;
import org.apache.taverna.component.registry.api.ComponentProfileType;
import org.apache.taverna.component.registry.api.Description;

/**
 * Profiles managed by the new-interface component registry.
 * 
 * @author Donal Fellows
 */
class NewComponentProfile extends ComponentProfileImpl {
	private static final String LOCATION = "content-uri";
	static final String ELEMENTS = LOCATION;

	private final NewComponentRegistry registry;
	private String id;
	private String location;
	private String resource;
	private final String uri;

	private static URL contentUrl(ComponentProfileType cpt)
			throws ComponentException {
		try {
			return new URL(cpt.getContentUri());
		} catch (MalformedURLException e) {
			throw new ComponentException("bad profile location", e);
		}
	}

	private static URL getLocationURL(Description cpd) throws ComponentException {
		try {
			return new URL(getElementString(cpd, LOCATION));
		} catch (MalformedURLException e) {
			throw new ComponentException("bad profile location", e);
		}
	}

	NewComponentProfile(NewComponentRegistry registry,
			ComponentProfileType profile, BaseProfileLocator base)
			throws ComponentException {
		super(registry, contentUrl(profile), base);
		this.registry = registry;
		uri = profile.getUri();
		id = profile.getId();
		location = profile.getContentUri();
		resource = profile.getResource();
	}

	NewComponentProfile(NewComponentRegistry registry, Description cpd,
			BaseProfileLocator base) throws ComponentException {
		super(registry, getLocationURL(cpd), base);
		this.registry = registry;
		uri = cpd.getUri();
		id = cpd.getId();
		location = getElementString(cpd, LOCATION);
		resource = cpd.getResource();
	}

	public String getLocation() {
		return location;
	}

	public String getID() {
		return id;
	}

	public String getUri() {
		return uri;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof NewComponentProfile) {
			NewComponentProfile other = (NewComponentProfile) o;
			return registry.equals(other.registry) && id.equals(other.id);
		}
		return false;
	}

	private static final int BASEHASH = NewComponentProfile.class.hashCode();

	@Override
	public int hashCode() {
		return BASEHASH ^ registry.hashCode() ^ id.hashCode();
	}

	@Override
	public String toString() {
		return "Remote Component Profile[" + location + "]";
	}

	public String getResourceLocation() {
		return resource;
	}
}
