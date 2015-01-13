/**
 *
 */
package org.apache.taverna.component.registry.local;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.taverna.component.registry.local.LocalComponentRegistry.ENC;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;

import org.apache.taverna.component.api.Component;
import org.apache.taverna.component.api.ComponentException;
import org.apache.taverna.component.api.Version;
import org.apache.taverna.component.api.profile.Profile;
import org.apache.taverna.component.registry.ComponentFamily;
import org.apache.taverna.component.registry.ComponentUtil;
import org.apache.taverna.component.utils.SystemUtils;
import org.slf4j.Logger;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;

/**
 * @author alanrw
 * 
 */
class LocalComponentFamily extends ComponentFamily {
	private static Logger logger = getLogger(LocalComponentFamily.class);
	private static final String PROFILE = "profile";

	private final File componentFamilyDir;
	private SystemUtils system;

	public LocalComponentFamily(LocalComponentRegistry parentRegistry,
			File componentFamilyDir, ComponentUtil util, SystemUtils system) {
		super(parentRegistry, util);
		this.componentFamilyDir = componentFamilyDir;
		this.system = system;
	}

	@Override
	protected final Profile internalGetComponentProfile()
			throws ComponentException {
		LocalComponentRegistry parentRegistry = (LocalComponentRegistry) getComponentRegistry();
		File profileFile = new File(componentFamilyDir, PROFILE);
		String profileName;
		try {
			profileName = readFileToString(profileFile, ENC);
		} catch (IOException e) {
			throw new ComponentException("Unable to read profile name", e);
		}
		for (Profile p : parentRegistry.getComponentProfiles())
			if (p.getName().equals(profileName))
				return p;
		return null;
	}

	@Override
	protected void populateComponentCache() throws ComponentException {
		for (File subFile : componentFamilyDir.listFiles()) {
			if (!subFile.isDirectory())
				continue;
			LocalComponent newComponent = new LocalComponent(subFile,
					(LocalComponentRegistry) getComponentRegistry(), this,
					system);
			componentCache.put(newComponent.getName(), newComponent);
		}
	}

	@Override
	protected final String internalGetName() {
		return componentFamilyDir.getName();
	}

	@Override
	protected final Version internalCreateComponentBasedOn(
			String componentName, String description, WorkflowBundle bundle)
			throws ComponentException {
		File newSubFile = new File(componentFamilyDir, componentName);
		if (newSubFile.exists())
			throw new ComponentException("Component already exists");
		newSubFile.mkdirs();
		File descriptionFile = new File(newSubFile, "description");
		try {
			writeStringToFile(descriptionFile, description, ENC);
		} catch (IOException e) {
			throw new ComponentException("Could not write out description", e);
		}
		LocalComponent newComponent = new LocalComponent(newSubFile,
				(LocalComponentRegistry) getComponentRegistry(), this, system);

		return newComponent.addVersionBasedOn(bundle, "Initial version");
	}

	@Override
	public int hashCode() {
		return 31 + ((componentFamilyDir == null) ? 0 : componentFamilyDir
				.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalComponentFamily other = (LocalComponentFamily) obj;
		if (componentFamilyDir == null)
			return (other.componentFamilyDir == null);
		return componentFamilyDir.equals(other.componentFamilyDir);
	}

	@Override
	protected final String internalGetDescription() {
		File descriptionFile = new File(componentFamilyDir, "description");
		try {
			if (descriptionFile.isFile())
				return readFileToString(descriptionFile);
		} catch (IOException e) {
			logger.error("failed to get description from " + descriptionFile, e);
		}
		return "";
	}

	@Override
	protected final void internalRemoveComponent(Component component)
			throws ComponentException {
		File componentDir = new File(componentFamilyDir, component.getName());
		try {
			deleteDirectory(componentDir);
		} catch (IOException e) {
			throw new ComponentException("Unable to delete component", e);
		}
	}
}
