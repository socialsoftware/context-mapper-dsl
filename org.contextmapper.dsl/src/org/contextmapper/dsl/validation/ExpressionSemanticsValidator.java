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

import static org.contextmapper.dsl.validation.ValidationMessages.INTRA_INVARIANT_CANNOT_CONTAIN_QUERY;
import static org.contextmapper.dsl.validation.ValidationMessages.INTER_INVARIANT_CANNOT_CONTAIN_ROOT;
import static org.contextmapper.dsl.validation.ValidationMessages.INTRA_INVARIANT_CANNOT_CONTAIN_VAR;
import static org.contextmapper.dsl.validation.ValidationMessages.QUERY_DOES_NOT_HAVE_ASSOCIATED_REPOSITORY;
import static org.contextmapper.dsl.validation.ValidationMessages.QUERY_OPERATION_IS_NOT_DEFINED;
import static org.contextmapper.dsl.validation.ValidationMessages.NUMBER_QUERY_PARAMETERS_ARE_NOT_CONSISTENT;
import static org.contextmapper.dsl.validation.ValidationMessages.QUERY_DOES_NOT_RETURN_ENTITY;
import static org.contextmapper.dsl.validation.ValidationMessages.QUERY_RETURNED_ENTITY_DOES_NOT_BELONG_TO_AGGREGATE;
import static org.contextmapper.dsl.validation.ValidationMessages.QUERY_PARAM_TYPE_DOES_NOT_MATCH;

import static org.contextmapper.dsl.validation.ValidationMessages.QUERY_PARAM_IS_NOT_DECLARED;
import static org.contextmapper.dsl.validation.ValidationMessages.BOOLEAN_EXPRESSION_INCORRECT_TYPE;
import static org.contextmapper.dsl.validation.ValidationMessages.COMPARISON_EXPRESSION_INCORRECT_TYPE;
import static org.contextmapper.dsl.validation.ValidationMessages.IS_NOT_NUMERIC_TYPE;
import static org.contextmapper.dsl.validation.ValidationMessages.INVARIANT_EXPRESSION_MUST_BE_BOOLEAN;
import static org.contextmapper.dsl.validation.ValidationMessages.INVARIANT_EXPRESSION_MUST_BE_BOOLEAN_OR_FINAL;
import static org.contextmapper.dsl.validation.ValidationMessages.INTER_INVARIANT_ATTRIBUTE_NOT_DECLARED_IN_ANTICORRUPTION_TRANSLATION;
import static org.contextmapper.dsl.validation.ValidationMessages.PROPERTY_IN_PATH_IS_NOT_CORRECT_ENTITY_PROPERTY;
import static org.contextmapper.dsl.validation.ValidationMessages.METHOD_REQUIRES_COLLECTION;
import static org.contextmapper.dsl.validation.ValidationMessages.METHOD_REQUIRES_OPTIONAL;
import static org.contextmapper.dsl.validation.ValidationMessages.VARIABLE_ALREADY_DECLARED_IN_SCOPE;
import static org.contextmapper.dsl.validation.ValidationMessages.METHOD_LOCAL_VARIABLE_NOT_DECLARED;
import static org.contextmapper.dsl.validation.ValidationMessages.PATH_EXPRESSION_HEAD_MUST_BE_OBJECT;
import static org.contextmapper.dsl.validation.ValidationMessages.TERNARY_EXPRESSION_CONDITION_MUST_BE_BOOLEAN;
import static org.contextmapper.dsl.validation.ValidationMessages.FINAL_EXPRESSION_ONLY_ALLOWED_IN_INTRA_INVARIANT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.contextmapper.dsl.contextMappingDSL.Addition;
import org.contextmapper.dsl.contextMappingDSL.Aggregate;
import org.contextmapper.dsl.contextMappingDSL.AntiCorruptionTranslation;
import org.contextmapper.dsl.contextMappingDSL.ArithmeticSigned;
import org.contextmapper.dsl.contextMappingDSL.BooleanExpression;
import org.contextmapper.dsl.contextMappingDSL.BooleanLiteral;
import org.contextmapper.dsl.contextMappingDSL.BooleanNegation;
import org.contextmapper.dsl.contextMappingDSL.Comparison;
import org.contextmapper.dsl.contextMappingDSL.ContextMappingDSLPackage;
import org.contextmapper.dsl.contextMappingDSL.Expression;
import org.contextmapper.dsl.contextMappingDSL.FinalElement;
import org.contextmapper.dsl.contextMappingDSL.FinalExpression;
import org.contextmapper.dsl.contextMappingDSL.InterAggregateInvariant;
import org.contextmapper.dsl.contextMappingDSL.IntraAggregateInvariant;
import org.contextmapper.dsl.contextMappingDSL.Method;
import org.contextmapper.dsl.contextMappingDSL.Multiplication;
import org.contextmapper.dsl.contextMappingDSL.NowLiteral;
import org.contextmapper.dsl.contextMappingDSL.NullLiteral;
import org.contextmapper.dsl.contextMappingDSL.NumberLiteral;
import org.contextmapper.dsl.contextMappingDSL.ParametricMethod;
import org.contextmapper.dsl.contextMappingDSL.ParenthesizedExpression;
import org.contextmapper.dsl.contextMappingDSL.PathExpression;
import org.contextmapper.dsl.contextMappingDSL.Query;
import org.contextmapper.dsl.contextMappingDSL.SimpleMethod;
import org.contextmapper.dsl.contextMappingDSL.StringLiteral;
import org.contextmapper.dsl.contextMappingDSL.TernaryExpression;
import org.contextmapper.tactic.dsl.tacticdsl.CollectionType;
import org.contextmapper.tactic.dsl.tacticdsl.ComplexType;
import org.contextmapper.tactic.dsl.tacticdsl.Attribute;
import org.contextmapper.tactic.dsl.tacticdsl.Entity;
import org.contextmapper.tactic.dsl.tacticdsl.Property;
import org.contextmapper.tactic.dsl.tacticdsl.Reference;
import org.contextmapper.tactic.dsl.tacticdsl.Repository;
import org.contextmapper.tactic.dsl.tacticdsl.RepositoryOperation;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.google.common.reflect.Parameter;


