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

import static org.contextmapper.dsl.validation.ValidationMessages.AGGREGATE_ROOT_CANNOT_USE_TRANSLATION;
import static org.contextmapper.dsl.validation.ValidationMessages.ENTITY_TRANSLATION_DOES_NOT_EXIST_FOR_USE;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_USES_BUT_ENTITY_DOES_NOT;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_USES_VARIABLE_NOT_DEFINED;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_USES_ATTRIBUTE_NOT_DEFINED;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_TYPE_DOES_NOT_CONFORM_ATTRIBUTE_USED_TYPE;
import static org.contextmapper.dsl.validation.ValidationMessages.ENTITY_ATTRIBUTE_USES_DO_NOT_USE_KEY_ATTRIBUTE;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.DOMAIN_OBJECT__AGGREGATE_ROOT;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ENTITY__ANTI_CORRUPTION_TRANSLATION;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__USES;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__VARIABLE;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__USES_ATTRIBUTE;
import static org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage.Literals.ATTRIBUTE__TYPE;

import org.contextmapper.dsl.contextMappingDSL.Aggregate;
import org.contextmapper.dsl.contextMappingDSL.AntiCorruptionTranslation;
import org.contextmapper.dsl.contextMappingDSL.AttributeTranslation;
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
	public void ValidateAggregateRootDoesNotUseTranslation(final Entity entity) {
		if (entity.isAggregateRoot() && entity.isUses()) {
			error(String.format(AGGREGATE_ROOT_CANNOT_USE_TRANSLATION, entity.getName()), 
					entity, DOMAIN_OBJECT__AGGREGATE_ROOT);
		}
	}
	
	@Check
	public void ValidateWellFormedEntityUsesTranslation(final Entity entity) {
		if (entity.isUses()) {
			Aggregate aggregate = (Aggregate) entity.eContainer();
			BoundedContext boundedContext = (BoundedContext) aggregate.eContainer();
			
			if (boundedContext.getAntiCorruptionTranslations().stream()
				.noneMatch(antiCorruptionTranslation -> antiCorruptionTranslation.getName().equals(entity.getAntiCorruptionTranslation()))) {
				error(String.format(ENTITY_TRANSLATION_DOES_NOT_EXIST_FOR_USE, entity.getName()), 
						entity, ENTITY__ANTI_CORRUPTION_TRANSLATION);
			}
		}
	}
	
	@Check
	public void ValidateTranslationKeyAttributeIsUsed(final Entity entity) {
		// this only applies to entities that uses
		if (!entity.isUses()) return;
		
		Aggregate aggregate = (Aggregate) entity.eContainer();
		BoundedContext boundedContext = (BoundedContext) aggregate.eContainer();
		AntiCorruptionTranslation antiCorruptionTranslation = getAntiCorruptionTranslationByName(boundedContext, entity.getAntiCorruptionTranslation());
		String keyAttributeName = antiCorruptionTranslation.getAggregate().getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.filter(Entity::isAggregateRoot)
				.flatMap(entity1 -> entity1.getAttributes().stream())
				.filter(Attribute::isKey)
				.map(Attribute::getName)
				.findAny()
				.orElse(null);
		AttributeTranslation keyAttributeTranslation = getAntiCorruptionTranslationAttributeByName(antiCorruptionTranslation, keyAttributeName);
				
		if (entity.isUses() &&
			entity.getAttributes().stream()
				.filter(Attribute::isUses)
				.noneMatch(attribute -> attribute.getName().equals(keyAttributeTranslation.getName()))) {
			error(String.format(ENTITY_ATTRIBUTE_USES_DO_NOT_USE_KEY_ATTRIBUTE, keyAttributeTranslation.getName()), 
					entity, ENTITY__ANTI_CORRUPTION_TRANSLATION);
		}
	}

	@Check
	public void ValidateWellFormedAttributesUsesTranslation(final Entity entity) {	
		// this only applies to entities that uses
		if (!entity.isUses()) return;
		
		Aggregate aggregate = (Aggregate) entity.eContainer();
		BoundedContext boundedContext = (BoundedContext) aggregate.eContainer();
		AntiCorruptionTranslation antiCorruptionTranslation = getAntiCorruptionTranslationByName(boundedContext, entity.getAntiCorruptionTranslation());

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
					AttributeTranslation attributeTranslation = getAntiCorruptionTranslationAttributeByName(antiCorruptionTranslation, 
							attribute.getUsesAttribute());
					if (attributeTranslation == null) {
						error(String.format(ATTRIBUTE_USES_ATTRIBUTE_NOT_DEFINED, antiCorruptionTranslation.getName()), 
								attribute, ATTRIBUTE__USES_ATTRIBUTE);
					} else {
						if (!attributeTranslation.getType().equals(attribute.getType())) {
							error(String.format(ATTRIBUTE_TYPE_DOES_NOT_CONFORM_ATTRIBUTE_USED_TYPE, attributeTranslation.getType()), 
									attribute, ATTRIBUTE__TYPE);
						}
					}
				}
			});
	}
	
	private AntiCorruptionTranslation getAntiCorruptionTranslationByName(BoundedContext boundedContext, String name) {
		return boundedContext.getAntiCorruptionTranslations().stream()
			.filter(antiCorruptionTranslation -> antiCorruptionTranslation.getName().equals(name))
			.findAny()
			.orElse(null);
	}

	private AttributeTranslation getAntiCorruptionTranslationAttributeByName(AntiCorruptionTranslation antiCorruptionTranslation, String name) {
		return antiCorruptionTranslation.getAttributeTranslations().stream()
			.filter(attributeTranslation -> attributeTranslation.getName().equals(name))
			.findAny()
			.orElse(null);
	}
}