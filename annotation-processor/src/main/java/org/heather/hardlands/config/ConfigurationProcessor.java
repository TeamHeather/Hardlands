package org.heather.hardlands.config;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedAnnotationTypes("org.heather.hardlands.config.ConfigBuilder")
public final class ConfigurationProcessor extends AbstractProcessor {

    private static final String DEFAULT_SUPERCLASS = "org.heather.hardlands.config.Configuration";
    private static final String LIST_TYPE = "java.util.List";
    private static final String SET_TYPE = "java.util.Set";
    private static final String MAP_TYPE = "java.util.Map";

    private final ValidatorFormatter validatorFormatter = new ValidatorFormatter();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(ConfigBuilder.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                this.reportError("@ConfigBuilder can only be applied to classes.", element);
                continue;
            }

            try {
                this.generate((TypeElement) element);
            } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
                this.reportError("Failed to generate configuration: " + exception.getMessage(), element);
            }
        }

        return true;
    }

    private void generate(TypeElement annotated) throws IOException {
        ConfigBuilder annotation = annotated.getAnnotation(ConfigBuilder.class);
        if (annotation == null) return;

        String packageName = this.processingEnv.getElementUtils()
                .getPackageOf(annotated)
                .getQualifiedName()
                .toString();

        String className = annotated.getSimpleName() + "Configuration";
        String qualifiedName = packageName + "." + className;
        String superclass = this.resolveSuperclass(annotation);

        try (Writer writer = this.processingEnv.getFiler()
                .createSourceFile(qualifiedName, annotated)
                .openWriter()) {

            this.writeHeader(writer, packageName, className, superclass);
            this.writeOptions(writer, annotated, annotation.options());
            this.writeConstructor(writer, className, annotation.identifier());

            writer.write("}\n");
        }
    }

    private void writeHeader(
            Writer writer,
            String packageName,
            String className,
            String superclass
    ) throws IOException {
        writer.write("""
                package %s;

                import org.heather.hardlands.config.Option;
                import org.heather.hardlands.config.Validator;

                public abstract class %s extends %s {

                """.formatted(packageName, className, superclass));
    }

    private void writeOptions(
            Writer writer,
            TypeElement annotated,
            OptionDef[] options
    ) throws IOException {
        Set<String> fieldNames = new HashSet<>();
        Set<String> keys = new HashSet<>();

        for (OptionDef option : options) {
            String fieldName = option.name();
            String key = toKebabCase(option.key().isBlank() ? fieldName : option.key());

            if (!this.validateOption(annotated, fieldNames, keys, fieldName, key)) continue;

            try {
                this.writeOption(writer, option, fieldName, key);
            } catch (IllegalArgumentException exception) {
                this.reportError(
                        "Invalid option '" + fieldName + "': " + exception.getMessage(),
                        annotated
                );
            }
        }
    }

    private void writeOption(
            Writer writer,
            OptionDef option,
            String fieldName,
            String key
    ) throws IOException {
        TypeMirror type = this.resolveType(option);

        if (this.isType(type, LIST_TYPE)) {
            this.writeCollectionOption(
                    writer,
                    option,
                    fieldName,
                    key,
                    LIST_TYPE,
                    "registerList"
            );
            return;
        }

        if (this.isType(type, SET_TYPE)) {
            this.writeCollectionOption(
                    writer,
                    option,
                    fieldName,
                    key,
                    SET_TYPE,
                    "registerSet"
            );
            return;
        }

        if (this.isType(type, MAP_TYPE)) {
            this.writeMapOption(writer, option, fieldName, key);
            return;
        }

        String typeName = this.referenceTypeName(type);
        String optionType = "Option<" + typeName + ">";
        String validators = this.validatorFormatter.format(typeName, option.validators());

        writer.write(
                "    public final %s %s = super.registerOption(\"%s\", %s.class%s);\n\n"
                        .formatted(optionType, fieldName, key, typeName, validators)
        );

        this.writeGetter(writer, fieldName, optionType);
    }

    private void writeCollectionOption(
            Writer writer,
            OptionDef option,
            String fieldName,
            String key,
            String collectionType,
            String registerMethod
    ) throws IOException {
        TypeMirror elementType = this.resolveElementType(option);

        if (this.isVoidType(elementType)) {
            throw new IllegalArgumentException(
                    this.simpleTypeName(collectionType) + " option requires elementType"
            );
        }

        String elementTypeName = this.referenceTypeName(elementType);
        String optionType = "Option<%s<%s>>".formatted(collectionType, elementTypeName);
        String validators = this.validatorFormatter.format(collectionType, option.validators());

        writer.write(
                "    public final %s %s = super.%s(\"%s\", %s.class%s);\n\n"
                        .formatted(
                                optionType,
                                fieldName,
                                registerMethod,
                                key,
                                elementTypeName,
                                validators
                        )
        );

        this.writeGetter(writer, fieldName, optionType);
    }

    private void writeMapOption(
            Writer writer,
            OptionDef option,
            String fieldName,
            String key
    ) throws IOException {
        TypeMirror keyType = this.resolveKeyType(option);
        TypeMirror valueType = this.resolveValueType(option);

        if (this.isVoidType(keyType)) {
            throw new IllegalArgumentException("Map option requires keyType");
        }

        if (this.isVoidType(valueType)) {
            throw new IllegalArgumentException("Map option requires valueType");
        }

        String keyTypeName = this.referenceTypeName(keyType);
        String valueTypeName = this.referenceTypeName(valueType);
        String optionType = "Option<java.util.Map<%s, %s>>"
                .formatted(keyTypeName, valueTypeName);
        String validators = this.validatorFormatter.format(MAP_TYPE, option.validators());

        writer.write(
                "    public final %s %s = super.registerMap(\"%s\", %s.class, %s.class%s);\n\n"
                        .formatted(
                                optionType,
                                fieldName,
                                key,
                                keyTypeName,
                                valueTypeName,
                                validators
                        )
        );

        this.writeGetter(writer, fieldName, optionType);
    }

    private void writeGetter(
            Writer writer,
            String fieldName,
            String optionType
    ) throws IOException {
        writer.write("""
                    public final %s get%sOption() {
                        return this.%s;
                    }

                """.formatted(
                optionType,
                capitalize(fieldName),
                fieldName
        ));
    }

    private void writeConstructor(
            Writer writer,
            String className,
            String identifier
    ) throws IOException {
        if (identifier.isBlank()) return;

        writer.write("""
                    protected %s() {
                        super(%s);
                    }

                """.formatted(className, quote(identifier)));
    }

    private boolean validateOption(
            TypeElement annotated,
            Set<String> fieldNames,
            Set<String> keys,
            String fieldName,
            String key
    ) {
        if (!SourceVersion.isIdentifier(fieldName) || SourceVersion.isKeyword(fieldName)) {
            this.reportError("Invalid option field name: " + fieldName, annotated);
            return false;
        }

        if (!fieldNames.add(fieldName)) {
            this.reportError("Duplicate option field name: " + fieldName, annotated);
            return false;
        }

        if (!keys.add(key)) {
            this.reportError("Duplicate option key: " + key, annotated);
            return false;
        }

        return true;
    }

    private String resolveSuperclass(ConfigBuilder annotation) {
        try {
            Class<?> type = annotation.superclass();
            return type == Void.class ? DEFAULT_SUPERCLASS : type.getCanonicalName();
        } catch (MirroredTypeException exception) {
            TypeMirror type = exception.getTypeMirror();
            return this.isVoidType(type) ? DEFAULT_SUPERCLASS : type.toString();
        }
    }

    private TypeMirror resolveType(OptionDef option) {
        try {
            return this.resolveClass(option.type());
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }

    private TypeMirror resolveElementType(OptionDef option) {
        try {
            return this.resolveClass(option.elementType());
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }

    private TypeMirror resolveKeyType(OptionDef option) {
        try {
            return this.resolveClass(option.keyType());
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }

    private TypeMirror resolveValueType(OptionDef option) {
        try {
            return this.resolveClass(option.valueType());
        } catch (MirroredTypeException exception) {
            return exception.getTypeMirror();
        }
    }

    private TypeMirror resolveClass(Class<?> type) {
        TypeElement element = this.processingEnv.getElementUtils()
                .getTypeElement(type.getCanonicalName());

        if (element == null) {
            throw new IllegalStateException("Unable to resolve type: " + type.getName());
        }

        return element.asType();
    }

    private boolean isType(TypeMirror type, String qualifiedName) {
        return this.processingEnv.getTypeUtils()
                .erasure(type)
                .toString()
                .equals(qualifiedName);
    }

    private boolean isVoidType(TypeMirror type) {
        return type.toString().equals(Void.class.getCanonicalName());
    }

    private String referenceTypeName(TypeMirror type) {
        String typeName = type.getKind().isPrimitive()
                ? this.processingEnv.getTypeUtils()
                .boxedClass((PrimitiveType) type)
                .getQualifiedName()
                .toString()
                : type.toString();

        return typeName.startsWith("java.lang.")
                ? typeName.substring("java.lang.".length())
                : typeName;
    }

    private String simpleTypeName(String typeName) {
        int separator = typeName.lastIndexOf('.');
        return separator < 0 ? typeName : typeName.substring(separator + 1);
    }

    private void reportError(String message, Element element) {
        this.processingEnv.getMessager()
                .printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String toKebabCase(String value) {
        return value
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
    }

    private static String quote(String value) {
        return '"' + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + '"';
    }
}