package org.contextmapper.dsl.validation;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.contextmapper.dsl.validation.ValidationMessages.CONSTRUCTOR_PARAMETERS_REQUIRE_AGGREGATE_ROOT;
import static org.contextmapper.dsl.validation.ValidationMessages.CONSTRUCTOR_PARAMETER_TYPE_ERROR;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_SHOULD_BE_ASSIGNED_IN_CONSTRUCTOR;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_IS_NOT_DECLARED;
import static org.contextmapper.dsl.validation.ValidationMessages.CONSTRUCTOR_ASSIGNMENTS_SHOULD_MAP_KEY_ATTRIBUTE;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_ONLY_ONCE;

import org.contextmapper.dsl.contextMappingDSL.Aggregate;
import org.contextmapper.tactic.dsl.tacticdsl.Attribute;
import org.contextmapper.tactic.dsl.tacticdsl.CollectionType;
import org.contextmapper.tactic.dsl.tacticdsl.ComplexType;
import org.contextmapper.tactic.dsl.tacticdsl.Constructor;
import org.contextmapper.tactic.dsl.tacticdsl.ConstructorAssignment;
import org.contextmapper.tactic.dsl.tacticdsl.DomainEvent;
import org.contextmapper.tactic.dsl.tacticdsl.DomainObject;
import org.contextmapper.tactic.dsl.tacticdsl.Entity;
import org.contextmapper.tactic.dsl.tacticdsl.Parameter;
import org.contextmapper.tactic.dsl.tacticdsl.PathExpression;
import org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage;
import org.contextmapper.tactic.dsl.tacticdsl.ValueObject;
import org.eclipse.emf.common.util.WeakInterningHashSet;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;


public class ConstructorValidator extends AbstractCMLValidator {
	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}
	
	@Check
	public void ValidateConstructorParameters(final Constructor constructor) {
		boolean hasAggregateRootEntity = false;
		DomainObject domainObject = (DomainObject) constructor.eContainer();
		Aggregate aggregate = (Aggregate) constructor.eContainer().eContainer();
		
		for (Parameter parameter: constructor.getParameters()) {
			if (isBasic(parameter.getParameterType())) {
				continue;
			} else if (isEntity(parameter.getParameterType(), aggregate)) {
				if (domainObject instanceof ValueObject && isAggregateRootEntity(parameter.getParameterType(), aggregate)) {
					hasAggregateRootEntity = true;
				}
			} else {
				error(String.format(CONSTRUCTOR_PARAMETER_TYPE_ERROR), 
						parameter, TacticdslPackage.Literals.PARAMETER__PARAMETER_TYPE);
			}
		
		}
			if (!hasAggregateRootEntity && domainObject instanceof ValueObject) {
				error(String.format(CONSTRUCTOR_PARAMETERS_REQUIRE_AGGREGATE_ROOT), 
						constructor, TacticdslPackage.Literals.CONSTRUCTOR__PARAMETERS);
			}
	}
	
	@Check
	public void ValidateAggregateRootKeyAttributeIsMapped(final Constructor constructor) {
		DomainObject domainObject = (DomainObject) constructor.eContainer();
		
		if (domainObject instanceof DomainEvent) return;
		
		Aggregate aggregate = (Aggregate) constructor.eContainer().eContainer();
		Entity rootEntity = aggregate.getDomainObjects().stream()
								.filter(Entity.class::isInstance)
								.map(Entity.class::cast)
								.filter(Entity::isAggregateRoot)
								.findAny()
								.orElse(null);
		
		if (rootEntity == null) return;
		
		Attribute keyAttribute = rootEntity.getAttributes().stream()
									.filter(Attribute::isKey)
									.findAny()
									.orElse(null);

		if (keyAttribute == null) return;
		
		String rootEntityParameterName = constructor.getParameters().stream()
											.filter(parameter -> isAggregateRootEntity(parameter.getParameterType(), aggregate))
											.map(parameter -> parameter.getName())
											.findAny()
											.orElse(null);
		
		if (rootEntityParameterName == null) return;
				
		boolean isKeyAttributeMapped = false;
		for (ConstructorAssignment assignment: constructor.getBody().getAssignments()) {
			if (assignment.getExpression() instanceof PathExpression) {
				PathExpression pathExpression = (PathExpression) assignment.getExpression();
				
				if (pathExpression.getHeadElement().getVar() != null
						&& pathExpression.getHeadElement().getVar().equals(rootEntityParameterName)
						&& pathExpression.getProperties().size() == 1
						&& pathExpression.getProperties().get(0).getName().equals(keyAttribute.getName())) {
					isKeyAttributeMapped = true;
				}
			} 
				
		}
		
		if (!isKeyAttributeMapped) {
			error(String.format(CONSTRUCTOR_ASSIGNMENTS_SHOULD_MAP_KEY_ATTRIBUTE, keyAttribute.getName()), 
					constructor, TacticdslPackage.Literals.CONSTRUCTOR__NAME);
		}

	}
	
	@Check
	public void ValidateConstructorAttributes(final Constructor constructor) {
		DomainObject domainObject = (DomainObject) constructor.eContainer();
		
		List<Attribute> attributes = domainObject.getAttributes();
		List<ConstructorAssignment> assignments = constructor.getBody().getAssignments();
		
		
		Set<String> attributeNames = attributes.stream()
				.map(Attribute::getName)
				.collect(Collectors.toSet());
		
		// assigned attributes only once
		Set<String> assignedAttributeNames = new HashSet<>();
		for (ConstructorAssignment assignment: assignments) {
			if (assignedAttributeNames.add(assignment.getAttribute().getName()) == false) {
				error(String.format(ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_ONLY_ONCE, assignment.getAttribute().getName()), 
						assignment, TacticdslPackage.Literals.CONSTRUCTOR_ASSIGNMENT__ATTRIBUTE);
			}
			
		}
		
		// all attributes have assignments
		attributes.forEach(attribute -> {
			System.out.println(attribute.getName());
			if (!assignedAttributeNames.contains(attribute.getName())) {
				error(String.format(ATTRIBUTE_SHOULD_BE_ASSIGNED_IN_CONSTRUCTOR, attribute.getName()), 
						constructor, TacticdslPackage.Literals.CONSTRUCTOR__NAME);
			}
		});
		
		// all assignments are declared
		assignments.forEach(assignment -> {
			if (!attributeNames.contains(assignment.getAttribute().getName())) {
				error(String.format(ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_IS_NOT_DECLARED, assignment.getAttribute().getName()), 
						assignment, TacticdslPackage.Literals.CONSTRUCTOR_ASSIGNMENT__ATTRIBUTE);
			}
		});
		

		
		// verify types of basic assignments
	
	}

	
	private boolean isBasic(ComplexType parameterType) {
		return parameterType.getType() != null;
	}

	private boolean isEntity(ComplexType parameterType, Aggregate aggregate) {
		if (parameterType.getCollectionType() != CollectionType.NONE) return false;
		
		return aggregate.getDomainObjects().stream()
			.filter(Entity.class::isInstance)
			.map(Entity.class::cast)
			.filter(entity -> entity.getName().equals(parameterType.getDomainObjectType().getName()))
			.findFirst()
			.isPresent();
	}
	
	private boolean isAggregateRootEntity(ComplexType parameterType, Aggregate aggregate) {
		return aggregate.getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.filter(entity -> entity.getName().equals(parameterType.getDomainObjectType().getName()))
				.filter(Entity::isAggregateRoot)
				.findFirst()
				.isPresent();
	}
}
