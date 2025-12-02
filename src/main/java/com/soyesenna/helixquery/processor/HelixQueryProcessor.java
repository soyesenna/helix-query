package com.soyesenna.helixquery.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.soyesenna.helixquery.annotations.GenerateFields;
import com.soyesenna.helixquery.annotations.IgnoreField;

/**
 * Annotation processor that generates XxxFields classes for JPA entities.
 * Generates type-specific field classes without QueryDSL Q-type dependencies.
 */
@SupportedAnnotationTypes({
        "com.soyesenna.helixquery.annotations.GenerateFields",
        "jakarta.persistence.Entity",
        "javax.persistence.Entity"
})
@SupportedOptions({"HelixQuery.generateRelations", "HelixQuery.includeTransient", "HelixQuery.relationDepth"})
public class HelixQueryProcessor extends AbstractProcessor {

    private static final Map<String, String> PRIMITIVE_BOXES = Map.of(
            "byte", Byte.class.getName(),
            "short", Short.class.getName(),
            "int", Integer.class.getName(),
            "long", Long.class.getName(),
            "float", Float.class.getName(),
            "double", Double.class.getName(),
            "boolean", Boolean.class.getName(),
            "char", Character.class.getName()
    );

    // Numeric types that should use NumberField
    private static final Set<String> NUMERIC_TYPES = Set.of(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
            "java.lang.Float", "java.lang.Double", "java.math.BigDecimal", "java.math.BigInteger"
    );

    // DateTime types that should use DateTimeField
    private static final Set<String> DATETIME_TYPES = Set.of(
            "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
            "java.time.OffsetDateTime", "java.time.ZonedDateTime", "java.time.Instant",
            "java.util.Date", "java.sql.Date", "java.sql.Time", "java.sql.Timestamp"
    );

    private Elements elements;
    private Types types;
    private Filer filer;
    private final Set<String> generated = new HashSet<>();
    private boolean generateRelations = true;
    private boolean includeTransient = false;
    private int relationDepth = 1;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();

        Map<String, String> options = processingEnv.getOptions();
        this.generateRelations = Boolean.parseBoolean(options.getOrDefault("HelixQuery.generateRelations", "true"));
        this.includeTransient = Boolean.parseBoolean(options.getOrDefault("HelixQuery.includeTransient", "false"));
        this.relationDepth = Integer.parseInt(options.getOrDefault("HelixQuery.relationDepth", "1"));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<TypeElement> candidates = new HashSet<>();

        collectCandidates(roundEnv, elements.getTypeElement("com.soyesenna.helixquery.annotations.GenerateFields"), candidates, true);
        collectCandidates(roundEnv, elements.getTypeElement("jakarta.persistence.Entity"), candidates, false);
        collectCandidates(roundEnv, elements.getTypeElement("javax.persistence.Entity"), candidates, false);

        for (TypeElement type : candidates) {
            try {
                generateFieldsClass(type);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate for " + type + ": " + e.getMessage());
            }
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void collectCandidates(RoundEnvironment roundEnv, TypeElement annotation, Set<TypeElement> out, boolean forceGenerate) {
        if (annotation == null) {
            return;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.RECORD) {
                continue;
            }
            TypeElement typeElement = (TypeElement) element;
            if (!isGenerationEnabled(typeElement, forceGenerate)) {
                continue;
            }
            out.add(typeElement);
        }
    }

    private boolean isGenerationEnabled(TypeElement typeElement, boolean forceGenerate) {
        GenerateFields generateFields = typeElement.getAnnotation(GenerateFields.class);
        if (generateFields != null) {
            return generateFields.value();
        }
        return forceGenerate || hasAnyAnnotation(typeElement, "jakarta.persistence.Entity", "javax.persistence.Entity");
    }

