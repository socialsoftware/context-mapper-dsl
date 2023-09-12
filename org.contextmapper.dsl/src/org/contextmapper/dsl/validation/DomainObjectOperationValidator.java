package org.contextmapper.dsl.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.contextmapper.dsl.validation.ValidationMessages.DOMAIN_OBJECT_OPERATION_PARAMETERS_REQUIRE_AGGREGATE_ROOT;
import static org.contextmapper.dsl.validation.ValidationMessages.DOMAIN_OBJECT_OPERATION_PARAMETER_TYPE_ERROR;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_SHOULD_BE_ASSIGNED_IN_CONSTRUCTOR;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_IS_NOT_DECLARED;
import static org.contextmapper.dsl.validation.ValidationMessages.DOMAIN_OBJECT_OPERATION_CONSTRUCTOR_ASSIGNMENTS_SHOULD_MAP_KEY_ATTRIBUTE;
import static org.contextmapper.dsl.validation.ValidationMessages.ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_ONLY_ONCE;
import static org.contextmapper.dsl.validation.ValidationMessages.DOMAIN_OBJECT_OPERATION_CONSTRUCTOR_COMMAND;
import static org.contextmapper.dsl.validation.ValidationMessages.DOMAIN_OBJECT_OPERATION_CONSTRUCTOR_ASSIGMENT_COMMAND_LEFT_PATH_EXPRESSION_SHOULD_USE_THIS;

import org.contextmapper.dsl.contextMappingDSL.Aggregate;
import org.contextmapper.dsl.contextMappingDSL.AssignmentCommand;
import org.contextmapper.tactic.dsl.tacticdsl.Attribute;
import org.contextmapper.tactic.dsl.tacticdsl.CollectionType;
import org.contextmapper.dsl.contextMappingDSL.Command;
import org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage;
import org.contextmapper.tactic.dsl.tacticdsl.ComplexType;
import org.contextmapper.tactic.dsl.tacticdsl.DomainEvent;
import org.contextmapper.tactic.dsl.tacticdsl.DomainObject;
import org.contextmapper.dsl.contextMappingDSL.DomainObjectOperation;
import org.contextmapper.tactic.dsl.tacticdsl.Entity;
import org.contextmapper.tactic.dsl.tacticdsl.Parameter;
import org.contextmapper.dsl.contextMappingDSL.PathExpression;
import org.contextmapper.tactic.dsl.tacticdsl.TacticdslPackage;
import org.contextmapper.tactic.dsl.tacticdsl.ValueObject;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

