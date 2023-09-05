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

public class ValidationMessages {
	/* Semantical problems in model */
	public static final String CUSTOMER_SUPPLIER_WITH_CONFORMIST_ERROR_MESSAGE = "The CONFORMIST pattern is not applicable for a Customer-Supplier relationship.";
	public static final String CUSTOMER_SUPPLIER_WITH_OHS_ERROR_MESSAGE = "The OPEN-HOST SERVICE pattern is not applicable for a Customer-Supplier relationship.";
	public static final String CUSTOMER_SUPPLIER_WITH_ACL_WARNING_MESSAGE = "Are you sure you need an ANTICORRUPTION LAYER here? This pattern should not be necessarily needed in a Customer-Supplier relationship.";
	public static final String RELATIONSHIP_CONTEXT_NOT_ON_MAP_ERROR_MESSAGE = "The Bounded Context '%s' is not part of the Context Map.";
	public static final String SYSTEM_LANDSCAPE_MAP_CONTAINS_TEAM = "A Bounded Context of type 'TEAM' is not allowed on a SYSTEM_LANDSCAPE map.";
	public static final String ONLY_TEAMS_CAN_REALIZE_OTHER_BOUNDED_CONTEXT = "Only teams can realize bounded contexts. '%s' is not a team!";
	public static final String EXPOSED_AGGREGATE_NOT_PART_OF_UPSTREAM_CONTEXT = "The aggregate '%s' is not part of the upstream context '%s'.";
	public static final String SELF_RELATIONSHIP_NOT_ALLOWED = "Bounded context relationships must be declared between two different bounded contexts.";
	public static final String OWNER_BC_IS_NOT_TEAM = "'%s' is not a team. The owner attribute must refer to a Bounded Context representing a team (type = TEAM)!";
	public static final String AGGREGATE_CAN_ONLY_HAVE_ONE_AGGREGATE_ROOT = "Your aggregate '%s' contains multiple aggregate roots. An aggregate must only contain one root.";
	public static final String AGGREGATE_CAN_ONLY_HAVE_ONE_STATES_ENUM = "Your aggregate '%s' contains multiple enums that define its states. Ensure that only one enum declares the states of the Aggregate.";
	public static final String ALREADY_IMPLEMENTED_SUBDOMAIN = "The subdomain '%s' is already implemented through its domain '%s'.";
	public static final String MULTIPLE_DOMAINS_IMPLEMENTED = "Are you sure you want to implement multiple Domains within one Bounded Context? A Bounded Context should typically implement only a part of your Domain; one or multiple Subdomains.";
	public static final String ORGANIZATIONAL_MAP_DOES_NOT_CONTAIN_TEAM = "Your Context Map is of the type ORGANIZATIONAL but does not contain Bounded Contexts of the type TEAM. This type of Context Map is intended to model team relationships.";
	public static final String COMMAND_OR_OPERATION_IS_NOT_PART_OF_BOUNDED_CONTEXT = "The command or operation '%s' is not part of the '%s' Bounded Context. Please ensure that your workflow only uses commands, operations and events that are part of the same Bounded Context.";
	public static final String STATE_VALUE_DOES_NOT_BELONG_TO_AGGREGATE = "'%s' is not a state of the Aggregate '%s'.";
	public static final String FUNCTIONALITY_STEP_CONTEXT_NOT_ON_MAP = "The Bounded Context '%s' is not part of the Context Map.";
	public static final String FUNCTIONALITY_STEP_SERVICE_NOT_ON_STEP_CONTEXT_APPLICATION = "The Service '%s' is not part of the Bounded Context '%s' Application.";
	public static final String FUNCTIONALITY_STEP_OPERATION_NOT_ON_STEP_SERVICE = "The operation '%s' is not part of the Service '%s' of the Bounded Context '%s' Application.";

	public static final String AGGREGATE_ROOT_CANNOT_USE_VAlUE_OBJECT = "Aggregate root '%s' cannot use a value object.";
	public static final String AGGREGATE_DOES_NOT_BELONG_TO_BOUNDED_CONTEXT = "Aggregate does not belong to bounded context '%s'.";
	public static final String VALUE_OBJECT_DOES_NOT_BELONG_TO_AGGREGATE = "Value object does not belong to aggregate '%s'.";
	public static final String AGGREGATE_VALUE_OBJECT_SHOULD_HAVE_ROOT_ENTITY = "Aggregate should have root entity.";
	public static final String MAPPED_BOUNDED_CONTEXT_IS_NOT_UPSTREAM = "Mapped bounded context '%s' is not upstream.";
	public static final String NO_RELATIONSHIP_BETWEEN_BOUNDED_CONTEXTS = "Bounded contexts should have a relationship.";
	public static final String BOUNDED_CONTEXT_IS_NOT_DEFINED = "Bounded context '%s' not defined";
	public static final String USED_VAlUE_OBJECT_BODY_RESTRICTIONS = "Value object '%s' can only have attributes and constructor.";
	public static final String USED_VAlUE_OBJECT_HAS_NO_CONSTRUCTOR = "Value object '%s' should have constructor.";

