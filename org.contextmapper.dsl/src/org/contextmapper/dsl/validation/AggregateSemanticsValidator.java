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

import static org.contextmapper.dsl.validation.ValidationMessages.AGGREGATE_CAN_ONLY_HAVE_ONE_AGGREGATE_ROOT;
import static org.contextmapper.dsl.validation.ValidationMessages.AGGREGATE_CAN_ONLY_HAVE_ONE_STATES_ENUM;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_ENTITY_MUST_BE_AGGREGATE_ROOT;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPED_ENTITY_MUST_HAVE_A_KEY_ATTRIBUTE;
import static org.contextmapper.dsl.validation.ValidationMessages.MAPPING_ENTITY_CANNOT_BE_AGGREGATE_ROOT;
import static org.contextmapper.dsl.validation.ValidationMessages.ALIAS_IN_MAPPING_IS_DUPLICATED;
import static org.contextmapper.dsl.validation.ValidationMessages.REFERENCE_IN_PATH_IS_NOT_ENTITY;
import static org.contextmapper.dsl.validation.ValidationMessages.REFERENCE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE;
import static org.contextmapper.dsl.validation.ValidationMessages.ALIAS_NOT_DECLARED;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_MAPPING_NOT_CONFORM_TYPES;
import static org.contextmapper.dsl.validation.ValidationMessages.KEY_ATTRIBUTE_IS_NOT_MAPPED;
import static org.contextmapper.dsl.validation.ValidationMessages.DUPLICATED_MAPPING_ATTRIBUTES;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.DOMAIN_OBJECT__AGGREGATE_ROOT;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ENUM__DEFINES_AGGREGATE_LIFECYCLE;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ENTITY__MAPPING;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.REFERENCE_PATH__REFERENCES;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE_PATH__REFERENCES;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ALIAS_DECLARATION__NAME;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE_PATH__ATTRIBUTE_PATH_HEAD;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE_MAPPING__ATTRIBUTE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.contextmapper.dsl.contextMappingDSL.Aggregate;
import org.contextmapper.tactic.dsl.tacticdsl.AliasDeclaration;
import org.contextmapper.tactic.dsl.tacticdsl.Attribute;
import org.contextmapper.tactic.dsl.tacticdsl.AttributeMapping;
import org.contextmapper.tactic.dsl.tacticdsl.AttributePath;
import org.contextmapper.tactic.dsl.tacticdsl.DomainObject;
import org.contextmapper.tactic.dsl.tacticdsl.Entity;
import org.contextmapper.tactic.dsl.tacticdsl.EntityMapping;
import org.contextmapper.tactic.dsl.tacticdsl.Enum;
import org.contextmapper.tactic.dsl.tacticdsl.Reference;
import org.contextmapper.tactic.dsl.tacticdsl.ReferencePath;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.fasterxml.jackson.core.sym.Name;


public class AggregateSemanticsValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void validateThatAggregateContainsOnlyOneAggregateRoot(final Aggregate aggregate) {
		List<DomainObject> aggregateRoots = aggregate.getDomainObjects().stream().filter(o -> o instanceof DomainObject).map(o -> (DomainObject) o).filter(o -> o.isAggregateRoot())
				.collect(Collectors.toList());

