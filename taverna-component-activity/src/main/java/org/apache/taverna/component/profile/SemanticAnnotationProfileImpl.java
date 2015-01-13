/*******************************************************************************
 * Copyright (C) 2012 The University of Manchester
 *
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
package org.apache.taverna.component.profile;

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.taverna.component.api.profile.SemanticAnnotationProfile;
import org.apache.taverna.component.api.profile.doc.SemanticAnnotation;
import org.slf4j.Logger;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;

/**
 * Definition of a semantic annotation for a component element.
 * 
 * @author David Withers
 */
public class SemanticAnnotationProfileImpl implements SemanticAnnotationProfile {
	private static final Logger log = getLogger(SemanticAnnotationProfileImpl.class);
	private final ComponentProfileImpl componentProfile;
	private final SemanticAnnotation semanticAnnotation;

	public SemanticAnnotationProfileImpl(ComponentProfileImpl componentProfile,
			SemanticAnnotation semanticAnnotation) {
		this.componentProfile = componentProfile;
		this.semanticAnnotation = semanticAnnotation;
	}

	/**
	 * Returns the ontology that defines semantic annotation.
	 * 
	 * @return the ontology that defines semantic annotation
	 */
	@Override
	public OntModel getOntology() {
		String ontology = semanticAnnotation.getOntology();
		if (ontology == null)
			return null;
		return componentProfile.getOntology(ontology);
	}

	/**
	 * Returns the predicate for the semantic annotation.
	 * 
	 * @return the predicate for the semantic annotation
	 */
	@Override
	public OntProperty getPredicate() {
		OntModel ontology = getOntology();
		if (ontology == null)
			return null;
		String predicate = semanticAnnotation.getPredicate();
		if (predicate == null)
			return null;
		if (predicate.contains("foaf")) {
			StringWriter sw = new StringWriter();
			ontology.writeAll(sw, null, "RDF/XML");
			try {
				writeStringToFile(createTempFile("foaf", null), sw.toString());
			} catch (IOException e) {
				log.info("failed to write foaf ontology to temporary file", e);
			}
		}

		return ontology.getOntProperty(predicate);
	}

	@Override
	public String getPredicateString() {
		return semanticAnnotation.getPredicate();
	}

	@Override
	public String getClassString() {
		return semanticAnnotation.getClazz();
	}

	/**
	 * Returns the individual that the semantic annotation must use.
	 * 
	 * May be null if no explicit individual is required.
	 * 
	 * @return the individual that the semantic annotation must use
	 */
	@Override
	public Individual getIndividual() {
		String individual = semanticAnnotation.getValue();
		if (individual == null || individual.isEmpty())
			return null;
		return getOntology().getIndividual(individual);
	}

	/**
	 * Returns the individuals in the range of the predicate defined in the
	 * ontology.
	 * 
	 * @return the individuals in the range of the predicate defined in the
	 *         ontology
	 */
	@Override
	public List<Individual> getIndividuals() {
		OntModel ontology = getOntology();
		OntProperty prop = getPredicate();
		if (ontology == null || prop == null)
			return new ArrayList<>();
		OntResource range = prop.getRange();
		if (range == null)
			return new ArrayList<>();
		return ontology.listIndividuals(range).toList();
	}

	@Override
	public Integer getMinOccurs() {
		return semanticAnnotation.getMinOccurs().intValue();
	}

	@Override
	public Integer getMaxOccurs() {
		try {
			return Integer.valueOf(semanticAnnotation.getMaxOccurs());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "SemanticAnnotation " + "\n Predicate : " + getPredicate()
				+ "\n Individual : " + getIndividual() + "\n Individuals : "
				+ getIndividuals();
	}

	@Override
	public OntClass getRangeClass() {
		String clazz = this.getClassString();
		if (clazz != null)
			return componentProfile.getClass(clazz);

		OntProperty prop = getPredicate();
		if (prop == null)
			return null;
		OntResource range = prop.getRange();
		if (range != null && range.isClass())
			return range.asClass();
		return null;
	}
}
