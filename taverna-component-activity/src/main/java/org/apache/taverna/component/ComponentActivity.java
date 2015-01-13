package org.apache.taverna.component;

import static net.sf.taverna.t2.workflowmodel.utils.AnnotationTools.getAnnotationString;
import static net.sf.taverna.t2.workflowmodel.utils.AnnotationTools.setAnnotationString;
import static org.apache.log4j.Logger.getLogger;

import java.net.MalformedURLException;
import java.util.Map;

import net.sf.taverna.t2.activities.dataflow.DataflowActivity;
import net.sf.taverna.t2.annotation.annotationbeans.SemanticAnnotation;
import net.sf.taverna.t2.invocation.InvocationContext;
import net.sf.taverna.t2.invocation.impl.InvocationContextImpl;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.Dataflow;
import net.sf.taverna.t2.workflowmodel.EditException;
import net.sf.taverna.t2.workflowmodel.Edits;
import net.sf.taverna.t2.workflowmodel.processor.activity.AbstractAsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;

import org.apache.log4j.Logger;
import org.apache.taverna.component.api.ComponentException;
import org.apache.taverna.component.api.profile.ExceptionHandling;
import org.apache.taverna.component.registry.ComponentImplementationCache;
import org.apache.taverna.component.registry.ComponentUtil;
import org.apache.taverna.component.utils.AnnotationUtils;
import org.apache.taverna.component.utils.SystemUtils;

import uk.org.taverna.platform.execution.api.InvalidWorkflowException;

import com.fasterxml.jackson.databind.JsonNode;

public class ComponentActivity extends
		AbstractAsynchronousActivity<JsonNode> {
	public static final String URI = "http://ns.taverna.org.uk/2010/activity/component";
	private Logger logger = getLogger(ComponentActivity.class);

	private ComponentUtil util;
	private ComponentImplementationCache cache;
	private volatile DataflowActivity componentRealization;
	private JsonNode json;
	private ComponentActivityConfigurationBean bean;
	private SystemUtils system;
	@SuppressWarnings("unused")
	private AnnotationUtils annUtils;
	private ComponentExceptionFactory cef;
	
	private Dataflow realizingDataflow = null;

	ComponentActivity(ComponentUtil util, ComponentImplementationCache cache,
			Edits edits, SystemUtils system, AnnotationUtils annUtils,
			ComponentExceptionFactory exnFactory) {
		this.util = util;
		this.cache = cache;
		this.system = system;
		this.annUtils = annUtils;
		setEdits(edits);
		this.componentRealization = new DataflowActivity();
		this.cef = exnFactory;
	}

	@Override
	public void configure(JsonNode json) throws ActivityConfigurationException {
		this.json = json;
		try {
			bean = new ComponentActivityConfigurationBean(json, util, cache);
		} catch (MalformedURLException e) {
			throw new ActivityConfigurationException(
					"failed to understand configuration", e);
		}
		try {
			configurePorts(bean.getPorts());
		} catch (ComponentException e) {
			throw new ActivityConfigurationException(
					"failed to get component realization", e);
		}
	}

	@Override
	public void executeAsynch(Map<String, T2Reference> inputs,
			AsynchronousActivityCallback callback) {
		try {
			ExceptionHandling exceptionHandling = bean.getExceptionHandling();
			// InvocationContextImpl newContext = copyInvocationContext(callback);

			getComponentRealization().executeAsynch(inputs, new ProxyCallback(
					callback, callback.getContext(), exceptionHandling, cef));
		} catch (ActivityConfigurationException e) {
			callback.fail("Unable to execute component", e);
		}
	}

	@SuppressWarnings("unused")
	private InvocationContextImpl copyInvocationContext(
			AsynchronousActivityCallback callback) {
		InvocationContext originalContext = callback.getContext();
		ReferenceService rs = originalContext.getReferenceService();
		InvocationContextImpl newContext = new InvocationContextImpl(rs, null);
		// for (Object o : originalContext.getEntities(Object.class))
		// newContext.addEntity(o);
		return newContext;
	}

	@Override
	public JsonNode getConfiguration() {
		return json;
	}

	ComponentActivityConfigurationBean getConfigBean() {
		return bean;
	}

	private DataflowActivity getComponentRealization()
			throws ActivityConfigurationException {
		synchronized (componentRealization) {
			try {
				if (componentRealization.getNestedDataflow() == null) {
					if (realizingDataflow == null)
						realizingDataflow = system.compile(util
								.getVersion(bean).getImplementation());
					componentRealization.setNestedDataflow(realizingDataflow);
					copyAnnotations();
				}
			} catch (ComponentException e) {
				logger.error("unable to read workflow", e);
				throw new ActivityConfigurationException(
						"unable to read workflow", e);
			} catch (InvalidWorkflowException e) {
				logger.error("unable to compile workflow", e);
				throw new ActivityConfigurationException(
						"unable to compile workflow", e);
			}
		}
		
		return componentRealization;
	}

	private void copyAnnotations() {
		// TODO Completely wrong way of doing this!
		try {
			//annUtils.getAnnotation(subject, uriForAnnotation)
			String annotationValue = getAnnotationString(realizingDataflow,
					SemanticAnnotation.class, null);
			if (annotationValue != null)
				setAnnotationString(this, SemanticAnnotation.class,
						annotationValue, getEdits()).doEdit();
		} catch (EditException e) {
			logger.error("failed to set annotation string", e);
		}
	}
}