		if (aggregateRoots.size() > 1) {
			for (DomainObject object : aggregateRoots) {
				error(String.format(AGGREGATE_CAN_ONLY_HAVE_ONE_AGGREGATE_ROOT, aggregate.getName()), object, DOMAIN_OBJECT__AGGREGATE_ROOT);
			}
		}
	}

	@Check
	public void validateThatAggregateContainsOnlyOneStatesEnum(final Aggregate aggregate) {
		List<Enum> stateEnums = aggregate.getDomainObjects().stream().filter(o -> o instanceof Enum).map(o -> (Enum) o).filter(o -> o.isDefinesAggregateLifecycle())
				.collect(Collectors.toList());

		if (stateEnums.size() > 1) {
			for (Enum enumm : stateEnums) {
				error(String.format(AGGREGATE_CAN_ONLY_HAVE_ONE_STATES_ENUM, aggregate.getName()), enumm, ENUM__DEFINES_AGGREGATE_LIFECYCLE);
			}
		}
	}
	
	@Check
	public void validateMappedEntitity(final Aggregate aggregate) {
		aggregate.getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.filter(entity -> entity.getMapping() != null)
				.forEach(entity -> {
					Map<String, Entity> aliases = new HashMap<>();

					checkMappingEntityIsNotAggregateRoot(entity);
					checkMappedEntityIsAggregateRoot(entity);
					checkMappedEntityHasAKeyAttribute(entity);
					entity.getMapping().getAliasDeclarations()
						.forEach(aliasDeclaration -> {
							validateAliasDeclaration(aliasDeclaration, aliases, entity);
						});
					
					Set<Attribute> mappedAttributes = new HashSet<>();
					entity.getMapping().getAttributeMappings()
						.forEach(attributeMapping -> {
							validateAttributeMapping(attributeMapping, aliases, entity, mappedAttributes);
						});
					checkUniqueMappedAttributes(entity);
					checkKeyAttributeIsMapped(entity, mappedAttributes);
				});
	}

	private void checkMappingEntityIsNotAggregateRoot(Entity entity) {
		if (entity.isAggregateRoot()) {
			error(String.format(MAPPING_ENTITY_CANNOT_BE_AGGREGATE_ROOT, entity.getName()), 
					entity, ENTITY__MAPPING);
		}
	}
	
	private void checkMappedEntityIsAggregateRoot(Entity entity) {
		if (!entity.getMapping().getEntity().isAggregateRoot()) {
			error(String.format(MAPPED_ENTITY_MUST_BE_AGGREGATE_ROOT, entity.getMapping().getEntity().getName()), 
					entity, ENTITY__MAPPING);
		}
	}

	private void checkMappedEntityHasAKeyAttribute(Entity entity) {
		if (entity.getMapping().getEntity().getAttributes().stream()
			.noneMatch(attribute -> attribute.isKey())) {
			error(String.format(MAPPED_ENTITY_MUST_HAVE_A_KEY_ATTRIBUTE, entity.getMapping().getEntity().getName()), 
					entity.getMapping().getEntity(), ENTITY__MAPPING);
		}
	}

	private void validateAliasDeclaration(AliasDeclaration aliasDeclaration, Map<String, Entity> aliases, Entity entity) {
		checkUniqueMappingAliases(aliasDeclaration, aliases);
		Entity resultEntity = checkReferencePath(aliasDeclaration.getReferencePath(), entity);
		aliases.put(aliasDeclaration.getName(), resultEntity);	
	}

	private Entity checkReferencePath(ReferencePath referencePath, Entity entity) {
		Entity currentEntity = entity.getMapping().getEntity();
		for (Reference reference: referencePath.getReferences()) {
			if (currentEntity.getReferences().contains(reference)) {
				if (!(reference.getDomainObjectType() instanceof Entity)) {
					error(String.format(REFERENCE_IN_PATH_IS_NOT_ENTITY, reference.getName()), 
							referencePath, REFERENCE_PATH__REFERENCES);
				} else {
					currentEntity = (Entity) reference.getDomainObjectType();
				}
			} else {
				error(String.format(REFERENCE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE, reference.getName()), 
						referencePath, REFERENCE_PATH__REFERENCES);
			}
		}
		return currentEntity;
	}
	
	private void checkUniqueMappingAliases(AliasDeclaration aliasDeclaration, Map<String, Entity> aliases) {
		if (aliases.containsKey(aliasDeclaration.getName())) {
			error(String.format(ALIAS_IN_MAPPING_IS_DUPLICATED, aliasDeclaration.getName()), 
					aliasDeclaration, ALIAS_DECLARATION__NAME);
		}
	}

	private void validateAttributeMapping(AttributeMapping attributeMapping, Map<String, Entity> aliases, Entity entity, Set<Attribute> mappedAttributes) {
		checkUniqueAlias(attributeMapping, aliases);
		Attribute resultAttribute = checkAttributePath(attributeMapping.getAttributePath(), aliases, entity);
		checkAttributeMappingTypes(attributeMapping, resultAttribute);
		mappedAttributes.add(resultAttribute);
	}

	private void checkUniqueAlias(AttributeMapping attributeMapping, Map<String, Entity> aliases) {
		AliasDeclaration aliasDeclaration = attributeMapping.getAttributePath().getAttributePathHead().getAlias();
		if (aliasDeclaration != null && !aliases.keySet().contains(aliasDeclaration.getName())) {
			error(String.format(ALIAS_NOT_DECLARED, aliasDeclaration.getName()), 
					attributeMapping.getAttributePath(), ATTRIBUTE_PATH__ATTRIBUTE_PATH_HEAD);
		}
	}
	
	private Attribute checkAttributePath(AttributePath attributePath, Map<String, Entity> aliases, Entity entity) {
		Entity currentEntity;
		if (attributePath.getAttributePathHead().getRoot() != null) {
			currentEntity = entity.getMapping().getEntity();
		} else {
			currentEntity = aliases.get(attributePath.getAttributePathHead().getAlias().getName());
		}
		
		for (Reference reference: attributePath.getReferences()) {
			if (currentEntity.getReferences().contains(reference)) {
				if (!(reference.getDomainObjectType() instanceof Entity)) {
					error(String.format(REFERENCE_IN_PATH_IS_NOT_ENTITY, reference.getName()), 
							attributePath, ATTRIBUTE_PATH__REFERENCES);
				} else {
					currentEntity = (Entity) reference.getDomainObjectType();
				}
			} else {
				error(String.format(REFERENCE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE, reference.getName()), 
						attributePath, ATTRIBUTE_PATH__REFERENCES);
			}
		}
		
		Attribute resultAttribute = currentEntity.getAttributes().stream()
				.filter(attribute -> attribute.getName().equals(attributePath.getAttribute().getName()))
				.findAny()
				.get();
				
		if (resultAttribute == null) {
			error(String.format(ATTRIBUTE_IN_PATH_IS_NOT_CORRECT_ENTITY_REFERENCE, attributePath.getAttribute().getName()), 
					attributePath, ATTRIBUTE_PATH__REFERENCES);
		}
		
		return resultAttribute;
	}
	
	private void checkAttributeMappingTypes(AttributeMapping attributeMapping, Attribute resultAttribute) {
		if (!attributeMapping.getAttribute().getType().equals(resultAttribute.getType())) {
			error(String.format(ATTRIBUTE_MAPPING_NOT_CONFORM_TYPES, resultAttribute.getType()), 
					attributeMapping, ATTRIBUTE_MAPPING__ATTRIBUTE);
		}
	}
	
	private void checkKeyAttributeIsMapped(Entity entity, Set<Attribute> mappedAttributes) {
		if (mappedAttributes.stream()
			.noneMatch(Attribute::isKey)) {
			error(String.format(KEY_ATTRIBUTE_IS_NOT_MAPPED, ""), 
					entity, ENTITY__MAPPING);
		}
	}
	
	private void checkUniqueMappedAttributes(Entity entity) {
		Set<String> names = new HashSet<>();
		entity.getMapping().getAttributeMappings()
			.forEach(attributeMapping -> {
				String name = attributeMapping.getAttribute().getName();
				if (names.contains(name)) {
					error(String.format(DUPLICATED_MAPPING_ATTRIBUTES, name), 
							attributeMapping, ATTRIBUTE_MAPPING__ATTRIBUTE);
				}
				names.add(name);
			});
	}
}