package com.caiotcruz.mygamelist.infra.security;
import java.lang.annotation.*;
import jakarta.validation.*;
@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=PasswordInput.Validator.class)
public @interface PasswordInput {
    String message() default "A senha precisa de 12 caracteres ou mais e no máximo 72 bytes UTF-8.";
    Class<?>[] groups() default {};Class<? extends Payload>[] payload() default {};
    boolean creation() default true;
    class Validator implements ConstraintValidator<PasswordInput,String> {
        private boolean creation;
        public void initialize(PasswordInput input){creation=input.creation();}
        public boolean isValid(String value,ConstraintValidatorContext context){return RequestLimits.validPassword(value,creation);}
    }
}
