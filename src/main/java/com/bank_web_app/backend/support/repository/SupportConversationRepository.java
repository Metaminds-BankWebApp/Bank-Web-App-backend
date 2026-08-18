package com.bank_web_app.backend.support.repository;

import com.bank_web_app.backend.support.entity.SupportConversation;
import com.bank_web_app.backend.support.entity.SupportConversationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, Long> {
	List<SupportConversation> findAllByCreatedBy_UserIdOrderByLastMessageAtDesc(Long userId);

	List<SupportConversation> findAllByOrderByLastMessageAtDesc();

	List<SupportConversation> findAllByStatusAndLastMessageAtBefore(
		SupportConversationStatus status,
		LocalDateTime lastMessageAt
	);
}
