package annotations;
import java.lang.annotation.*;

public class TypeValidationAnnotation {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotNull {
        String value() default "The fied cannot be null";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Min {
        int value();
        String message() default "The field value is less than the minimum allowed";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Max {
        int value();
        String message() default "The field value exceeds the maximum allowed";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Pattern {
        String regex();
        String message() default "The field does not match the required pattern";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotEmpty {
        String message() default "The field cannot be empty";
    }
    
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Positive {
        String message() default "The field value must be positive";
    }

}