	public static final String INVARIANT_EXPRESSION_MUST_BE_BOOLEAN_OR_FINAL = "Invariant expression must be boolean or final, instead of %s.";
	public static final String BOOLEAN_EXPRESSION_INCORRECT_TYPE = "Expected a boolean expression but got %s.";
	public static final String COMPARISON_EXPRESSION_INCORRECT_TYPE = "Incompatible types %s in comparison expression.";
	public static final String IS_NOT_NUMERIC_TYPE = "%s is not numeric type.";
	public static final String TERNARY_EXPRESSION_CONDITION_MUST_BE_BOOLEAN = "Ternary expression condition must be boolean intead of %s.";
	public static final String FINAL_EXPRESSION_ONLY_ALLOWED_IN_INVARIANT = "Final Expression only allowed in aggregate Invariant.";
	public static final String PROPERTY_IN_PATH_IS_NOT_CORRECT_ENTITY_PROPERTY = "Property %s in path is not a correct entity property.";
	public static final String INVARIANT_CANNOT_CONTAIN_QUERY = "Aggregate invariant cannot contain query %s.";
	public static final String CONSTRUCTOR_ASSIGNMENT_CANNOT_CONTAIN_QUERY = "Constructor assignment cannot contain query %s.";
	public static final String INVARIANT_CANNOT_CONTAIN_VAR = "Aggregate invariant cannot contain var %s.";
	public static final String METHOD_LOCAL_VARIABLE_NOT_DECLARED = "Method local variable %s not declared.";
	public static final String PATH_EXPRESSION_HEAD_MUST_BE_OBJECT = "Path expresssion head %s must be object.";
	public static final String QUERY_DOES_NOT_HAVE_ASSOCIATED_REPOSITORY = "Query %s does not have associated repository.";
	public static final String QUERY_OPERATION_IS_NOT_DEFINED = "Query operation %s is not defined in the repository.";
	public static final String NUMBER_QUERY_PARAMETERS_ARE_NOT_CONSISTENT = "Number of query %s parameters are not consistent with definition.";
	public static final String QUERY_PARAM_IS_NOT_DECLARED = "Query param %s is not declared.";
	public static final String QUERY_PARAM_TYPE_DOES_NOT_MATCH = "Query param %s type does not match repository operation parameter type.";
	public static final String QUERY_DOES_NOT_RETURN_ENTITY = "Query %s does not return entity.";
	public static final String QUERY_RETURNED_ENTITY_DOES_NOT_BELONG_TO_AGGREGATE = "Query returned entity %s does not belong to mapped aggregate.";
	public static final String METHOD_REQUIRES_COLLECTION = "Method requires collection.";
	public static final String VARIABLE_ALREADY_DECLARED_IN_SCOPE = "Variable already declared in scope.";
	public static final String METHOD_REQUIRES_OPTIONAL = "Method requires optional.";
	public static final String CONSTRUCTOR_ASSIGNMENT_CANNOT_CONTAIN_ROOT = "Constructor assignment cannot contain root.";
	public static final String CONSTRUCTOR_ASSIGNMENT_PATH_HEAD_NOT_DECLARED_AS_PARAMETER = "Constructor assigment head %s not declared as parameter.";
	public static final String CONSTRUCTOR_ASSIGNMENT_EXPRESSION_TYPE_ERROR = "Constructor assigment expression should be of type %s.";
	public static final String CONSTRUCTOR_PARAMETER_TYPE_ERROR = "Constructor parameter should be simple type of aggregate entity type.";
	public static final String CONSTRUCTOR_PARAMETERS_REQUIRE_AGGREGATE_ROOT = "Constructor parameters require root aggregate.";
	public static final String CONSTRUCTOR_ASSIGNMENTS_SHOULD_MAP_KEY_ATTRIBUTE = "Constructor assignements should map the key attribute %s of the aggregate root entity.";
	public static final String ATTRIBUTE_SHOULD_BE_ASSIGNED_IN_CONSTRUCTOR = "Attribute %s is not assigned in constructor.";
	public static final String ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_IS_NOT_DECLARED = "Attribute is not declared.";
	public static final String ATTRIBUTE_IN_CONSTRUCTOR_ASSIGNMENT_ONLY_ONCE = "Attribute is assigned more than once.";
	public static final String ROOT_ENTITY_OF_MAPPED_AGGREGATE_SHOULD_HAVE_A_KEY_ATTRIBUTE = "Root entity %s of mapped aggreagate should have a key attribute.";

