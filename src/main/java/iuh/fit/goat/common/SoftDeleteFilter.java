package iuh.fit.goat.common;

import lombok.Getter;

@Getter
public enum SoftDeleteFilter {
    ACCOUNT("activeAccountFilter"),
    ROLE("activeRoleFilter"),
    PERMISSION("activePermissionFilter"),
    APPLICATION("activeApplicationFilter"),
    RESUME("activeResumeFilter"),
    SKILL("activeSkillFilter"),
    CAREER("activeCareerFilter"),
    SUBSCRIBER("activeSubscriberFilter"),
    INTERVIEW("activeInterviewFilter"),
    FRIENDSHIP("activeFriendshipFilter"),
    JOB("activeJobFilter"),
    BLOG("activeBlogFilter"),
    COMMENT("activeCommentFilter"),
    NOTIFICATION("activeNotificationFilter"),
    CHATROOM("activeChatRoomFilter"),
    CHATMEMBER("activeChatMemberFilter"),
    TICKET("activeTicketFilter"),
    REVIEW("activeReviewFilter");

    private final String value;

    SoftDeleteFilter(String value) {
        this.value = value;
    }
}
