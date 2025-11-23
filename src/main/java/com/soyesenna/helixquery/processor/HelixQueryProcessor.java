package com.soyesenna.helixquery.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.soyesenna.helixquery.annotations.GenerateFields;
import com.soyesenna.helixquery.annotations.IgnoreField;

/**
 * Annotation processor that generates XxxFields classes for JPA entities.
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
        String className = entitySimpleName + "Fields";
        String qRootType = qualifiedName(packageName, "Q" + entitySimpleName);

        StringBuilder builder = new StringBuilder();
        if (!packageName.isEmpty()) {
            builder.append("package ").append(packageName).append(";\n\n");
        }
        builder.append("public final class ").append(className).append(" {\n\n");
        builder.append("    private ").append(className).append("() {}\n\n");

        for (VariableElement field : collectFields(entity)) {
            String constant = renderFieldConstant(field, qRootType);
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
        return entity.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> (VariableElement) e)
                .filter(this::isProcessableField)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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

    private String renderFieldConstant(VariableElement field, String qRootType) {
        String fieldName = field.getSimpleName().toString();
        String constName = toUpperSnake(fieldName);
        TypeMirror type = field.asType();

        boolean isCollection = isCollection(type);
        boolean isRelation = isRelation(field);

        if (isCollection) {
            TypeMirror elementMirror = getCollectionElementType(type);
            String elementType = elementMirror != null ? boxedTypeName(elementMirror) : "java.lang.Object";
            String elementClassLiteral = elementMirror != null ? boxedErasure(elementMirror) : "java.lang.Object";
            String collectionType = "com.querydsl.core.types.dsl.CollectionExpressionBase<?, " + elementType + ">";
            return "    public static final com.soyesenna.helixquery.CollectionField<" + elementType + ", " + qRootType + ", " + collectionType + "> "
                    + constName + " = new com.soyesenna.helixquery.CollectionField<>(\""
                    + fieldName + "\", " + elementClassLiteral + ".class, q -> q." + fieldName + ");\n";
        }

        if (isRelation && generateRelations) {
            String targetType = boxedTypeName(type);
            String targetSimple = simpleName(type);
            String targetPkg = packageName(type);
            String targetQ = qualifiedName(targetPkg, "Q" + targetSimple);
            return "    public static final com.soyesenna.helixquery.RelationField<" + targetType + ", " + qRootType + ", " + targetQ + "> "
                    + constName + " = new com.soyesenna.helixquery.RelationField<>(\""
                    + fieldName + "\", " + boxedErasure(type) + ".class, q -> q." + fieldName + ");\n";
        }

        String valueType = boxedTypeName(type);
        return "    public static final com.soyesenna.helixquery.Field<" + valueType + ", " + qRootType + "> "
                + constName + " = new com.soyesenna.helixquery.Field<>(\""
                + fieldName + "\", " + boxedErasure(type) + ".class, q -> q." + fieldName + ");\n";
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

    private String simpleName(TypeMirror mirror) {
        Element element = types.asElement(mirror);
        if (element instanceof TypeElement typeElement) {
            return typeElement.getSimpleName().toString();
        }
        String text = mirror.toString();
        int lastDot = text.lastIndexOf('.');
        return lastDot >= 0 ? text.substring(lastDot + 1) : text;
    }

    private String packageName(TypeMirror mirror) {
        Element element = types.asElement(mirror);
        if (element instanceof TypeElement typeElement) {
            PackageElement pkg = elements.getPackageOf(typeElement);
            if (pkg != null && !pkg.isUnnamed()) {
                return pkg.getQualifiedName().toString();
            }
        }
        String text = mirror.toString();
        int lastDot = text.lastIndexOf('.');
        if (lastDot > 0) {
            return text.substring(0, lastDot);
        }
        return "";
    }
}
