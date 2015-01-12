package org.apache.taverna.component.registry;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.taverna.component.api.Component;
import org.apache.taverna.component.api.ComponentException;
import org.apache.taverna.component.api.ComponentFactory;
import org.apache.taverna.component.api.Family;
import org.apache.taverna.component.api.Registry;
import org.apache.taverna.component.api.Version;
import org.apache.taverna.component.api.profile.Profile;
import org.apache.taverna.component.profile.BaseProfileLocator;
import org.apache.taverna.component.profile.ComponentProfileImpl;
import org.apache.taverna.component.registry.local.LocalComponentRegistryFactory;
import org.apache.taverna.component.registry.standard.NewComponentRegistryFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author alanrw
 * @author dkf
 */
public class ComponentUtil implements ComponentFactory {
	private NewComponentRegistryFactory netLocator;
	private BaseProfileLocator base;
	private LocalComponentRegistryFactory fileLocator;

	private final Map<String, Registry> cache = new HashMap<>();

	@Required
	public void setNetworkLocator(NewComponentRegistryFactory locator) {
		this.netLocator = locator;
	}

	@Required
	public void setFileLocator(LocalComponentRegistryFactory fileLocator) {
		this.fileLocator = fileLocator;
	}

	@Required
	public void setBaseLocator(BaseProfileLocator base) {
		this.base = base;
	}

	@Override
	public Registry getRegistry(URL registryBase) throws ComponentException {
		Registry registry = cache.get(registryBase.toString());
		if (registry != null)
			return registry;

		if (registryBase.getProtocol().startsWith("http")) {
			if (!netLocator.verifyBase(registryBase))
				throw new ComponentException(
						"Unable to establish credentials for " + registryBase);
			registry = netLocator.getComponentRegistry(registryBase);
		} else
			registry = fileLocator.getComponentRegistry(registryBase);
		cache.put(registryBase.toString(), registry);
		return registry;
	}

	@Override
	public Family getFamily(URL registryBase, String familyName)
			throws ComponentException {
		return getRegistry(registryBase).getComponentFamily(familyName);
	}

	@Override
	public Component getComponent(URL registryBase, String familyName,
			String componentName) throws ComponentException {
		return getRegistry(registryBase).getComponentFamily(familyName)
				.getComponent(componentName);
	}

	@Override
	public Version getVersion(URL registryBase, String familyName,
			String componentName, Integer componentVersion)
			throws ComponentException {
		return getRegistry(registryBase).getComponentFamily(familyName)
				.getComponent(componentName)
				.getComponentVersion(componentVersion);
	}

	@Override
	public Version getVersion(Version.ID ident) throws ComponentException {
		return getVersion(ident.getRegistryBase(), ident.getFamilyName(),
				ident.getComponentName(), ident.getComponentVersion());
	}

	@Override
	public Component getComponent(Version.ID ident) throws ComponentException {
		return getComponent(ident.getRegistryBase(), ident.getFamilyName(),
				ident.getComponentName());
	}

	@Override
	public Profile getProfile(URL url) throws ComponentException {
		Profile p = new ComponentProfileImpl(url, base);
		p.getProfileDocument(); // force immediate loading
		return p;
	}

	@Override
	public Profile getBaseProfile() throws ComponentException {
		return base.getProfile();
	}

	public BaseProfileLocator getBaseProfileLocator() {
		return base;
	}
}
