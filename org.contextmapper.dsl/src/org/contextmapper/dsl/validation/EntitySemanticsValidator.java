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

import static org.contextmapper.dsl.validation.ValidationMessages.AGGREGATE_ROOT_CANNOT_USE_MAPPING;
import static org.contextmapper.dsl.validation.ValidationMessages.ENTITY_MAPPING_DOES_NOT_EXIST_FOR_USE;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_USES_BUT_ENTITY_DOES_NOT;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_USES_VARIABLE_NOT_DEFINED;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_USES_ATTRIBUTE_NOT_DEFINED;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_TYPE_DOES_NOT_CONFORM_ATTRIBUTE_USED_TYPE;
import static org.contextmapper.dsl.validation.ValidationMessages.ENTITY_ATTRIBUTE_USES_DO_NOT_USE_KEY_ATTRIBUTE;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.DOMAIN_OBJECT__AGGREGATE_ROOT;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ENTITY__ANTI_CORRUPTION_MAPPING;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__USES;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__VARIABLE;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__USES_ATTRIBUTE;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__TYPE;

import org.contextmapper.dsl.contextMappingDSL.Aggregate;
import org.contextmapper.dsl.contextMappingDSL.AntiCorruptionMapping;
import org.contextmapper.dsl.contextMappingDSL.AttributeMapping;
import org.contextmapper.dsl.contextMappingDSL.BoundedContext;
import org.contextmapper.tactic.dsl.tacticdsl.Attribute;
import org.contextmapper.tactic.dsl.tacticdsl.Entity;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;


public class EntitySemanticsValidator extends AbstractCMLValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}
	
	@Check
	public void ValidateAggregateRootDoesNotUseMapping(final Entity entity) {
		if (entity.isAggregateRoot() && entity.isUses()) {
			error(String.format(AGGREGATE_ROOT_CANNOT_USE_MAPPING, entity.getName()), 
					entity, DOMAIN_OBJECT__AGGREGATE_ROOT);
		}
	}
	
	@Check
	public void ValidateWellFormedEntityUsesMapping(final Entity entity) {
		Aggregate aggregate = (Aggregate) entity.eContainer();
		BoundedContext boundedContext = (BoundedContext) aggregate.eContainer();
		
		if (entity.isUses() && boundedContext.getAntiCorruptionMappings().stream()
			.noneMatch(antiCorruptionMapping -> antiCorruptionMapping.getName().equals(entity.getAntiCorruptionMapping()))) {
			error(String.format(ENTITY_MAPPING_DOES_NOT_EXIST_FOR_USE, entity.getName()), 
					entity, ENTITY__ANTI_CORRUPTION_MAPPING);
		}
	}
	
	@Check
	public void ValidateMappingKeyAttributeIsUsed(final Entity entity) {
		Aggregate aggregate = (Aggregate) entity.eContainer();
		BoundedContext boundedContext = (BoundedContext) aggregate.eContainer();
		AntiCorruptionMapping antiCorruptionMapping = getAntiCorruptionMappingByName(boundedContext, entity.getAntiCorruptionMapping());
		String keyAttributeName = antiCorruptionMapping.getAggregate().getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.filter(Entity::isAggregateRoot)
				.flatMap(entity1 -> entity1.getAttributes().stream())
				.filter(Attribute::isKey)
				.map(Attribute::getName)
				.findAny()
				.orElse(null);
		AttributeMapping keyAttributeMapping = getAntiCorruptionMappingAttributeByName(antiCorruptionMapping, keyAttributeName);
				
		if (entity.isUses() &&
			entity.getAttributes().stream()
				.filter(Attribute::isUses)
				.noneMatch(attribute -> attribute.getName().equals(keyAttributeMapping.getName()))) {
			error(String.format(ENTITY_ATTRIBUTE_USES_DO_NOT_USE_KEY_ATTRIBUTE, keyAttributeMapping.getName()), 
					entity, ENTITY__ANTI_CORRUPTION_MAPPING);
		}
	}

	@Check
	public void ValidateWellFormedAttributesUsesMapping(final Entity entity) {	
		Aggregate aggregate = (Aggregate) entity.eContainer();
		BoundedContext boundedContext = (BoundedContext) aggregate.eContainer();
		AntiCorruptionMapping antiCorruptionMapping = getAntiCorruptionMappingByName(boundedContext, entity.getAntiCorruptionMapping());

		entity.getAttributes().stream()
			.filter(Attribute::isUses)
			.forEach(attribute -> {
				if (!entity.isUses() && attribute.isUses()) {
					error(String.format(ATTRIBUTE_USES_BUT_ENTITY_DOES_NOT, attribute.getName()), 
							attribute, ATTRIBUTE__USES);
				}
				if (entity.isUses() && attribute.isUses()) {
					if (!attribute.getVariable().equals(entity.getVariable())) {
						error(String.format(ATTRIBUTE_USES_VARIABLE_NOT_DEFINED, entity.getVariable()), 
								attribute, ATTRIBUTE__VARIABLE);
					}		
					AttributeMapping attributeMapping = getAntiCorruptionMappingAttributeByName(antiCorruptionMapping, 
							attribute.getUsesAttribute());
					if (attributeMapping == null) {
						error(String.format(ATTRIBUTE_USES_ATTRIBUTE_NOT_DEFINED, antiCorruptionMapping.getName()), 
								attribute, ATTRIBUTE__USES_ATTRIBUTE);
					} else {
						if (!attributeMapping.getType().equals(attribute.getType())) {
							error(String.format(ATTRIBUTE_TYPE_DOES_NOT_CONFORM_ATTRIBUTE_USED_TYPE, attributeMapping.getType()), 
									attribute, ATTRIBUTE__TYPE);
						}
					}
				}
			});
	}
	
	private AntiCorruptionMapping getAntiCorruptionMappingByName(BoundedContext boundedContext, String name) {
		return boundedContext.getAntiCorruptionMappings().stream()
			.filter(antiCorruptionMapping -> antiCorruptionMapping.getName().equals(name))
			.findAny()
			.orElse(null);
	}

	private AttributeMapping getAntiCorruptionMappingAttributeByName(AntiCorruptionMapping antiCorruptionMapping, String name) {
		return antiCorruptionMapping.getAttributeMappings().stream()
			.filter(attributeMapping -> attributeMapping.getName().equals(name))
			.findAny()
			.orElse(null);
	}

	
