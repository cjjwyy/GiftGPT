package com.giftgpt.recommendation.service;
import com.giftgpt.recommendation.dto.*;
import org.junit.jupiter.api.Test;
import javax.validation.*;
import static org.junit.jupiter.api.Assertions.*;
class OccasionRequestTest {
    @Test void calendarScenesAreAcceptedByBothRecommendationSteps() {
        try(ValidatorFactory factory=Validation.buildDefaultValidatorFactory()) {
            Validator v=factory.getValidator();
            for(String scene:new String[]{"daily","visit_patient","family_visit"}) {
                RecommendRequest r=new RecommendRequest();r.setOccasion(scene);
                MatchRequest m=new MatchRequest();m.setOccasion(scene);
                assertTrue(v.validateProperty(r,"occasion").isEmpty());
                assertTrue(v.validateProperty(m,"occasion").isEmpty());
            }
            RecommendRequest invalid=new RecommendRequest();invalid.setOccasion("invalid");
            assertFalse(v.validateProperty(invalid,"occasion").isEmpty());
        }
    }
}