    private boolean hasAnyAnnotation(Element element, String... annotationNames) {
        for (var mirror : element.getAnnotationMirrors()) {
            String name = mirror.getAnnotationType().toString();
            for (String target : annotationNames) {
                if (target.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void generateFieldsClass(TypeElement entity) throws IOException {
        String packageName = getPackageName(entity);
        String entitySimpleName = entity.getSimpleName().toString();
        String entityFqcn = entity.getQualifiedName().toString();
        String className = entitySimpleName + "Fields";

        StringBuilder builder = new StringBuilder();
        if (!packageName.isEmpty()) {
            builder.append("package ").append(packageName).append(";\n\n");
        }

        // Generate imports
        builder.append("import com.soyesenna.helixquery.field.*;\n\n");

        builder.append("/**\n");
        builder.append(" * Generated field constants for {@link ").append(entityFqcn).append("}.\n");
        builder.append(" * Auto-generated by HelixQueryProcessor - do not modify.\n");
        builder.append(" */\n");
        builder.append("public final class ").append(className).append(" {\n\n");
        builder.append("    private ").append(className).append("() {}\n\n");

        for (VariableElement field : collectFields(entity)) {
            String constant = renderFieldConstant(field, entityFqcn);
            if (!constant.isEmpty()) {
                builder.append(constant).append("\n");
            }
        }

        builder.append("}\n");

        String fqcn = qualifiedName(packageName, className);
        if (!generated.add(fqcn)) {
            return;
        }
        JavaFileObject file = filer.createSourceFile(fqcn, entity);
        try (Writer writer = file.openWriter()) {
            writer.write(builder.toString());
        }
    }

    private String qualifiedName(String pkg, String simple) {
        if (pkg == null || pkg.isEmpty()) {
            return simple;
        }
        return pkg + "." + simple;
    }

    private String getPackageName(TypeElement type) {
        PackageElement pkg = elements.getPackageOf(type);
        if (pkg == null || pkg.isUnnamed()) {
            return "";
        }
        return pkg.getQualifiedName().toString();
    }

    private Set<VariableElement> collectFields(TypeElement entity) {
        Map<String, VariableElement> fieldsByName = new LinkedHashMap<>();
        for (TypeElement type : getTypeHierarchy(entity)) {
            type.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .map(e -> (VariableElement) e)
                    .filter(this::isProcessableField)
                    .forEach(field -> fieldsByName.put(field.getSimpleName().toString(), field));
        }
        return new LinkedHashSet<>(fieldsByName.values());
    }

    private boolean isProcessableField(VariableElement field) {
        if (field.getAnnotation(IgnoreField.class) != null) {
            return false;
        }
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.FINAL)) {
            return false;
        }
        if (modifiers.contains(Modifier.TRANSIENT)) {
            return false;
        }
        if (!includeTransient && hasAnyAnnotation(field, "jakarta.persistence.Transient", "javax.persistence.Transient")) {
            return false;
        }
        return true;
    }

    /**
     * Categorize field type for selecting appropriate Field class.
     */
    private enum FieldCategory {
        STRING,
        NUMBER,
        DATETIME,
        BOOLEAN,
        ENUM,
        COLLECTION,
        RELATION,
        EMBEDDED,
        COMPARABLE,
        SIMPLE
    }

    private FieldCategory categorizeField(TypeMirror type, VariableElement field) {
        String typeName = boxedErasure(type);

        // Check for collection first
        if (isCollection(type)) {
            return FieldCategory.COLLECTION;
        }

        // Check for embedded (before relation, as embedded is also an object reference)
        if (isEmbedded(field)) {
            return FieldCategory.EMBEDDED;
        }

        // Check for relation
        if (isRelation(field)) {
            return FieldCategory.RELATION;
        }

        // Check specific types
        if ("java.lang.String".equals(typeName)) {
            return FieldCategory.STRING;
        }

        if (NUMERIC_TYPES.contains(typeName)) {
            return FieldCategory.NUMBER;
        }

        if (DATETIME_TYPES.contains(typeName)) {
            return FieldCategory.DATETIME;
        }

        if ("java.lang.Boolean".equals(typeName) || "boolean".equals(type.toString())) {
            return FieldCategory.BOOLEAN;
        }

        // Check if enum
        if (isEnum(type)) {
            return FieldCategory.ENUM;
        }

        // Check if Comparable (for generic comparable support)
        if (isComparable(type)) {
            return FieldCategory.COMPARABLE;
        }

        return FieldCategory.SIMPLE;
    }

    private String renderFieldConstant(VariableElement field, String entityType) {
        String fieldName = field.getSimpleName().toString();
        String constName = toUpperSnake(fieldName);
        TypeMirror type = field.asType();

        FieldCategory category = categorizeField(type, field);

        return switch (category) {
            case STRING -> renderStringField(constName, fieldName, entityType);
            case NUMBER -> renderNumberField(constName, fieldName, type, entityType);
            case DATETIME -> renderDateTimeField(constName, fieldName, type, entityType);
            case BOOLEAN -> renderSimpleField(constName, fieldName, type, entityType);
            case ENUM -> renderComparableField(constName, fieldName, type, entityType);
            case COLLECTION -> renderCollectionField(constName, fieldName, type, entityType);
            case RELATION -> generateRelations ? renderRelationField(constName, fieldName, type, entityType) : "";
            case EMBEDDED -> renderEmbeddedFields(constName, fieldName, type, entityType);
            case COMPARABLE -> renderComparableField(constName, fieldName, type, entityType);
            case SIMPLE -> renderSimpleField(constName, fieldName, type, entityType);
        };
    }

    private String renderStringField(String constName, String fieldName, String entityType) {
        return String.format(
                "    public static final StringField %s = new StringField(\"%s\", %s.class);%n",
                constName, fieldName, entityType
        );
    }

    private String renderNumberField(String constName, String fieldName, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final NumberField<%s> %s = new NumberField<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, fieldName, boxedErasure(type), entityType
        );
    }

    private String renderDateTimeField(String constName, String fieldName, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final DateTimeField<%s> %s = new DateTimeField<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, fieldName, boxedErasure(type), entityType
        );
    }