//	private Entity checkReferencePath(ReferencePath referencePath, Entity entity) {
//		Entity currentEntity = entity.getMapping().getEntity();
//		for (Reference reference: referencePath.getReferences()) {
//			if (currentEntity.getReferences().contains(reference)) {
//				if (!(reference.getDomainObjectType() instanceof Entity)) {
//					error(String.format(REFERENCE_IN_PATH_IS_NOT_ENTITY, reference.getName()), 
//							referencePath, REFERENCE_PATH__REFERENCES);
//				} else {
//					currentEntity = (Entity) reference.getDomainObjectType();
//				}
//			} else {
//				error(String.format(REFERENCE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE, reference.getName()), 
//						referencePath, REFERENCE_PATH__REFERENCES);
//			}
//		}
//		return currentEntity;
//	}
//	
//	private Attribute checkAttributePath(AttributePath attributePath, Map<String, Entity> aliases, Entity entity) {
//		Entity currentEntity;
//		if (attributePath.getAttributePathHead().getRoot() != null) {
//			currentEntity = entity.getMapping().getEntity();
//		} else {
//			currentEntity = aliases.get(attributePath.getAttributePathHead().getAlias().getName());
//		}
//		
//		for (Reference reference: attributePath.getReferences()) {
//			if (currentEntity.getReferences().contains(reference)) {
//				if (!(reference.getDomainObjectType() instanceof Entity)) {
//					error(String.format(REFERENCE_IN_PATH_IS_NOT_ENTITY, reference.getName()), 
//							attributePath, ATTRIBUTE_PATH__REFERENCES);
//				} else {
//					currentEntity = (Entity) reference.getDomainObjectType();
//				}
//			} else {
//				error(String.format(REFERENCE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE, reference.getName()), 
//						attributePath, ATTRIBUTE_PATH__REFERENCES);
//			}
//		}
//		
//		Attribute resultAttribute = currentEntity.getAttributes().stream()
//				.filter(attribute -> attribute.getName().equals(attributePath.getAttribute().getName()))
//				.findAny()
//				.get();
//				
//		if (resultAttribute == null) {
//			error(String.format(ATTRIBUTE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE, attributePath.getAttribute().getName()), 
//					attributePath, ATTRIBUTE_PATH__REFERENCES);
//		}
//		
//		return resultAttribute;
//	}	

}