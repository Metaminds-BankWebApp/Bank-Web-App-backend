package com.bank_web_app.backend.support.repository;

import com.bank_web_app.backend.support.entity.SupportMessage;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
	@EntityGraph(attributePaths = {"sender", "sender.role", "reads", "reads.reader", "reads.reader.role"})
	List<SupportMessage> findAllByConversation_ConversationIdOrderByCreatedAtAsc(Long conversationId);

	@Modifying
	@Query("delete from SupportMessage message where message.conversation.conversationId = :conversationId")
	void deleteAllByConversationId(@Param("conversationId") Long conversationId);
}
