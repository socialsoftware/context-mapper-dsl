/*
 * Copyright 2018 The Context Mapper Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.contextmapper.dsl.validation;

import static org.contextmapper.dsl.contextMappingDSL.BoundedContextType.TEAM;
import static org.contextmapper.dsl.contextMappingDSL.ContextMapType.ORGANIZATIONAL;
import static org.contextmapper.dsl.contextMappingDSL.ContextMapType.SYSTEM_LANDSCAPE;
import static org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage.Literals.CONTEXT_MAP__BOUNDED_CONTEXTS;
import static org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage.Literals.CONTEXT_MAP__TYPE;
import static org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage.Literals.SYMMETRIC_RELATIONSHIP__PARTICIPANT1;
import static org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage.Literals.SYMMETRIC_RELATIONSHIP__PARTICIPANT2;
import static org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage.Literals.UPSTREAM_DOWNSTREAM_RELATIONSHIP__DOWNSTREAM;
import static org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage.Literals.UPSTREAM_DOWNSTREAM_RELATIONSHIP__UPSTREAM;
import static org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage.Literals.UPSTREAM_DOWNSTREAM_RELATIONSHIP__UPSTREAM_EXPOSED_AGGREGATES;
import static org.contextmapper.dsl.validation.ValidationMessages.EXPOSED_AGGREGATE_NOT_PART_OF_UPSTREAM_CONTEXT;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_BOUNDED_CONTEXT_DOES_NOT_EXIST;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_BOUNDED_CONTEXT_IS_NOT_UPSTREAM;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_AGGREGATE_DOES_NOT_BELONG_TO_MAPPED_BOUNDED_CONTEXT;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_ENTITY_DOES_NOT_BELONG_TO_MAPPED_AGGREGATE;
import static org.contextmapper.dsl.validation.ValidationMessages.ORGANIZATIONAL_MAP_DOES_NOT_CONTAIN_TEAM;
import static org.contextmapper.dsl.validation.ValidationMessages.RELATIONSHIP_CONTEXT_NOT_ON_MAP_ERROR_MESSAGE;
import static org.contextmapper.dsl.validation.ValidationMessages.SYSTEM_LANDSCAPE_MAP_CONTAINS_TEAM;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ENTITY__MAPPING;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.MalformedObjectNameException;

import org.contextmapper.dsl.contextMappingDSL.Aggregate;
import org.contextmapper.dsl.contextMappingDSL.BoundedContext;
import org.contextmapper.dsl.contextMappingDSL.BoundedContextType;
import org.contextmapper.dsl.contextMappingDSL.ContextMap;
import org.contextmapper.dsl.contextMappingDSL.SculptorModule;
import org.contextmapper.dsl.contextMappingDSL.Relationship;
import org.contextmapper.dsl.contextMappingDSL.SymmetricRelationship;
import org.contextmapper.dsl.contextMappingDSL.UpstreamDownstreamRelationship;
import org.contextmapper.tactic.dsl.tacticdsl.Entity;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;
import org.jgrapht.alg.util.AliasMethodSampler;

public class ContextMapSemanticsValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void validateThatRelationshipContextArePartOfMap(final ContextMap map) {
		for (Relationship relationship : map.getRelationships()) {
			BoundedContext context1 = null;
			BoundedContext context2 = null;
			EReference attributeRelContext1 = null;
			EReference attributeRelContext2 = null;
			if (relationship instanceof SymmetricRelationship) {
				context1 = ((SymmetricRelationship) relationship).getParticipant1();
				context2 = ((SymmetricRelationship) relationship).getParticipant2();
				attributeRelContext1 = SYMMETRIC_RELATIONSHIP__PARTICIPANT1;
				attributeRelContext2 = SYMMETRIC_RELATIONSHIP__PARTICIPANT2;
			} else if (relationship instanceof UpstreamDownstreamRelationship) {
				context1 = ((UpstreamDownstreamRelationship) relationship).getUpstream();
				context2 = ((UpstreamDownstreamRelationship) relationship).getDownstream();
				attributeRelContext1 = UPSTREAM_DOWNSTREAM_RELATIONSHIP__UPSTREAM;
				attributeRelContext2 = UPSTREAM_DOWNSTREAM_RELATIONSHIP__DOWNSTREAM;
			}

			if (context1 != null && !isContextPartOfMap(map, context1))
				error(String.format(RELATIONSHIP_CONTEXT_NOT_ON_MAP_ERROR_MESSAGE, context1.getName()), relationship, attributeRelContext1);
			if (context2 != null && !isContextPartOfMap(map, context2))
				error(String.format(RELATIONSHIP_CONTEXT_NOT_ON_MAP_ERROR_MESSAGE, context2.getName()), relationship, attributeRelContext2);
		}
	}

	@Check
	public void validateThatExposedAggregateIsPartOfUpstreamContext(final ContextMap map) {
		for (Relationship rel : map.getRelationships()) {
			if (rel instanceof UpstreamDownstreamRelationship) {
				UpstreamDownstreamRelationship relationship = (UpstreamDownstreamRelationship) rel;
				BoundedContext upstreamContext = ((UpstreamDownstreamRelationship) relationship).getUpstream();
				int aggregateRefIndex = 0;
				for (Aggregate aggregate : relationship.getUpstreamExposedAggregates()) {
					List<String> aggregates = upstreamContext.getAggregates().stream().map(a -> a.getName()).collect(Collectors.toList());
					for (SculptorModule module : upstreamContext.getModules()) {
						aggregates.addAll(module.getAggregates().stream().map(b -> b.getName()).collect(Collectors.toList()));
					}
					if (!aggregates.contains(aggregate.getName()))
						error(String.format(EXPOSED_AGGREGATE_NOT_PART_OF_UPSTREAM_CONTEXT, aggregate.getName(), upstreamContext.getName()), relationship,
								UPSTREAM_DOWNSTREAM_RELATIONSHIP__UPSTREAM_EXPOSED_AGGREGATES, aggregateRefIndex);
					aggregateRefIndex++;
				}
			}
		}
	}

	@Check
	public void validateBoundedContextTypes(final ContextMap map) {
		if (ORGANIZATIONAL.equals(map.getType())) {
			if (!map.getBoundedContexts().stream().anyMatch(bc -> bc.getType() == BoundedContextType.TEAM))
				warning(ORGANIZATIONAL_MAP_DOES_NOT_CONTAIN_TEAM, map, CONTEXT_MAP__TYPE);
		} else if (SYSTEM_LANDSCAPE.equals(map.getType())) {
			for (BoundedContext bc : map.getBoundedContexts()) {
				if (TEAM.equals(bc.getType()))
					error(String.format(SYSTEM_LANDSCAPE_MAP_CONTAINS_TEAM), map, CONTEXT_MAP__BOUNDED_CONTEXTS);
			}
		}
	}
	
	@Check
	public void validateMappedEntities(final ContextMap map) {
		Set<String> boundedContextNames = map.getBoundedContexts().stream()
			.map(BoundedContext::getName)
			.collect(Collectors.toSet());
		
		Set<UpstreamDownstreamRelationship> upstreamDownstreamRelationships = map.getRelationships().stream()
				.filter(UpstreamDownstreamRelationship.class::isInstance)
				.map(UpstreamDownstreamRelationship.class::cast)
				.collect(Collectors.toSet());
		
		for (BoundedContext boundedContext: map.getBoundedContexts()) {
			for (Aggregate aggregate: boundedContext.getAggregates()) {
				aggregate.getDomainObjects().stream()
					.filter(Entity.class::isInstance)
					.map(Entity.class::cast)
					.filter(entity -> entity.getMapping() != null)
					.forEach(entity -> {
						checkMappedBoundedContextExists(boundedContextNames, entity);
						checkMappedBoundedContextIsUpstream(upstreamDownstreamRelationships, boundedContext, entity);
						checkMappedAggregateBelongsToBoundedContext(map.getBoundedContexts(), entity);
						checkMappedEntityBelongsToAggregate(map.getBoundedContexts(), entity);
					}); 
			}
		}
	}

	private void checkMappedBoundedContextExists(Set<String> boundedContextNames, Entity entity) {
		if (!boundedContextNames.contains(entity.getMapping().getBoundedContext())) {
			error(String.format(MAPPED_BOUNDED_CONTEXT_DOES_NOT_EXIST, entity.getMapping().getBoundedContext()), 
					entity, ENTITY__MAPPING);
		}
	}

	private void checkMappedBoundedContextIsUpstream(
			Set<UpstreamDownstreamRelationship> upstreamDownstreamRelationships, BoundedContext boundedContext,
			Entity entity) {
		String mappingBoundedContextName = boundedContext.getName();
		String mappedBoundedContextName = entity.getMapping().getBoundedContext();
		
		 if (!upstreamDownstreamRelationships.stream()
			.anyMatch(relationship -> relationship.getDownstream().getName().equals(mappingBoundedContextName)
									&& relationship.getUpstream().getName().equals(mappedBoundedContextName))) {
			 error(String.format(MAPPED_BOUNDED_CONTEXT_IS_NOT_UPSTREAM, entity.getMapping().getBoundedContext()), 
						entity, ENTITY__MAPPING);
		 }
	}
	
	private void checkMappedAggregateBelongsToBoundedContext(EList<BoundedContext> boundedContexts, Entity entity) {
		if (boundedContexts.stream()
				.filter(boundedContext -> boundedContext.getName().equals(entity.getMapping().getBoundedContext()))
				.flatMap(boundedContext -> boundedContext.getAggregates().stream())
				.noneMatch(aggregate -> aggregate.getName().equals(entity.getMapping().getAggregate()))) {
			error(String.format(MAPPED_AGGREGATE_DOES_NOT_BELONG_TO_MAPPED_BOUNDED_CONTEXT, entity.getMapping().getAggregate()), 
						entity, ENTITY__MAPPING);
		}
	}

	private void checkMappedEntityBelongsToAggregate(EList<BoundedContext> boundedContexts, Entity entity) {
		if (boundedContexts.stream()
				.filter(boundedContext -> boundedContext.getName().equals(entity.getMapping().getBoundedContext()))
				.flatMap(boundedContext -> boundedContext.getAggregates().stream())
				.filter(aggregate -> aggregate.getName().equals(entity.getMapping().getAggregate()))
				.flatMap(aggregate -> aggregate.getDomainObjects().stream())
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.noneMatch(entity1 -> entity1.equals(entity.getMapping().getEntity()))) {
			error(String.format(MAPPED_ENTITY_DOES_NOT_BELONG_TO_MAPPED_AGGREGATE, entity.getMapping().getEntity().getName()), 
						entity, ENTITY__MAPPING);
		}
	}

	private boolean isContextPartOfMap(ContextMap map, BoundedContext context) {
		return map.getBoundedContexts().contains(context);
	}
}
