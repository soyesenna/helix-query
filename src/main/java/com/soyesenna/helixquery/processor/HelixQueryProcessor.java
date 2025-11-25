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
@SupportedOptions({"HelixQuery.generateRelations", "HelixQuery.includeTransient"})
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

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();

        Map<String, String> options = processingEnv.getOptions();
        this.generateRelations = Boolean.parseBoolean(options.getOrDefault("HelixQuery.generateRelations", "true"));
        this.includeTransient = Boolean.parseBoolean(options.getOrDefault("HelixQuery.includeTransient", "false"));
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
        COMPARABLE,
        SIMPLE
    }

    private FieldCategory categorizeField(TypeMirror type, VariableElement field) {
        String typeName = boxedErasure(type);

        // Check for collection first
        if (isCollection(type)) {
            return FieldCategory.COLLECTION;
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

    private String renderRelationField(String constName, String fieldName, TypeMirror type, String entityType) {
        String targetType = boxedTypeName(type);
        return String.format(
                "    public static final RelationField<%s> %s = new RelationField<>(\"%s\", %s.class, %s.class);%n",
                targetType, constName, fieldName, boxedErasure(type), entityType
        );
    }

    private boolean isRelation(VariableElement field) {
        return hasAnyAnnotation(field,
                "jakarta.persistence.ManyToOne",
                "javax.persistence.ManyToOne",
                "jakarta.persistence.OneToOne",
                "javax.persistence.OneToOne");
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