	/* Uniqueness problems */
	public static final String BOUNDED_CONTEXT_NAME_NOT_UNIQUE = "Multiple bounded contexts with the name '%s' have been declared.";
	public static final String AGGREGATE_NAME_NOT_UNIQUE = "Multiple aggregates with the name '%s' have been declared.";
	public static final String MODULE_NAME_NOT_UNIQUE = "Duplicate name. There is already an existing Module named '%s'.";
	public static final String USE_CASE_NAME_NOT_UNIQUE = "Multiple use cases with the name '%s' have been declared.";
	public static final String DOMAIN_OBJECT_NOT_UNIQUE = "Multiple domain objects with the name '%s' have been declared.";
	public static final String DOMAIN_NOT_UNIQUE = "Multiple domains with the name '%s' have been declared.";
	public static final String SUBDOMAIN_OBJECT_NOT_UNIQUE = "Multiple subdomains with the name '%s' have been declared.";
	public static final String SUBDOMAIN_NAME_EQUALS_DOMAIN_NAME = "The subdomain '%s' has the same name as the domain. Please define a unique subdomain name.";
	public static final String SERVICE_NAME_NOT_UNIQUE_IN_BC = "Multiple services with the name '%s' have been declared in this Bounded Context.";
	public static final String SERVICE_NAME_NOT_UNIQUE_IN_SUBDOMAIN = "Multiple services with the name '%s' have been declared in this Subdomain.";
	public static final String FLOW_NAME_NOT_UNIQUE = "Multiple flows with the name '%s' have been declared. Please provide unique names!";
	public static final String FUNCTIONALITY_NAME_NOT_UNIQUE = "Multiple functionalities with the name '%s' have been declared. Please provide unique names!";

	public static final String INVARIANT_NAME_NOT_UNIQUE = "Multiple invariantes with the name '%s' have been declared.";
	
	/* Generator problems */
	public static final String EMPTY_UML_COMPONENT_DIAGRAM_MESSAGE = "Sorry, we cannot generate a component diagram. Your Context Map seems to be empty.";
	public static final String EMPTY_UML_CLASS_DIAGRAM_MESSAGE = "Sorry, we cannot generate a class diagram for this Bounded Context. There are no Aggregates or none of the Aggregates contain any domain objects (entities, value objects or domain events).";
	public static final String EMPTY_UML_CLASS_DIAGRAM_MESSAGE_AGGREGATE = "Sorry, we cannot generate a class diagram for this Aggregate. There are no domain objects (entities, value objects or domain events).";
	public static final String EMPTY_UML_CLASS_DIAGRAM_MESSAGE_MODULE = "Sorry, we cannot generate a class diagram for this module. There are no domain objects (entities, value objects or domain events).";

	/* General input problems */
	public static final String ENTITY_NAME_CONTAINS_INVALID_CHARACTERS = "The entity name should consist of the following characters: a-z, A-Z, 0-9, _. Please rename the entity accordingly before applying transformations.";
	public static final String VERB_CONTAINS_INVALID_CHARACTERS = "The verb should consist of the following characters: a-z, A-Z, 0-9, _. Please rename it accordingly before applying transformations.";
	public static final String STRING_IS_NOT_NANOENTITY = "The string '%s' does not match the format of a Nanoentity. Please use the following format: 'Entity.attribute' (Example: 'Customer.firstName')";

	/* Warnings and suggestions */
	public static final String MODULE_CONTAINS_POTENTIALLY_IGNORED_OBJECTS = "The module '%s' contains Services and/or Entities that are not part of an Aggregate. The MDSL and Service Cutter generators ignore them. Please move them into an Aggregate.";
	public static final String PRIMITIVE_ID_TYPE = "This attribute seems to be an identifier. Have you thought about creating a Value Object?";
	public static final String REFERENCE_IS_AMBIGUOUS = "The reference to the type '%s' is ambiguous, since there exist multiple domain objects with that name in your model. We suggest to keep the names of domain objects distinct.";
	public static final String DOMAIN_OBJECT_NAME_ALREADY_EXISTS = "The name '%s' is used for multiple domain objects. We suggest to keep the names of domain objects distinct.";
	public static final String REFERENCE_TO_NOT_REACHABLE_TYPE = "The object '%s' is not part of this Bounded Context and not reachable through Context Map relationships. Maybe you want to create a relationship with the context that contains that object?";

	/* Quickfix suggestions */
	public static final String SPLIT_STORY_BY_VERB_SUGGESTION = "You could split your story by its operation/verb.";
	public static final String VISUALIZE_FLOW_WITH_SKETCH_MINER = "Do you want to vizualize this flow? You can open it in BPMN Sketch Miner ...";
}