public class ExpressionSemanticsValidator extends AbstractCMLValidator {
	private static final String FINAL = "Final";
	private static final String BIG_INTEGER = "BigInteger";
	private static final String TIMESTAMP = "Timestamp";
	private static final String BIG_DECIMAL = "BigDecimal";
	private static final String DATE = "Date";
	private static final String DOUBLE = "Double";
	private static final String LONG = "Long";
	private static final String FLOAT = "Float";
	private static final String INTEGER = "Integer";
	private static final String NULL = "null";
	private static final String DATE_TIME = "DateTime";
	private static final String STRING = "String";
	private static final String ERROR = "Error";
	private static final String BOOLEAN = "Boolean";
	
	private Set<String> objectTypes = new HashSet<>(Arrays.asList(STRING,INTEGER,LONG,BOOLEAN,DATE,DATE_TIME,TIMESTAMP,BIG_DECIMAL,BIG_INTEGER,DOUBLE,FLOAT));


	enum ScopeType {
		INTRA, INTER, METHOD
	}

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}
	
	@Check
	public void ValidateExpression(final Expression expression) {
		Aggregate scopeAggregate = null;
		Map<String, String> scopeVariables = new HashMap<>();
		ScopeType scopeType;
		IntraAggregateInvariant intraAggregateInvariant = null;
		InterAggregateInvariant interAggregateInvariant = null;

		if (expression.eContainer() instanceof InterAggregateInvariant) {
			scopeType = ScopeType.INTER; 
			interAggregateInvariant = (InterAggregateInvariant) expression.eContainer();
			AntiCorruptionTranslation antiCorruptionTranslation = (AntiCorruptionTranslation) interAggregateInvariant.eContainer();
			scopeAggregate = antiCorruptionTranslation.getAggregate();
			antiCorruptionTranslation.getAttributeTranslations().stream()
				.forEach(attributeTranslation -> {
					scopeVariables.put(attributeTranslation.getName(), attributeTranslation.getType());
				});
		} else if (expression.eContainer() instanceof IntraAggregateInvariant) {
			scopeType = ScopeType.INTRA;
			intraAggregateInvariant = (IntraAggregateInvariant) expression.eContainer();
			scopeAggregate = (Aggregate) intraAggregateInvariant.eContainer();
		} else {
			return;
		} 
		
		objectTypes.addAll(getAggregateEntityNames(scopeAggregate));
		
		String type = dispatch(expression, scopeAggregate, scopeVariables, scopeType);
		
		if (!areCompatibleTypes(type, BOOLEAN) && scopeType.equals(ScopeType.INTER)) {
			error(String.format(INVARIANT_EXPRESSION_MUST_BE_BOOLEAN, type), 
					interAggregateInvariant, ContextMappingDSLPackage.Literals.INTER_AGGREGATE_INVARIANT__EXPRESSION);
		} else if (scopeType.equals(ScopeType.INTRA) && !areCompatibleTypes(type, BOOLEAN) && !areCompatibleTypes(type, FINAL)) {
			error(String.format(INVARIANT_EXPRESSION_MUST_BE_BOOLEAN_OR_FINAL, type), 
					intraAggregateInvariant, ContextMappingDSLPackage.Literals.INTRA_AGGREGATE_INVARIANT__EXPRESSION);
		}
	}
	
	private String dispatch(Expression expression, Aggregate scopeAggregate, Map<String, String> scopeVariables, ScopeType scopeType) {
		System.out.println(expression.getClass().getName());

		if (expression instanceof BooleanExpression) {
			BooleanExpression booleanExpression = (BooleanExpression) expression;
			return process(booleanExpression, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof Comparison) {
			Comparison comparison = (Comparison) expression;
			return process(comparison, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof Addition) {
			Addition addition = (Addition) expression;
			return process(addition, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof Multiplication) {
			Multiplication multiplication = (Multiplication) expression;
			return process(multiplication, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof BooleanNegation) {
			BooleanNegation booleanNegation = (BooleanNegation) expression;
			return process(booleanNegation, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof ArithmeticSigned) {
			ArithmeticSigned arithmeticSigned = (ArithmeticSigned) expression;
			return process(arithmeticSigned, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof TernaryExpression) {
			TernaryExpression ternaryExpression = (TernaryExpression) expression;
			return process(ternaryExpression, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof FinalExpression) {
			FinalExpression finalExpression = (FinalExpression) expression;
			return process(finalExpression, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof PathExpression) {
			PathExpression pathExpression = (PathExpression) expression;
			return process(pathExpression, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof ParenthesizedExpression) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
			return process(parenthesizedExpression, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof NumberLiteral) {
			NumberLiteral numberLiteral = (NumberLiteral) expression;
			return process(numberLiteral, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof NowLiteral) {
			NowLiteral nowLiteral = (NowLiteral) expression;
			return process(nowLiteral, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof StringLiteral) {
			StringLiteral stringLiteral = (StringLiteral) expression;
			return process(stringLiteral, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof NullLiteral) {
			NullLiteral nullLiteral = (NullLiteral) expression;
			return process(nullLiteral, scopeAggregate, scopeVariables, scopeType);
		} else if (expression instanceof BooleanLiteral) {
			BooleanLiteral booleanLiteral = (BooleanLiteral) expression;
			return process(booleanLiteral, scopeAggregate, scopeVariables, scopeType);
		} else {
			return ERROR;
		}
	}
	
	private String process(BooleanExpression booleanExpression, Aggregate scopeAggregate, Map<String, String> scopeVariables, ScopeType scopeType) {
		String result = BOOLEAN;
		
		String leftType = dispatch(booleanExpression.getLeft(), scopeAggregate, scopeVariables, scopeType);
		
		String rightType = null;
		if (booleanExpression.getRight() != null) {
			rightType = dispatch(booleanExpression.getRight(), scopeAggregate, scopeVariables, scopeType);
		} 
		
		if (!areCompatibleTypes(leftType, BOOLEAN)) {
			error(String.format(BOOLEAN_EXPRESSION_INCORRECT_TYPE, leftType), 
					booleanExpression, ContextMappingDSLPackage.Literals.BOOLEAN_EXPRESSION__LEFT);
			result = ERROR;
		}
		if (rightType != null && !areCompatibleTypes(rightType, BOOLEAN)) {
			error(String.format(BOOLEAN_EXPRESSION_INCORRECT_TYPE, rightType), 
					booleanExpression, ContextMappingDSLPackage.Literals.BOOLEAN_EXPRESSION__RIGHT);
			result = ERROR;
		}
		
		return result;
	}
	
	private String process(Comparison comparison, Aggregate scopeAggregate, Map<String, String> scopeVariables, ScopeType scopeType) {
		String result = BOOLEAN;
		
		String leftType = dispatch(comparison.getLeft(), scopeAggregate, scopeVariables, scopeType);
		
		String rightType = null;
		if (comparison.getRight() != null) {
			rightType = dispatch(comparison.getRight(), scopeAggregate, scopeVariables, scopeType);
		}
				
		if (rightType == null) {
			result = leftType;
		} else if (!areCompatibleTypes(leftType, rightType)) {
			error(String.format(COMPARISON_EXPRESSION_INCORRECT_TYPE, leftType + " <> " + rightType), 
					comparison, ContextMappingDSLPackage.Literals.COMPARISON__OP);
			result = ERROR;
		}

		return result;
	}

	
	private String process(Addition addition, Aggregate scopeAggregate, Map<String, String> scopeVariables, ScopeType scopeType) {
		String result = null;
		
		String leftType = dispatch(addition.getLeft(), scopeAggregate, scopeVariables, scopeType);
		
		String rightType = null;
		if (addition.getRight() != null) {
			rightType = dispatch(addition.getRight(), scopeAggregate, scopeVariables, scopeType);
		} 
		
		if (rightType == null) {
			result = leftType;
		} else if (!isNumeric(leftType)) {
			error(String.format(IS_NOT_NUMERIC_TYPE, leftType), 
					addition, ContextMappingDSLPackage.Literals.ADDITION__LEFT);
			result = ERROR;
		} else if (!isNumeric(rightType)) {
			error(String.format(IS_NOT_NUMERIC_TYPE, rightType), 
					addition, ContextMappingDSLPackage.Literals.ADDITION__RIGHT);
			result = ERROR;
		} else {
			result = leftType;
		}


		return result;
	}

	private String process(Multiplication multiplication, Aggregate scopeAggregate, Map<String, String> scopeVariables, ScopeType scopeType) {
		String result = null;
		
		String leftType = dispatch(multiplication.getLeft(), scopeAggregate, scopeVariables, scopeType);
	
		String rightType = null;
		if (multiplication.getRight() != null) {
			rightType = dispatch(multiplication.getRight(), scopeAggregate, scopeVariables, scopeType);
		} 
		
		if (rightType == null) {
			result = leftType;
		} else if (!isNumeric(leftType)) {
			error(String.format(IS_NOT_NUMERIC_TYPE, leftType), 
					multiplication, ContextMappingDSLPackage.Literals.MULTIPLICATION__LEFT);
		} else if (!isNumeric(rightType)) {
			error(String.format(IS_NOT_NUMERIC_TYPE, rightType), 
					multiplication, ContextMappingDSLPackage.Literals.MULTIPLICATION__RIGHT);
			result = ERROR;
		} else {
			result = leftType;
		}

		return result;
	}

	private String process(ArithmeticSigned arithmeticSigned, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {		
		String type = dispatch(arithmeticSigned.getAtomic(), scopeAggregate, scopeVariables, scopeType);
		
		if (!isNumeric(type)) {
			error(String.format(IS_NOT_NUMERIC_TYPE, type), 
					arithmeticSigned, ContextMappingDSLPackage.Literals.ARITHMETIC_SIGNED__ATOMIC);
			type = ERROR;
		}
			
		return type;
	}

	private String process(BooleanNegation booleanNegation, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		String type = dispatch(booleanNegation.getAtomic(), scopeAggregate, scopeVariables, scopeType);
		
		if (!areCompatibleTypes(type, BOOLEAN)) {
			error(String.format(BOOLEAN_EXPRESSION_INCORRECT_TYPE, type), 
					booleanNegation, ContextMappingDSLPackage.Literals.BOOLEAN_NEGATION__ATOMIC);
			type = ERROR;
		}
			
		return type;
	}
	
	private String process(NumberLiteral numberLiteral, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		String type = ERROR;
		
		if (numberLiteral.getValue().stripTrailingZeros().scale() <= 0) {
			type = INTEGER;
		} else {
			type = FLOAT;
		}
		
		return type;
	}
	
	private String process(NowLiteral nowLiteral, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		return DATE_TIME;
	}

	private String process(StringLiteral stringLiteral, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		return STRING;
	}
	
	private String process(NullLiteral nullLiteral, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		return NULL;
	}
	
	private String process(BooleanLiteral booBooleanLiteral, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		return BOOLEAN;
	}

	private String process(TernaryExpression ternaryExpression, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		String result = dispatch(ternaryExpression.getCondition(), scopeAggregate, scopeVariables, scopeType);
		if (!result.equals(BOOLEAN)) {
			error(String.format(TERNARY_EXPRESSION_CONDITION_MUST_BE_BOOLEAN, result), 
					ternaryExpression, ContextMappingDSLPackage.Literals.TERNARY_EXPRESSION__CONDITION);
			return ERROR;
		}
		result = dispatch(ternaryExpression.getTruevalue(), scopeAggregate, scopeVariables, scopeType);
		if (result.equals(ERROR)) {
			return ERROR;
		}
		result = dispatch(ternaryExpression.getFalsevalue(), scopeAggregate, scopeVariables, scopeType);
		if (result.equals(ERROR)) {
			return ERROR;
		}
		return result;
	}
	
	private String process(FinalExpression finalExpression, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		String result = FINAL;
		
		if (!scopeType.equals(ScopeType.INTRA)) {
			error(String.format(FINAL_EXPRESSION_ONLY_ALLOWED_IN_INTRA_INVARIANT), 
					finalExpression, ContextMappingDSLPackage.Literals.FINAL_EXPRESSION__FINAL_ELEMENTS);
		}
		
		Entity currentEntity = getAggregateEntityRoot(scopeAggregate);
		
		for (FinalElement finalElement: finalExpression.getFinalElements()) {
			String[] processPropertiesResult = new String[2];
			processPropertiesResult = processProperties(finalElement.getProperties(), currentEntity);
			if (processPropertiesResult[0].equals(ERROR)) {
				error(String.format(PROPERTY_IN_PATH_IS_NOT_CORRECT_ENTITY_PROPERTY, processPropertiesResult[1]), 
						finalElement, ContextMappingDSLPackage.Literals.FINAL_ELEMENT__PROPERTIES);
			}
			
			result = processPropertiesResult[0].equals(ERROR) || result.equals(ERROR) ? ERROR : result;
		}
		return result;
		
	}
	
	private String process(ParenthesizedExpression parenthesizedExpression, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		return dispatch(parenthesizedExpression.getExpression(), scopeAggregate, scopeVariables, scopeType);
	}
	
	private String process(PathExpression pathExpression, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		String result = ERROR;
		Entity currentEntity = null;
		String collectionType = null;
		
		// process head element
		if (pathExpression.getHeadElement().isRoot()) {
			if (scopeType.equals(ScopeType.INTER)) {
				error(String.format(INTER_INVARIANT_CANNOT_CONTAIN_ROOT), 
						pathExpression, ContextMappingDSLPackage.Literals.PATH_EXPRESSION__HEAD_ELEMENT);
				return result;
			} else {
				currentEntity = getAggregateEntityRoot(scopeAggregate);
			}
		} else if (pathExpression.getHeadElement().getQuery() != null) {
			Query query = pathExpression.getHeadElement().getQuery();
			if (scopeType.equals(ScopeType.INTRA)) {
				error(String.format(INTRA_INVARIANT_CANNOT_CONTAIN_QUERY, query.getRepositoryOperation().getName()), 
						query, ContextMappingDSLPackage.Literals.QUERY__REPOSITORY_OPERATION);
				return result;
			} else if (!validateQuery(scopeAggregate, scopeVariables, query)) {
				return result;
			}
			currentEntity = (Entity) query.getRepositoryOperation().getReturnType().getDomainObjectType();
		} else if (pathExpression.getHeadElement().getVar() != null) {
			String var = pathExpression.getHeadElement().getVar();
			if (scopeType.equals(ScopeType.INTRA)) {
				error(String.format(INTRA_INVARIANT_CANNOT_CONTAIN_VAR, var), 
						pathExpression, ContextMappingDSLPackage.Literals.PATH_EXPRESSION__HEAD_ELEMENT);
				return result;
			} else if (scopeType.equals(ScopeType.INTER)) {
				String type = scopeVariables.get(var);
				if (type == null) {
					error(String.format(INTER_INVARIANT_ATTRIBUTE_NOT_DECLARED_IN_ANTICORRUPTION_TRANSLATION, var), 
							pathExpression, ContextMappingDSLPackage.Literals.PATH_EXPRESSION__HEAD_ELEMENT);
					return result;
				} else {
					return type;
				}
			} else if (scopeType.equals(ScopeType.METHOD)) {
				String type = scopeVariables.get(var);
				
				System.out.println(var + ": " + type);
				
				if (type == null) {
					error(String.format(METHOD_LOCAL_VARIABLE_NOT_DECLARED, var), 
							pathExpression, ContextMappingDSLPackage.Literals.PATH_EXPRESSION__HEAD_ELEMENT);
					return result;
				}
				
				if (isCollectionType(type)) {
					collectionType = type;
					result = type;
				} else {
					currentEntity = getAggregateEntityByName(scopeAggregate, type);
					if (currentEntity == null && objectTypes.contains(type)) {
						return type;
					}
					
					if (currentEntity == null) {
						error(String.format(PATH_EXPRESSION_HEAD_MUST_BE_OBJECT, var), 
								pathExpression, ContextMappingDSLPackage.Literals.PATH_EXPRESSION__HEAD_ELEMENT);
						return result;
					}
				}
			}
		}
		
		if (pathExpression.getProperties().isEmpty() && collectionType == null) 
			return currentEntity.getName();
		
		if (!pathExpression.getProperties().isEmpty()) {
			String[] processPropertiesResult = new String[2];
			processPropertiesResult = processProperties(pathExpression.getProperties(), currentEntity);
			result = processPropertiesResult[0];
			if (result.equals(ERROR)) {
				error(String.format(PROPERTY_IN_PATH_IS_NOT_CORRECT_ENTITY_PROPERTY, processPropertiesResult[1]), 
						pathExpression, ContextMappingDSLPackage.Literals.PATH_EXPRESSION__PROPERTIES);
			}
		}
		
		if (pathExpression.getMethods().isEmpty()) return result;
		
		// process methods
		String currentType = result;
		for (Method method: pathExpression.getMethods()) {
			if (method instanceof SimpleMethod) {
				SimpleMethod simpleMethod = (SimpleMethod) method;
				currentType = processMethod(simpleMethod, currentType, scopeAggregate, scopeVariables, ScopeType.METHOD);
			} else if (method instanceof ParametricMethod) {
				ParametricMethod parametricMethod = (ParametricMethod) method;
				currentType = processMethod(parametricMethod, currentType, scopeAggregate, scopeVariables, ScopeType.METHOD);
			}
			
			if (currentType.equals(ERROR)) return ERROR;
		}
		result = currentType;
		
		pathExpression.getProperties().forEach(property -> {
			System.out.print(property.getName() + " " 
					+ property.getCollectionType().getName() + " " 
					+ property.getCollectionType().getValue());
			if (property instanceof Entity) System.out.println(" is entity");
			else if (property instanceof Attribute) System.out.println(" is attribute");
			else if (property instanceof Reference) System.out.println(" is reference of: " + ((Reference) property).getDomainObjectType().getName());
			else System.out.println(property.getClass().getName());
		});

		return result;
	}
	
	private boolean validateQuery(Aggregate scopeAggregate, Map<String, String> scopeVariables, Query query) {
		Repository repository = getAggregateEntityRoot(scopeAggregate).getRepository();
		if (repository == null) {
			error(String.format(QUERY_DOES_NOT_HAVE_ASSOCIATED_REPOSITORY, query.getRepositoryOperation().getName()), 
					query, ContextMappingDSLPackage.Literals.QUERY__REPOSITORY_OPERATION);
			return false;
		}
		
		RepositoryOperation operation = repository.getOperations().stream()
			.filter(op -> op.getName().equals(query.getRepositoryOperation().getName()))
			.findAny()
			.orElse(null);
		if (operation == null) { 
			error(String.format(QUERY_OPERATION_IS_NOT_DEFINED, query.getRepositoryOperation().getName()), 
					query, ContextMappingDSLPackage.Literals.QUERY__REPOSITORY_OPERATION);
			return false;
		}
		
		if (operation.getParameters().size() != query.getParams().size()) {
			error(String.format(NUMBER_QUERY_PARAMETERS_ARE_NOT_CONSISTENT, query.getRepositoryOperation().getName()), 
					query, ContextMappingDSLPackage.Literals.QUERY__REPOSITORY_OPERATION);
			return false;
		}
		
		for (String param: query.getParams()) {
			if (scopeVariables.get(param) == null) {
				error(String.format(QUERY_PARAM_IS_NOT_DECLARED, param), 
						query, ContextMappingDSLPackage.Literals.QUERY__PARAMS);
				return false;
			}
		}
		
		for (int i = 0; i < query.getParams().size(); i++) {
			
			if (!isSameType(scopeVariables.get(query.getParams().get(i)), operation.getParameters().get(i).getParameterType()) ) {
				error(String.format(QUERY_PARAM_TYPE_DOES_NOT_MATCH, query.getParams().get(i)), 
						query, ContextMappingDSLPackage.Literals.QUERY__PARAMS);
				return false;
			}
			
		}
		
		if (query.getRepositoryOperation().getReturnType().getDomainObjectType() == null 
				|| !(query.getRepositoryOperation().getReturnType().getDomainObjectType() instanceof Entity)) {
			error(String.format(QUERY_DOES_NOT_RETURN_ENTITY, query.getRepositoryOperation().getName()), 
					query, ContextMappingDSLPackage.Literals.QUERY__REPOSITORY_OPERATION);
			return false;	
		}
		
		Entity entity = (Entity) query.getRepositoryOperation().getReturnType().getDomainObjectType();
		if (!isAggregateEntity(scopeAggregate, entity)) {
			error(String.format(QUERY_RETURNED_ENTITY_DOES_NOT_BELONG_TO_AGGREGATE, entity.getName()), 
					query, ContextMappingDSLPackage.Literals.QUERY__REPOSITORY_OPERATION);
			return false;
		}
		
		return true;
	}

	private String[] processProperties(EList<Property> properties, Entity currentEntity) {
		boolean pathExpressionEnd = false;
		Property finalProperty = null;
		String[] result = new String[2];
		for (Property property: properties) {
			if (pathExpressionEnd) {
				result[0] = ERROR;
				result[1] = property.getName();
				return result;
			}
			
			if (currentEntity.getReferences().contains(property)) {
				if (property.getCollectionType().equals(CollectionType.NONE)) {
					currentEntity = (Entity) ((Reference) property).getDomainObjectType();
				} else {
					finalProperty = property;
					pathExpressionEnd = true;
				}
			} else if (getEntityAttributeByName(currentEntity,property.getName()) != null) {
				finalProperty = property;
				pathExpressionEnd = true;
			}
			else {
				result[0] = ERROR;
				result[1] = property.getName();
				return result;
			}
		}
		
		if (finalProperty instanceof Reference) {
			result[0] = finalProperty.getCollectionType().equals(CollectionType.NONE) ? "" : "COLLECTION$" ;
			result[0] = result[0] + ((Reference) finalProperty).getDomainObjectType().getName();
		} else {
			result[0] = getEntityAttributeByName(currentEntity,finalProperty.getName()).getType();
		}
		return result;
	}
	
	private String processMethod(ParametricMethod parametricMethod, String type, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		String result = ERROR;
		
		if (!isCollectionType(type)) {
			EAttribute feature = 
						parametricMethod.isAllMatch() ? ContextMappingDSLPackage.Literals.PARAMETRIC_METHOD__ALL_MATCH 
						: (parametricMethod.isFilter() ? ContextMappingDSLPackage.Literals.PARAMETRIC_METHOD__FILTER
						: ContextMappingDSLPackage.Literals.PARAMETRIC_METHOD__MAP);
			error(String.format(METHOD_REQUIRES_COLLECTION), parametricMethod, feature);
			return result;
		}
		
		if (scopeVariables.get(parametricMethod.getVariable()) != null) {
			error(String.format(VARIABLE_ALREADY_DECLARED_IN_SCOPE, parametricMethod.getVariable()), 
					parametricMethod, ContextMappingDSLPackage.Literals.PARAMETRIC_METHOD__VARIABLE);
			return result;
		} 
		
		scopeVariables.put(parametricMethod.getVariable(), getCollectionParameter(type));
		
		if (parametricMethod.isAllMatch() || parametricMethod.isAnyMatch() || parametricMethod.isNoneMatch()) {
			String expressionType = dispatch(parametricMethod.getBooleanExpression(), scopeAggregate, scopeVariables, scopeType);
			System.out.println("ALLMatch " + expressionType);
			if (!expressionType.equals(BOOLEAN)) {
				error(String.format(BOOLEAN_EXPRESSION_INCORRECT_TYPE, expressionType), 
						parametricMethod, ContextMappingDSLPackage.Literals.PARAMETRIC_METHOD__BOOLEAN_EXPRESSION);
			}
			result = BOOLEAN;
		} else if (parametricMethod.isFilter()) {
			String expressionType = dispatch(parametricMethod.getBooleanExpression(), scopeAggregate, scopeVariables, scopeType);
			System.out.println("Filter: " + expressionType);
			if (!expressionType.equals(BOOLEAN)) {
				error(String.format(BOOLEAN_EXPRESSION_INCORRECT_TYPE, expressionType), 
						parametricMethod, ContextMappingDSLPackage.Literals.PARAMETRIC_METHOD__BOOLEAN_EXPRESSION);
			}
			result = type;
		} else if (parametricMethod.isMap()) {
			String expressionType = dispatch(parametricMethod.getExpression(), scopeAggregate, scopeVariables, scopeType);
			result = "COLLECTION$" + expressionType;
		} else if (parametricMethod.isFlatMap()) {
			String expressionType = dispatch(parametricMethod.getExpression(), scopeAggregate, scopeVariables, scopeType);
			result = expressionType;
		}
		
		scopeVariables.remove(parametricMethod.getVariable());
		
		return result;
	}
	
	private String processMethod(SimpleMethod simpleMethod, String type, Aggregate scopeAggregate, Map<String, String> scopeVariables,
			ScopeType scopeType) {
		String result =ERROR;
		if (simpleMethod.isCount()) {
			if (isCollectionType(type)) {
				result = LONG;
			} else {
				error(String.format(METHOD_REQUIRES_COLLECTION), 
						simpleMethod, ContextMappingDSLPackage.Literals.SIMPLE_METHOD__COUNT);
			}
		} else if (simpleMethod.isDistinct()) {
			if (isCollectionType(type)) {
				result = type;
			} else {
				error(String.format(METHOD_REQUIRES_COLLECTION), 
						simpleMethod, ContextMappingDSLPackage.Literals.SIMPLE_METHOD__DISTINCT);
			}
		} else if (simpleMethod.isFindAny()) {
			if (isCollectionType(type)) {
				result = "OPTIONAL$" + getCollectionParameter(type);
			} else {
				error(String.format(METHOD_REQUIRES_COLLECTION), 
						simpleMethod, ContextMappingDSLPackage.Literals.SIMPLE_METHOD__FIND_ANY);
			}
		} else if (simpleMethod.isFindFirst()) {
			if (isCollectionType(type)) {
				result = "OPTIONAL$" + getCollectionParameter(type);
			} else {
				error(String.format(METHOD_REQUIRES_COLLECTION), 
						simpleMethod, ContextMappingDSLPackage.Literals.SIMPLE_METHOD__FIND_FIRST);
			}
		} else if (simpleMethod.isIsEmpty()) {
			if (isOptionalType(type)) {
				result = BOOLEAN;
			} else {
				error(String.format(METHOD_REQUIRES_OPTIONAL), 
						simpleMethod, ContextMappingDSLPackage.Literals.SIMPLE_METHOD__IS_EMPTY);
			}
		}  else if (simpleMethod.isGet()) {
			if (isOptionalType(type)) {
				result = getOptionalParameter(type);
			} else {
				error(String.format(METHOD_REQUIRES_OPTIONAL), 
						simpleMethod, ContextMappingDSLPackage.Literals.SIMPLE_METHOD__GET);
			}
		}
		return result;
	}
	
	private boolean isNumeric(String type) {
		Set<String> numericTypes = new HashSet<>(Arrays.asList(INTEGER,"Integer","long",LONG,BIG_DECIMAL,BIG_INTEGER,
			  	"double",DOUBLE,FLOAT,"Float"));
	
		return numericTypes.contains(type);
	}

	private boolean areCompatibleTypes(String typeOne, String typeTwo) {
		// To avoid propagation of error
		if (typeOne.equals(ERROR) || typeTwo.equals(ERROR)) {
			return true;
		}
		
		if (typeOne.toLowerCase().equals(typeTwo.toLowerCase())) {
			return true;
		}
		
		if (typeOne.equals(NULL) || typeTwo.equals(NULL)) {
			return objectTypes.contains(typeOne) || objectTypes.contains(typeTwo) || 
					isCollectionType(typeOne) || isCollectionType(typeTwo);
		}
		
		Set<String> numericTypes = new HashSet<>(Arrays.asList("int",INTEGER,"long",LONG,BIG_INTEGER,BIG_DECIMAL,"double",DOUBLE,FLOAT,"float"));
		if (numericTypes.contains(typeOne) && numericTypes.contains(typeTwo)) {
			return true;
		}
		
		return false;
	}	
	
	private boolean isSameType(String type, ComplexType parameterType) {
		if (parameterType.getType() == null)
			return false;
		
		return type.equals(parameterType.getType());
	}
	
	private boolean isOptionalType(String type) {
		return type.startsWith("OPTIONAL$");
	}
	
	private String getOptionalParameter(String type) {
		return type.substring("OPTIONAL$".length());
	}
	
	private boolean isCollectionType(String type) {
		return type.startsWith("COLLECTION$");
	}
	
	private String getCollectionParameter(String type) {
		return type.substring("COLLECTION$".length());
	}
	
	private Entity getAggregateEntityRoot(Aggregate aggregate) {
		return aggregate.getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.filter(Entity::isAggregateRoot)
				.findAny()
				.orElse(null);
	}

	private boolean isAggregateEntity(Aggregate aggregate, Entity entity) {
		return aggregate.getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.anyMatch(entity1 -> entity1.getName().equals(entity.getName()));
	}
	
	private Entity getAggregateEntityByName(Aggregate aggregate, String name) {
		return aggregate.getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.filter(entity1 -> entity1.getName().equals(name))
				.findAny()
				.orElse(null);
	}
	
	private Attribute getEntityAttributeByName(Entity entity, String name) {
		return entity.getAttributes().stream()
				.filter(attribute -> attribute.getName().equals(name))
				.findAny()
				.orElse(null);
	}
	
	private Set<String> getAggregateEntityNames(Aggregate aggregate) {
		return aggregate.getDomainObjects().stream()
				.filter(Entity.class::isInstance)
				.map(Entity.class::cast)
				.map(Entity::getName)
				.collect(Collectors.toSet());
	}

}