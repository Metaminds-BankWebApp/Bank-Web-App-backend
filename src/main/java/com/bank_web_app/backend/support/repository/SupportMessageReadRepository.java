package com.bank_web_app.backend.support.repository;

import com.bank_web_app.backend.support.entity.SupportMessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportMessageReadRepository extends JpaRepository<SupportMessageRead, Long> {
	boolean existsByMessage_MessageIdAndReader_UserId(Long messageId, Long readerUserId);

	@Modifying
	@Query("delete from SupportMessageRead read where read.message.conversation.conversationId = :conversationId")
	void deleteAllByConversationId(@Param("conversationId") Long conversationId);
}
