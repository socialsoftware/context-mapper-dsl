/*
 * Copyright 2019 The Context Mapper Project Team
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

import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_MAPPING_NOT_CONFORM_TYPES;
import static org.contextmapper.dsl.validation.ValidationMessages.DUPLICATED_MAPPING_ATTRIBUTES;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_BOUNDED_CONTEXT_IS_NOT_UPSTREAM;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_AGGREGATE_DOES_NOT_BELONG_TO_MAPPED_BOUNDED_CONTEXT;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_ENTITY_DOES_NOT_BELONG_TO_MAPPED_AGGREGATE;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_ATTRIBUTE_DOES_NOT_BELONG_TO_MAPPED_ENTITY;
import static org.contextmapper.dsl.validation.ValidationMessages.AGGREGATE_ROOT_ENTITY_AND_ITS_KEY_ATTRIBUTE_SHOULD_BE_MAPPED;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.contextmapper.dsl.cml.CMLModelObjectsResolvingHelper;
import org.contextmapper.dsl.contextMappingDSL.AntiCorruptionMapping;
import org.contextmapper.dsl.contextMappingDSL.BoundedContext;
import org.contextmapper.dsl.contextMappingDSL.ContextMap;
import org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage;
import org.contextmapper.dsl.contextMappingDSL.UpstreamDownstreamRelationship;
import org.contextmapper.tactic.dsl.tacticdsl.Attribute;
import org.contextmapper.tactic.dsl.tacticdsl.Entity;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;



public class AntiCorruptionMappingSemanticsValidator extends AbstractCMLValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}
	
	@Check
	public void validateMappedBoundedContextIsUpstream(final AntiCorruptionMapping antiCorruptionMapping) {
		BoundedContext mappedBoundedContext = antiCorruptionMapping.getBoundedContext();
		BoundedContext mappingBoundedContext = (BoundedContext) antiCorruptionMapping.eContainer();
		ContextMap contextMap = new CMLModelObjectsResolvingHelper(getRootCMLModel(antiCorruptionMapping))
				.getContextMap(mappedBoundedContext);
		
		if (!contextMap.getRelationships().stream()
			.filter(UpstreamDownstreamRelationship.class::isInstance)
			.map(UpstreamDownstreamRelationship.class::cast)
			.anyMatch(relationship -> relationship.getDownstream().equals(mappingBoundedContext)
								&& relationship.getUpstream().equals(mappedBoundedContext))) {
			error(String.format(MAPPED_BOUNDED_CONTEXT_IS_NOT_UPSTREAM, mappedBoundedContext.getName()), 
					antiCorruptionMapping, ContextMappingDSLPackage.Literals.ANTI_CORRUPTION_MAPPING__BOUNDED_CONTEXT);
		}
	}
	
	@Check
	public void validateMappedAggregateBelongsToBoundedContext(final AntiCorruptionMapping antiCorruptionMapping) {
		if (antiCorruptionMapping.getBoundedContext().getAggregates().stream()
				.noneMatch(aggregate -> aggregate.equals(antiCorruptionMapping.getAggregate()))) {
			error(String.format(MAPPED_AGGREGATE_DOES_NOT_BELONG_TO_MAPPED_BOUNDED_CONTEXT, antiCorruptionMapping.getAggregate().getName()), 
					antiCorruptionMapping, ContextMappingDSLPackage.Literals.ANTI_CORRUPTION_MAPPING__AGGREGATE);
		}
	}
	
	@Check
	public void validateMappedEntitiesBelongToAggregate(final AntiCorruptionMapping antiCorruptionMapping) {
		 Set<Entity> aggregateEntities = antiCorruptionMapping.getAggregate().getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.collect(Collectors.toSet());
		 
		 antiCorruptionMapping.getAttributeMappings()
		 		.forEach(attributeMapping -> {
		 			Entity entity = attributeMapping.getEntityAttribute().getEntity();
		 			if (!aggregateEntities.contains(entity)) {
		 				error(String.format(MAPPED_ENTITY_DOES_NOT_BELONG_TO_MAPPED_AGGREGATE, entity.getName()), 
		 						attributeMapping.getEntityAttribute(), ContextMappingDSLPackage.Literals.ENTITY_ATTRIBUTE__ENTITY);
		 			}
		 		});		
	}
	
	@Check
	public void validateMappedAttributesBelongToEntity(final AntiCorruptionMapping antiCorruptionMapping) {
		 antiCorruptionMapping.getAttributeMappings()
		 		.forEach(attributeMapping -> {
		 			Entity entity = attributeMapping.getEntityAttribute().getEntity();
		 			Attribute attribute = attributeMapping.getEntityAttribute().getAttribute();
		 			if (entity.getAttributes().stream()
		 					.map(Attribute::getName)
		 					.noneMatch(attribute1 -> attribute1.equals(attribute.getName()))) {
		 				error(String.format(MAPPED_ATTRIBUTE_DOES_NOT_BELONG_TO_MAPPED_ENTITY, attribute.getName()), 
		 						attributeMapping.getEntityAttribute(), ContextMappingDSLPackage.Literals.ENTITY_ATTRIBUTE__ATTRIBUTE);
		 			}
		 		});		
	}
	
	@Check
	public void validateAttributeMappingTypes(final AntiCorruptionMapping antiCorruptionMapping) {
		 antiCorruptionMapping.getAttributeMappings()
	 		.forEach(attributeMappping -> {
	 			String mappingAttributeType = attributeMappping.getType();
	 			String mappedAttributeType = 
	 				getEntityAttributeByName(attributeMappping.getEntityAttribute().getEntity(), attributeMappping.getEntityAttribute().getAttribute().getName()).getType(); 
	 			if (!mappingAttributeType.equals(mappedAttributeType)) {
	 				error(String.format(ATTRIBUTE_MAPPING_NOT_CONFORM_TYPES, mappedAttributeType), 
	 						attributeMappping, ContextMappingDSLPackage.Literals.ATTRIBUTE_MAPPING__TYPE);
	 			}
	 		});
	}
	
	@Check
	public void validateAggregateRootKeyAttributeIsMapped(final AntiCorruptionMapping antiCorruptionMapping) {
		if (antiCorruptionMapping.getAttributeMappings().stream()
				 	.noneMatch(attributeMapping -> 
	 					attributeMapping.getEntityAttribute().getEntity().isAggregateRoot() &&
	 					getEntityAttributeByName(attributeMapping.getEntityAttribute().getEntity(),
	 							attributeMapping.getEntityAttribute().getAttribute().getName()).isKey())) {
	 		error(String.format(AGGREGATE_ROOT_ENTITY_AND_ITS_KEY_ATTRIBUTE_SHOULD_BE_MAPPED, antiCorruptionMapping.getAggregate().getName()), 
	 						antiCorruptionMapping, ContextMappingDSLPackage.Literals.ANTI_CORRUPTION_MAPPING__AGGREGATE);
	 	}
	}
	
	@Check
	public void validateUniqueAttributeMappings(final AntiCorruptionMapping antiCorruptionMapping) {
		Set<String> names = new HashSet<>();
		antiCorruptionMapping.getAttributeMappings()
			.forEach(attributeMapping -> {
				String name = attributeMapping.getName();
				if (names.contains(name)) {
					error(String.format(DUPLICATED_MAPPING_ATTRIBUTES, name), 
							attributeMapping, ContextMappingDSLPackage.Literals.ATTRIBUTE_MAPPING__NAME);
				}
				names.add(name);
			});
	}
	
	private Attribute getEntityAttributeByName(Entity entity, String name) {
		return entity.getAttributes().stream()
			.filter(attribute -> attribute.getName().equals(name))
			.findAny()
			.orElse(null);
	}
	
}