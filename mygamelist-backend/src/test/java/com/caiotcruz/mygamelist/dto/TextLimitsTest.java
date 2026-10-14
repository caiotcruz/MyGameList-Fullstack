package com.caiotcruz.mygamelist.dto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import com.caiotcruz.mygamelist.model.enums.ThemePreference;
import static org.junit.jupiter.api.Assertions.*;
class TextLimitsTest {
 @Test void reviewAcceptsBoundaryAndRejectsOverflowIncludingSurrogatePairs(){
  try(var factory=Validation.buildDefaultValidatorFactory()){
   var validator=factory.getValidator();
   String review="🎮".repeat(TextLimits.REVIEW/2);
   assertTrue(validator.validate(new AddGameDTO(1L,null,null,review,null,null)).isEmpty());
   assertFalse(validator.validate(new AddGameDTO(1L,null,null,review+"a",null,null)).isEmpty());
  }
 }
 @Test void profileCommentAndMessageBoundariesAreConsistent(){
  try(var factory=Validation.buildDefaultValidatorFactory()){
   var validator=factory.getValidator();
   assertTrue(validator.validate(new UserProfileDTO("N".repeat(TextLimits.NAME),"b".repeat(TextLimits.BIO),false,null,ThemePreference.BLUE)).isEmpty());
   assertFalse(validator.validate(new UserProfileDTO("Valid","b".repeat(TextLimits.BIO+1),false,null,ThemePreference.BLUE)).isEmpty());
   assertTrue(validator.validate(new CreateCommentDTO("c".repeat(TextLimits.COMMENT))).isEmpty());
   assertFalse(validator.validate(new CreateCommentDTO("c".repeat(TextLimits.COMMENT+1))).isEmpty());
   assertTrue(validator.validate(new SendMessageDTO("m".repeat(TextLimits.MESSAGE))).isEmpty());
   assertFalse(validator.validate(new SendMessageDTO("m".repeat(TextLimits.MESSAGE+1))).isEmpty());
  }
 }
}
