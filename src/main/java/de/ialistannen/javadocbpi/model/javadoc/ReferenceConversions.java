package de.ialistannen.javadocbpi.model.javadoc;

import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.FIELD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.METHOD;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.MODULE;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.PACKAGE;
import static de.ialistannen.javadocbpi.model.elements.DocumentedElementType.TYPE;

import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.ReferencePathElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference.StringPathElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtModuleReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;

public class ReferenceConversions {

  public static DocumentedElementReference getReference(CtReference reference) {
    return switch (reference) {
      case CtModuleReference ref -> getReference(ref.getDeclaration());
      case CtPackageReference ref -> getReference(ref.getDeclaration());
      case CtArrayTypeReference<?> ref -> getReference(ref);
      case CtTypeReference<?> ref -> getReference(ref.getTypeDeclaration());
      case CtExecutableReference<?> ref -> getReference(ref.getExecutableDeclaration());
      case CtFieldReference<?> ref -> getReference(ref.getFieldDeclaration());
      default -> throw new IllegalStateException("Unexpected value: " + reference);
    };
  }

  private static DocumentedElementReference getReference(CtArrayTypeReference<?> ref) {
    DocumentedElementReference arrayRef = getReference(ref.getArrayType());
    String arrayAppendix;
    if (ref.isParentInitialized()
        && ref.getParent() instanceof CtParameter<?> param
        && param.isVarArgs()) {
      arrayAppendix = "[]".repeat(ref.getDimensionCount() - 1);
      arrayAppendix += "...";
    } else {
      arrayAppendix = "[]".repeat(ref.getDimensionCount());
    }

    return new DocumentedElementReference(
        arrayRef.nullableParent(),
        new StringPathElement(arrayRef.segment().toString() + arrayAppendix),
        arrayRef.type()
    );
  }

  public static DocumentedElementReference getReference(CtModule module) {
    return DocumentedElementReference.root(MODULE, module.getSimpleName());
  }

  public static DocumentedElementReference getReference(CtPackage pack) {
    DocumentedElementReference parent;
    if (pack.getDeclaringPackage() == null || pack.getDeclaringPackage().isUnnamedPackage()) {
      if (pack.getDeclaringModule().isUnnamedModule()) {
        return DocumentedElementReference.root(PACKAGE, pack.getSimpleName());
      }

      parent = getReference(pack.getDeclaringModule());
    } else {
      parent = getReference(pack.getDeclaringPackage());
    }
    return parent.andThen(pack.getSimpleName(), PACKAGE);
  }

  public static DocumentedElementReference getReference(CtType<?> type) {
    if (type.getPackage() == null) {
      return DocumentedElementReference.root(TYPE, type.getSimpleName());
    }
    if (type.getDeclaringType() != null) {
      return getReference(type.getDeclaringType().getReference())
          .andThen(type.getSimpleName(), TYPE);
    }
    return getReference(type.getPackage()).andThen(type.getSimpleName(), TYPE);
  }

  public static DocumentedElementReference getReference(CtField<?> field) {
    return getReference(field.getDeclaringType()).andThen(field.getSimpleName(), FIELD);
  }

  public static DocumentedElementReference getReference(CtExecutable<?> executable) {
    DocumentedElementReference reference = getReference(executable.getParent(CtType.class))
        .andThen(executable.getSimpleName(), METHOD);

    for (CtParameter<?> parameter : executable.getParameters()) {
      reference = reference.andThen(
          new ReferencePathElement(getReference(parameter.getType())),
          TYPE
      );
    }
    return reference;
  }

  public static String renderTypeReference(CtTypeReference<?> reference) {
    String ref = reference.prettyprint();
    if (reference.isParentInitialized()
        && reference.getParent() instanceof CtParameter<?> param
        && param.isVarArgs()) {
      // cut off last dimension
      ref = ref.substring(0, ref.length() - 2);
      // add varargs marker
      ref += "...";
    }
    return ref;
  }

}