    private String renderComparableField(String constName, String fieldName, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final ComparableField<%s> %s = new ComparableField<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, fieldName, boxedErasure(type), entityType
        );
    }

    private String renderSimpleField(String constName, String fieldName, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final Field<%s> %s = new Field<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, fieldName, boxedErasure(type), entityType
        );
    }

    private String renderCollectionField(String constName, String fieldName, TypeMirror type, String entityType) {
        TypeMirror elementMirror = getCollectionElementType(type);
        String elementType = elementMirror != null ? boxedTypeName(elementMirror) : "Object";
        String elementClassLiteral = elementMirror != null ? boxedErasure(elementMirror) : "Object";
        return String.format(
                "    public static final CollectionField<%s> %s = new CollectionField<>(\"%s\", %s.class, %s.class);%n",
                elementType, constName, fieldName, elementClassLiteral, entityType
        );
    }

    /**
     * Render a relation field as a nested static class.
     * This allows chained access like UserFields.DEPARTMENT.NAME
     *
     * @param constName  the constant name (e.g., "DEPARTMENT")
     * @param fieldName  the field name (e.g., "department")
     * @param type       the relation target type
     * @param entityType the owning entity type
     * @return the generated nested class as a string
     */
    private String renderRelationField(String constName, String fieldName, TypeMirror type, String entityType) {
        String targetType = boxedTypeName(type);

        Element typeElement = types.asElement(type);
        if (!(typeElement instanceof TypeElement targetEntity)) {
            // Fallback to simple RelationField if type cannot be resolved
            return String.format(
                    "    public static final RelationField<%s> %s = new RelationField<>(\"%s\", %s.class, %s.class);%n",
                    targetType, constName, fieldName, boxedErasure(type), entityType
            );
        }

        // Check if target is an entity
        if (!isEntity(targetEntity)) {
            return String.format(
                    "    public static final RelationField<%s> %s = new RelationField<>(\"%s\", %s.class, %s.class);%n",
                    targetType, constName, fieldName, boxedErasure(type), entityType
            );
        }

        StringBuilder result = new StringBuilder();

        // Generate nested static class
        result.append(String.format("    /**%n"));
        result.append(String.format("     * Nested field accessor for %s relation.%n", fieldName));
        result.append(String.format("     * Use $ for join operations, other fields for queries.%n"));
        result.append(String.format("     */%n"));
        result.append(String.format("    public static final class %s {%n", constName));
        result.append(String.format("        private %s() {}%n%n", constName));

        // Generate $ field for the RelationField itself (used for joins)
        result.append(String.format(
                "        /** RelationField for join operations */%n" +
                "        public static final RelationField<%s> $ = new RelationField<>(\"%s\", %s.class, %s.class);%n%n",
                targetType, fieldName, boxedErasure(type), entityType
        ));

        // Generate nested fields from the target entity
        String nestedFields = renderRelationNestedClassFields(fieldName, type, entityType, new HashSet<>(), 0);
        result.append(nestedFields);

        result.append("    }\n");

        return result.toString();
    }

    /**
     * Render fields inside a relation nested class.
     *
     * @param pathPrefix    the field path prefix (e.g., "department")
     * @param type          the type of the relation field (target entity type)
     * @param entityType    the owning entity type
     * @param visitedTypes  set of already visited type names to prevent circular references
     * @param currentDepth  current recursion depth
     * @return the generated field constants as a string
     */
    private String renderRelationNestedClassFields(String pathPrefix, TypeMirror type,
            String entityType, Set<String> visitedTypes, int currentDepth) {

        Element typeElement = types.asElement(type);
        if (!(typeElement instanceof TypeElement targetEntity)) {
            return "";
        }

        String targetTypeName = targetEntity.getQualifiedName().toString();

        // Prevent circular references
        if (visitedTypes.contains(targetTypeName)) {
            return "";
        }

        Set<String> newVisitedTypes = new HashSet<>(visitedTypes);
        newVisitedTypes.add(targetTypeName);

        StringBuilder result = new StringBuilder();

        // The relationPath is the path prefix itself (e.g., "department")
        // This is used for auto-join when using nested fields
        String relationPath = pathPrefix;

        // Collect fields from the target entity
        Set<VariableElement> targetFields = collectEntityFields(targetEntity);

        for (VariableElement targetField : targetFields) {
            String nestedFieldName = targetField.getSimpleName().toString();
            String nestedPath = pathPrefix + "." + nestedFieldName;
            String nestedConstName = toUpperSnake(nestedFieldName);
            TypeMirror nestedType = targetField.asType();

            FieldCategory nestedCategory = categorizeField(nestedType, targetField);

            String fieldConstant = switch (nestedCategory) {
                case STRING -> renderNestedStringField(nestedConstName, nestedPath, entityType, relationPath);
                case NUMBER -> renderNestedNumberField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case DATETIME -> renderNestedDateTimeField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case BOOLEAN -> renderNestedSimpleField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case ENUM -> renderNestedComparableField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case EMBEDDED -> renderNestedEmbeddedFields(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case COMPARABLE -> renderNestedComparableField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case RELATION -> {
                    // For nested relations, check depth limit
                    if (currentDepth + 1 < relationDepth) {
                        yield renderNestedRelationClass(nestedConstName, nestedPath, nestedType, entityType, newVisitedTypes, currentDepth + 1);
                    } else {
                        // Just generate a simple RelationField at max depth
                        String relTargetType = boxedTypeName(nestedType);
                        yield String.format(
                                "        public static final RelationField<%s> %s = new RelationField<>(\"%s\", %s.class, %s.class);%n",
                                relTargetType, nestedConstName, nestedPath, boxedErasure(nestedType), entityType
                        );
                    }
                }
                case COLLECTION, SIMPLE -> ""; // Skip collections and simple fields within relations
            };

            if (!fieldConstant.isEmpty()) {
                result.append(fieldConstant);
            }
        }

        return result.toString();
    }

    /**
     * Render a nested relation as a sub-class within a relation class.
     */
    private String renderNestedRelationClass(String constName, String pathPrefix, TypeMirror type,
            String entityType, Set<String> visitedTypes, int currentDepth) {

        String targetType = boxedTypeName(type);

        Element typeElement = types.asElement(type);
        if (!(typeElement instanceof TypeElement targetEntity)) {
            return String.format(
                    "        public static final RelationField<%s> %s = new RelationField<>(\"%s\", %s.class, %s.class);%n",
                    targetType, constName, pathPrefix, boxedErasure(type), entityType
            );
        }

        if (!isEntity(targetEntity)) {
            return String.format(
                    "        public static final RelationField<%s> %s = new RelationField<>(\"%s\", %s.class, %s.class);%n",
                    targetType, constName, pathPrefix, boxedErasure(type), entityType
            );
        }

        StringBuilder result = new StringBuilder();

        // Generate nested static class
        result.append(String.format("        public static final class %s {%n", constName));
        result.append(String.format("            private %s() {}%n%n", constName));

        // Generate $ field for the RelationField
        result.append(String.format(
                "            public static final RelationField<%s> $ = new RelationField<>(\"%s\", %s.class, %s.class);%n%n",
                targetType, pathPrefix, boxedErasure(type), entityType
        ));

        // Generate nested fields (with deeper indentation)
        String nestedFields = renderDeeperNestedClassFields(pathPrefix, type, entityType, visitedTypes, currentDepth);
        result.append(nestedFields);

        result.append("        }\n");

        return result.toString();
    }

    /**
     * Render fields inside a deeper nested relation class (with extra indentation).
     * The relationPath is the full path to this nested relation (e.g., "department.manager").
     */
    private String renderDeeperNestedClassFields(String pathPrefix, TypeMirror type,
            String entityType, Set<String> visitedTypes, int currentDepth) {

        Element typeElement = types.asElement(type);
        if (!(typeElement instanceof TypeElement targetEntity)) {
            return "";
        }

        String targetTypeName = targetEntity.getQualifiedName().toString();

        if (visitedTypes.contains(targetTypeName)) {
            return "";
        }

        Set<String> newVisitedTypes = new HashSet<>(visitedTypes);
        newVisitedTypes.add(targetTypeName);

        // The relationPath is the pathPrefix for deeper nested fields
        String relationPath = pathPrefix;

        StringBuilder result = new StringBuilder();
        Set<VariableElement> targetFields = collectEntityFields(targetEntity);

        for (VariableElement targetField : targetFields) {
            String nestedFieldName = targetField.getSimpleName().toString();
            String nestedPath = pathPrefix + "." + nestedFieldName;
            String nestedConstName = toUpperSnake(nestedFieldName);
            TypeMirror nestedType = targetField.asType();

            FieldCategory nestedCategory = categorizeField(nestedType, targetField);

            String fieldConstant = switch (nestedCategory) {
                case STRING -> String.format(
                        "            public static final StringField %s = new StringField(\"%s\", %s.class, \"%s\");%n",
                        nestedConstName, nestedPath, entityType, relationPath);
                case NUMBER -> {
                    String valueType = boxedTypeName(nestedType);
                    yield String.format(
                            "            public static final NumberField<%s> %s = new NumberField<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                            valueType, nestedConstName, nestedPath, boxedErasure(nestedType), entityType, relationPath);
                }
                case DATETIME -> {
                    String valueType = boxedTypeName(nestedType);
                    yield String.format(
                            "            public static final DateTimeField<%s> %s = new DateTimeField<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                            valueType, nestedConstName, nestedPath, boxedErasure(nestedType), entityType, relationPath);
                }
                case BOOLEAN -> {
                    String valueType = boxedTypeName(nestedType);
                    yield String.format(
                            "            public static final Field<%s> %s = new Field<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                            valueType, nestedConstName, nestedPath, boxedErasure(nestedType), entityType, relationPath);
                }
                case ENUM, COMPARABLE -> {
                    String valueType = boxedTypeName(nestedType);
                    yield String.format(
                            "            public static final ComparableField<%s> %s = new ComparableField<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                            valueType, nestedConstName, nestedPath, boxedErasure(nestedType), entityType, relationPath);
                }
                case RELATION -> {
                    String relTargetType = boxedTypeName(nestedType);
                    yield String.format(
                            "            public static final RelationField<%s> %s = new RelationField<>(\"%s\", %s.class, %s.class);%n",
                            relTargetType, nestedConstName, nestedPath, boxedErasure(nestedType), entityType);
                }
                case EMBEDDED, COLLECTION, SIMPLE -> "";
            };

            if (!fieldConstant.isEmpty()) {
                result.append(fieldConstant);
            }
        }

        return result.toString();
    }

    // ==================== Nested class field renderers (8-space indent) ====================

    private String renderNestedStringField(String constName, String path, String entityType, String relationPath) {
        return String.format(
                "        public static final StringField %s = new StringField(\"%s\", %s.class, \"%s\");%n",
                constName, path, entityType, relationPath
        );
    }

    private String renderNestedNumberField(String constName, String path, TypeMirror type, String entityType, String relationPath) {
        String valueType = boxedTypeName(type);
        return String.format(
                "        public static final NumberField<%s> %s = new NumberField<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                valueType, constName, path, boxedErasure(type), entityType, relationPath
        );
    }

    private String renderNestedDateTimeField(String constName, String path, TypeMirror type, String entityType, String relationPath) {
        String valueType = boxedTypeName(type);
        return String.format(
                "        public static final DateTimeField<%s> %s = new DateTimeField<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                valueType, constName, path, boxedErasure(type), entityType, relationPath
        );
    }

    private String renderNestedComparableField(String constName, String path, TypeMirror type, String entityType, String relationPath) {
        String valueType = boxedTypeName(type);
        return String.format(
                "        public static final ComparableField<%s> %s = new ComparableField<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                valueType, constName, path, boxedErasure(type), entityType, relationPath
        );
    }

    private String renderNestedSimpleField(String constName, String path, TypeMirror type, String entityType, String relationPath) {
        String valueType = boxedTypeName(type);
        return String.format(
                "        public static final Field<%s> %s = new Field<>(\"%s\", %s.class, %s.class, \"%s\");%n",
                valueType, constName, path, boxedErasure(type), entityType, relationPath
        );
    }

    /**
     * Render embedded fields inside a nested relation class.
     *
     * @param constPrefix   the constant name prefix
     * @param pathPrefix    the field path prefix
     * @param type          the embedded type
     * @param entityType    the owning entity type
     * @param relationPath  the relation path for auto-join
     */
    private String renderNestedEmbeddedFields(String constPrefix, String pathPrefix, TypeMirror type, String entityType, String relationPath) {
        Element typeElement = types.asElement(type);
        if (!(typeElement instanceof TypeElement embeddableType)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("        // Embedded fields from %s%n", embeddableType.getSimpleName()));

        Set<VariableElement> embeddedFields = collectEmbeddableFields(embeddableType);

        for (VariableElement embeddedField : embeddedFields) {
            String nestedFieldName = embeddedField.getSimpleName().toString();
            String nestedPath = pathPrefix + "." + nestedFieldName;
            String nestedConstName = constPrefix + "_" + toUpperSnake(nestedFieldName);
            TypeMirror nestedType = embeddedField.asType();

            FieldCategory nestedCategory = categorizeField(nestedType, embeddedField);

            String fieldConstant = switch (nestedCategory) {
                case STRING -> renderNestedStringField(nestedConstName, nestedPath, entityType, relationPath);
                case NUMBER -> renderNestedNumberField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case DATETIME -> renderNestedDateTimeField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case BOOLEAN -> renderNestedSimpleField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case ENUM, COMPARABLE -> renderNestedComparableField(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case EMBEDDED -> renderNestedEmbeddedFields(nestedConstName, nestedPath, nestedType, entityType, relationPath);
                case COLLECTION, RELATION, SIMPLE -> "";
            };

            if (!fieldConstant.isEmpty()) {
                result.append(fieldConstant);
            }
        }

        return result.toString();
    }

    /**
     * Check if a TypeElement is an entity (has @Entity annotation).
     */
    private boolean isEntity(TypeElement typeElement) {
        return hasAnyAnnotation(typeElement, "jakarta.persistence.Entity", "javax.persistence.Entity");
    }

    /**
     * Collect fields from an entity, excluding back-references and collections.
     * This is used for flattening relation fields.
     */
    private Set<VariableElement> collectEntityFields(TypeElement entity) {
        Map<String, VariableElement> fieldsByName = new LinkedHashMap<>();
        for (TypeElement type : getTypeHierarchy(entity)) {
            type.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .map(e -> (VariableElement) e)
                    .filter(this::isRelationNestedFieldProcessable)
                    .forEach(field -> fieldsByName.put(field.getSimpleName().toString(), field));
        }
        return new LinkedHashSet<>(fieldsByName.values());
    }

    /**
     * Check if a field from a related entity should be processed for flattening.
     * Excludes static, transient, collections, and back-reference fields.
     */
    private boolean isRelationNestedFieldProcessable(VariableElement field) {
        if (field.getAnnotation(IgnoreField.class) != null) {
            return false;
        }
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.FINAL)) {
            return false;
        }
        if (modifiers.contains(Modifier.TRANSIENT)) {
            return false;
        }
        if (!includeTransient && hasAnyAnnotation(field, "jakarta.persistence.Transient", "javax.persistence.Transient")) {
            return false;
        }
        // Skip OneToMany and ManyToMany collections (back-references)
        if (hasAnyAnnotation(field,
                "jakarta.persistence.OneToMany", "javax.persistence.OneToMany",
                "jakarta.persistence.ManyToMany", "javax.persistence.ManyToMany")) {
            return false;
        }
        return true;
    }

    /**
     * Render flattened fields for an @Embedded field.
     * Analyzes the Embeddable class and generates individual field constants
     * with nested path names (e.g., "address.street" -> ADDRESS_STREET).
     *
     * @param constPrefix  the constant name prefix (e.g., "ADDRESS")
     * @param pathPrefix   the field path prefix (e.g., "address")
     * @param type         the type of the embedded field
     * @param entityType   the owning entity type
     * @return the generated field constants as a string
     */
    private String renderEmbeddedFields(String constPrefix, String pathPrefix, TypeMirror type, String entityType) {
        Element typeElement = types.asElement(type);
        if (!(typeElement instanceof TypeElement embeddableType)) {
            // Fallback to simple field if type cannot be resolved
            return renderSimpleField(constPrefix, pathPrefix, type, entityType);
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("    // Embedded fields from %s%n", embeddableType.getSimpleName()));

        // Collect fields from the Embeddable class
        Set<VariableElement> embeddedFields = collectEmbeddableFields(embeddableType);

        for (VariableElement embeddedField : embeddedFields) {
            String nestedFieldName = embeddedField.getSimpleName().toString();
            String nestedPath = pathPrefix + "." + nestedFieldName;
            String nestedConstName = constPrefix + "_" + toUpperSnake(nestedFieldName);
            TypeMirror nestedType = embeddedField.asType();

            FieldCategory nestedCategory = categorizeField(nestedType, embeddedField);

            String fieldConstant = switch (nestedCategory) {
                case STRING -> renderStringFieldWithPath(nestedConstName, nestedPath, entityType);
                case NUMBER -> renderNumberFieldWithPath(nestedConstName, nestedPath, nestedType, entityType);
                case DATETIME -> renderDateTimeFieldWithPath(nestedConstName, nestedPath, nestedType, entityType);
                case BOOLEAN -> renderSimpleFieldWithPath(nestedConstName, nestedPath, nestedType, entityType);
                case ENUM -> renderComparableFieldWithPath(nestedConstName, nestedPath, nestedType, entityType);
                case EMBEDDED -> renderEmbeddedFields(nestedConstName, nestedPath, nestedType, entityType);
                case COMPARABLE -> renderComparableFieldWithPath(nestedConstName, nestedPath, nestedType, entityType);
                case COLLECTION, RELATION -> ""; // Skip relations and collections inside embedded
                case SIMPLE -> renderSimpleFieldWithPath(nestedConstName, nestedPath, nestedType, entityType);
            };

            if (!fieldConstant.isEmpty()) {
                result.append(fieldConstant);
            }
        }

        return result.toString();
    }

    /**
     * Collect fields from an Embeddable class.
     * Supports both regular classes and Java records.
     */
    private Set<VariableElement> collectEmbeddableFields(TypeElement embeddableType) {
        Map<String, VariableElement> fieldsByName = new LinkedHashMap<>();

        // Check if it's a record (Java 14+)
        if (embeddableType.getKind() == ElementKind.RECORD) {
            // For records, use record components which map to fields
            embeddableType.getRecordComponents().stream()
                    .forEach(component -> {
                        // Record components have corresponding fields with same name
                        String fieldName = component.getSimpleName().toString();
                        // Find the corresponding field
                        embeddableType.getEnclosedElements().stream()
                                .filter(e -> e.getKind() == ElementKind.FIELD)
                                .map(e -> (VariableElement) e)
                                .filter(f -> f.getSimpleName().toString().equals(fieldName))
                                .findFirst()
                                .ifPresent(field -> fieldsByName.put(fieldName, field));
                    });
        } else {
            // For regular classes, use getAllMembers
            elements.getAllMembers(embeddableType).stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .map(e -> (VariableElement) e)
                    .filter(this::isEmbeddableFieldProcessable)
                    .forEach(field -> fieldsByName.put(field.getSimpleName().toString(), field));
        }

        return new LinkedHashSet<>(fieldsByName.values());
    }

    /**
     * Check if a field from an Embeddable class should be processed.
     * More lenient than isProcessableField() - only checks for static.
     * Note: final is allowed because record fields are implicitly final.
     */
    private boolean isEmbeddableFieldProcessable(VariableElement field) {
        Set<Modifier> modifiers = field.getModifiers();
        // Skip static fields only (allow final for potential record-like patterns)
        if (modifiers.contains(Modifier.STATIC)) {
            return false;
        }
        // Skip synthetic or compiler-generated fields
        if (field.getSimpleName().toString().startsWith("$")) {
            return false;
        }
        // Skip fields inherited from Object
        Element enclosing = field.getEnclosingElement();
        if (enclosing instanceof TypeElement typeElement) {
            if ("java.lang.Object".equals(typeElement.getQualifiedName().toString())) {
                return false;
            }
        }
        return true;
    }

    // ==================== Field renderers with custom path support ====================

    private String renderStringFieldWithPath(String constName, String path, String entityType) {
        return String.format(
                "    public static final StringField %s = new StringField(\"%s\", %s.class);%n",
                constName, path, entityType
        );
    }

    private String renderNumberFieldWithPath(String constName, String path, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final NumberField<%s> %s = new NumberField<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, path, boxedErasure(type), entityType
        );
    }

    private String renderDateTimeFieldWithPath(String constName, String path, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final DateTimeField<%s> %s = new DateTimeField<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, path, boxedErasure(type), entityType
        );
    }

    private String renderComparableFieldWithPath(String constName, String path, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final ComparableField<%s> %s = new ComparableField<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, path, boxedErasure(type), entityType
        );
    }

    private String renderSimpleFieldWithPath(String constName, String path, TypeMirror type, String entityType) {
        String valueType = boxedTypeName(type);
        return String.format(
                "    public static final Field<%s> %s = new Field<>(\"%s\", %s.class, %s.class);%n",
                valueType, constName, path, boxedErasure(type), entityType
        );
    }

    private boolean isRelation(VariableElement field) {
        return hasAnyAnnotation(field,
                "jakarta.persistence.ManyToOne",
                "javax.persistence.ManyToOne",
                "jakarta.persistence.OneToOne",
                "javax.persistence.OneToOne");
    }

    private boolean isEmbedded(VariableElement field) {
        return hasAnyAnnotation(field,
                "jakarta.persistence.Embedded",
                "javax.persistence.Embedded");
    }

    private boolean isCollection(TypeMirror type) {
        TypeElement collectionType = elements.getTypeElement("java.util.Collection");
        if (collectionType == null) {
            return false;
        }
        return types.isAssignable(types.erasure(type), types.erasure(collectionType.asType()));
    }

    private boolean isEnum(TypeMirror type) {
        Element element = types.asElement(type);
        return element != null && element.getKind() == ElementKind.ENUM;
    }

    private boolean isComparable(TypeMirror type) {
        TypeElement comparableType = elements.getTypeElement("java.lang.Comparable");
        if (comparableType == null) {
            return false;
        }
        return types.isAssignable(types.erasure(type), types.erasure(comparableType.asType()));
    }

    private TypeMirror getCollectionElementType(TypeMirror type) {
        if (!(type instanceof DeclaredType declaredType)) {
            return null;
        }
        if (declaredType.getTypeArguments().isEmpty()) {
            return null;
        }
        return declaredType.getTypeArguments().get(0);
    }

    private String boxedTypeName(TypeMirror mirror) {
        String raw = mirror.toString();
        return PRIMITIVE_BOXES.getOrDefault(raw, raw);
    }

    private String boxedErasure(TypeMirror mirror) {
        String erased = types.erasure(mirror).toString();
        return PRIMITIVE_BOXES.getOrDefault(erased, erased);
    }

    private String toUpperSnake(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    private List<TypeElement> getTypeHierarchy(TypeElement entity) {
        List<TypeElement> hierarchy = new ArrayList<>();
        TypeElement current = entity;
        while (current != null && !isJavaLangObject(current)) {
            hierarchy.add(current);
            TypeMirror superType = current.getSuperclass();
            if (superType == null || superType.getKind() == TypeKind.NONE) {
                break;
            }
            Element superElement = types.asElement(superType);
            if (superElement instanceof TypeElement superTypeElement) {
                current = superTypeElement;
            } else {
                break;
            }
        }
        Collections.reverse(hierarchy);
        return hierarchy;
    }

    private boolean isJavaLangObject(TypeElement typeElement) {
        return "java.lang.Object".equals(typeElement.getQualifiedName().toString());
    }
}