public class DomainObjectOperationValidator extends AbstractCMLValidator {
	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}
	
	@Check
	public void ValidateOperationParameters(final DomainObjectOperation domainObjectOperation) {
		DomainObject domainObject = (DomainObject) domainObjectOperation.eContainer();
		
		if (domainObject instanceof Entity) return;
		
		if (!domainObjectOperation.getName().equals(domainObject.getName())) {
			return;
		}

		boolean hasAggregateRootEntity = false;
		Aggregate aggregate = (Aggregate) domainObjectOperation.eContainer().eContainer();
		
		for (Parameter parameter: domainObjectOperation.getParameters()) {
			if (isBasic(parameter.getParameterType())) {
				
				continue;
			} else if (isDomainObject(parameter.getParameterType(), aggregate)) {
				if (domainObject instanceof ValueObject && isAggregateRootEntity(parameter.getParameterType(), aggregate)) {
					hasAggregateRootEntity = true;
				}
			} else {
				error(String.format(DOMAIN_OBJECT_OPERATION_PARAMETER_TYPE_ERROR), 
						parameter, TacticdslPackage.Literals.PARAMETER__PARAMETER_TYPE);
			}
		
		}
			if (!hasAggregateRootEntity && domainObject instanceof ValueObject) {
				error(String.format(DOMAIN_OBJECT_OPERATION_PARAMETERS_REQUIRE_AGGREGATE_ROOT), 
						domainObjectOperation, TacticdslPackage.Literals.DOMAIN_OBJECT_OPERATION__PARAMETERS);
			}
	}
	
	@Check
	public void ValidateCommands(final DomainObjectOperation domainObjectOperation) {
		DomainObject domainObject = (DomainObject) domainObjectOperation.eContainer();

		if (!domainObjectOperation.getName().equals(domainObject.getName())) {
			return;
		}
				
		if (domainObject instanceof DomainEvent) return;
		
		Aggregate aggregate = (Aggregate) domainObjectOperation.eContainer().eContainer();
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
		
		String rootEntityParameterName = domainObjectOperation.getParameters().stream()
											.filter(parameter -> isAggregateRootEntity(parameter.getParameterType(), aggregate))
											.map(parameter -> parameter.getName())
											.findAny()
											.orElse(null);
		
		if (rootEntityParameterName == null) return;
				
		boolean isKeyAttributeMapped = false;
		for (Command command: domainObjectOperation.getBody().getCommands()) {
			if (command.getAssigmentCommand() != null) {
				AssignmentCommand assignmentCommand = command.getAssigmentCommand();
				PathExpression leftPathExpression = assignmentCommand.getLeftValue();
				if (!leftPathExpression.getHeadElement().isThis()) {
					error(String.format(DOMAIN_OBJECT_OPERATION_CONSTRUCTOR_ASSIGMENT_COMMAND_LEFT_PATH_EXPRESSION_SHOULD_USE_THIS), 
							leftPathExpression, ContextMappingDSLPackage.Literals.PATH_EXPRESSION__HEAD_ELEMENT);
				}
				
				if (assignmentCommand.getRightValue() instanceof PathExpression) {
					PathExpression rightPathExpression = (PathExpression) assignmentCommand.getRightValue();
									
					if (rightPathExpression.getHeadElement().getVar() != null
							&& rightPathExpression.getHeadElement().getVar().equals(rootEntityParameterName)
							&& rightPathExpression.getProperties().size() == 1
							&& rightPathExpression.getProperties().get(0).getName().equals(keyAttribute.getName())) {
						isKeyAttributeMapped = true;
					}
				}
			} else {
				error(String.format(DOMAIN_OBJECT_OPERATION_CONSTRUCTOR_COMMAND), 
							command, ContextMappingDSLPackage.Literals.COMMAND__OBJECT_COMMAND);
			}
		}
		
		if (!isKeyAttributeMapped) {
			error(String.format(DOMAIN_OBJECT_OPERATION_CONSTRUCTOR_ASSIGNMENTS_SHOULD_MAP_KEY_ATTRIBUTE, keyAttribute.getName()), 
					domainObjectOperation, TacticdslPackage.Literals.DOMAIN_OBJECT_OPERATION__NAME);
		}
	}
	
	@Check
	public void ValidateConstructorAttributes(final DomainObjectOperation domainObjectOperation) {
		DomainObject domainObject = (DomainObject) domainObjectOperation.eContainer();
				
		if (!domainObjectOperation.getName().equals(domainObject.getName())) {
			return;
		}
		
		List<Attribute> attributes = domainObject.getAttributes();
		
		System.out.println(domainObjectOperation.getBody().getCommands().size());
				
		List<AssignmentCommand> assignments = domainObjectOperation.getBody().getCommands().stream()
																.map(command -> command.getAssigmentCommand())
																.collect(Collectors.toList());
		
		Set<String> attributeNames = attributes.stream()
				.map(Attribute::getName)
				.collect(Collectors.toSet());
		
		// assigned attributes only once
		Set<String> assignedAttributeNames = new HashSet<>();
		for (AssignmentCommand assignment: assignments) {
			String attributeName = assignment.getLeftValue().getProperties().get(0).getName();
			if (assignedAttributeNames.add(attributeName) == false) {
				error(String.format(ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_ONLY_ONCE, attributeName), 
						assignment, ContextMappingDSLPackage.Literals.ASSIGNMENT_COMMAND__LEFT_VALUE);
			}
			
		}
		
		// all attributes have assignments
		attributes.forEach(attribute -> {
			if (!assignedAttributeNames.contains(attribute.getName())) {
				error(String.format(ATTRIBUTE_SHOULD_BE_ASSIGNED_IN_CONSTRUCTOR, attribute.getName()), 
						domainObjectOperation, TacticdslPackage.Literals.DOMAIN_OBJECT_OPERATION__NAME);
			}
		});
		
		// all assignments are declared
		assignments.forEach(assignment -> {
			String attributeName = assignment.getLeftValue().getProperties().get(0).getName();
			if (!attributeNames.contains(attributeName)) {
				error(String.format(ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_IS_NOT_DECLARED, attributeName), 
						assignment, ContextMappingDSLPackage.Literals.ASSIGNMENT_COMMAND__LEFT_VALUE);
			}
		});	
	}

	
	private boolean isBasic(ComplexType parameterType) {
		return parameterType.getType() != null;
	}

	private boolean isDomainObject(ComplexType parameterType, Aggregate aggregate) {
		if (parameterType.getCollectionType() != CollectionType.NONE) return false;
		
		return aggregate.getDomainObjects().stream()
			.filter(domainObject -> domainObject.getName().equals(parameterType.getDomainObjectType().getName()))
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
