package com.giftgpt.content.service;
import com.giftgpt.content.entity.Story;
import com.giftgpt.content.dto.StoryView;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StoryViewTest {
    @Test void anonymousResponsesHideIdentityEvenFromOwnerWithoutMutatingEntity() {
        Story story = new Story(); story.setUserId(4L); story.setGiftRecordId(9L);
        story.setNickname("secret"); story.setIsAnonymous(1);
        StoryView view = StoryView.from(story,4L);
        assertNull(view.getUserId()); assertNull(view.getNickname()); assertTrue(view.isCanDelete());
        assertEquals(4L,story.getUserId()); assertFalse(StoryView.from(story,5L).isCanDelete());
    }
}
