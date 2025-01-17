package io.leangen.graphql.metadata;

import graphql.language.OperationDefinition;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Operation {

    private final String name;
    private final String description;
    private final String deprecationReason;
    private final TypedElement typedElement;
    private final Type contextType;
    private final Map<String, Resolver> resolversByFingerprint;
    private final List<OperationArgument> arguments;
    private final OperationDefinition.Operation operationType;

    public Operation(String name, AnnotatedType javaType, Type contextType, List<OperationArgument> arguments,
                     List<Resolver> resolvers, OperationDefinition.Operation operationType) {
        
        this.name = name;
        this.description = resolvers.stream().map(Resolver::getOperationDescription).filter(Utils::isNotEmpty).findFirst().orElse(null);
        this.deprecationReason = resolvers.stream().map(Resolver::getOperationDeprecationReason).filter(Objects::nonNull).findFirst().orElse(null);
        this.typedElement = new TypedElement(javaType, resolvers.stream()
                .flatMap(resolver -> resolver.getTypedElement().getElements().stream())
                .distinct().collect(Collectors.toList()));
        this.contextType = contextType;
        this.resolversByFingerprint = collectResolversByFingerprint(resolvers);
        this.arguments = arguments;
        this.operationType = operationType;
    }

    private Map<String, Resolver> collectResolversByFingerprint(List<Resolver> resolvers) {
        Map<String, Resolver> resolversByFingerprint = new HashMap<>();
        resolvers.forEach(resolver -> resolversByFingerprint.putIfAbsent(resolver.getFingerprint(), resolver));
        return resolversByFingerprint;
    }

    public Resolver getApplicableResolver(Set<String> argumentNames) {
        if (resolversByFingerprint.size() == 1) {
            return getResolvers().iterator().next();
        } else {
            return resolversByFingerprint.get(getFingerprint(argumentNames));
        }
    }

    public Resolver getResolver(String... argumentNames) {
        return resolversByFingerprint.get(getFingerprint(new HashSet<>(Arrays.asList(argumentNames))));
    }

    public boolean isEmbeddableForType(Type type) {
        return contextType != null && GenericTypeReflector.isSuperType(contextType, type);
    }

    public boolean isRoot() {
        return this.contextType == null;
    }
    
    private String getFingerprint(Set<String> argumentNames) {
        StringBuilder fingerPrint = new StringBuilder();
        argumentNames.stream().sorted().forEach(fingerPrint::append);
        return fingerPrint.toString();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public AnnotatedType getJavaType() {
        return typedElement.getJavaType();
    }

    public List<OperationArgument> getArguments() {
        return arguments;
    }

    public Collection<Resolver> getResolvers() {
        return resolversByFingerprint.values();
    }

    public OperationDefinition.Operation getOperationType() {
        return operationType;
    }

    public TypedElement getTypedElement() {
        return typedElement;
    }

    @Override
    public String toString() {
        return name + "(" + arguments.stream().map(OperationArgument::getName).collect(Collectors.joining(",")) + ")";
    }
}
