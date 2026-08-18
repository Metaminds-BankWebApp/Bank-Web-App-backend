package com.bank_web_app.backend.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.notification.service.NotificationCommand;
import com.bank_web_app.backend.notification.service.NotificationCurrentUserService;
import com.bank_web_app.backend.notification.service.NotificationService;
import com.bank_web_app.backend.support.dto.request.SupportConversationCreateRequest;
import com.bank_web_app.backend.support.dto.response.SupportConversationDetailResponse;
import com.bank_web_app.backend.support.entity.SupportConversation;
import com.bank_web_app.backend.support.entity.SupportConversationStatus;
import com.bank_web_app.backend.support.entity.SupportMessage;
import com.bank_web_app.backend.support.repository.SupportConversationRepository;
import com.bank_web_app.backend.support.repository.SupportMessageReadRepository;
import com.bank_web_app.backend.support.repository.SupportMessageRepository;
import com.bank_web_app.backend.user.entity.Role;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportConversationServiceTest {

	@Mock private SupportConversationRepository conversationRepository;
	@Mock private SupportMessageRepository messageRepository;
	@Mock private SupportMessageReadRepository messageReadRepository;
	@Mock private UserRepository userRepository;
	@Mock private NotificationCurrentUserService currentUserService;
	@Mock private NotificationService notificationService;

	private SupportConversationService supportConversationService;

	@BeforeEach
	void setUp() {
		supportConversationService = new SupportConversationService(
			conversationRepository,
			messageRepository,
			messageReadRepository,
			userRepository,
			currentUserService,
			notificationService
		);
	}

	@Test
	void createsConversationAndNotifiesEveryActiveAdmin() {
		User customer = user(12L, "BANK_CUSTOMER", "Customer", "One", "ACTIVE");
		User activeAdmin = user(21L, "ADMIN", "Admin", "One", "ACTIVE");
		User inactiveAdmin = user(22L, "ADMIN", "Admin", "Two", "INACTIVE");
		AtomicReference<SupportMessage> savedMessage = new AtomicReference<>();

		when(currentUserService.resolveRequiredUser()).thenReturn(customer);
		when(conversationRepository.save(any(SupportConversation.class))).thenAnswer(invocation -> {
			SupportConversation conversation = invocation.getArgument(0);
			conversation.setConversationId(45L);
			return conversation;
		});
		when(messageRepository.save(any(SupportMessage.class))).thenAnswer(invocation -> {
			SupportMessage message = invocation.getArgument(0);
			message.setMessageId(99L);
			message.setCreatedAt(LocalDateTime.now());
			savedMessage.set(message);
			return message;
		});
		when(messageRepository.findAllByConversation_ConversationIdOrderByCreatedAtAsc(45L))
			.thenAnswer(invocation -> List.of(savedMessage.get()));
		when(userRepository.findAllByRole_RoleNameOrderByUpdatedAtDesc("ADMIN"))
			.thenReturn(List.of(activeAdmin, inactiveAdmin));

		SupportConversationDetailResponse result = supportConversationService.createConversation(
			new SupportConversationCreateRequest("Transact", "Transfer failed", "My transfer did not complete.")
		);

		ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
		verify(notificationService).createOrRefresh(command.capture());
		assertThat(result.conversationId()).isEqualTo(45L);
		assertThat(result.status()).isEqualTo(SupportConversationStatus.OPEN);
		assertThat(result.messages()).singleElement().satisfies(message ->
			assertThat(message.message()).isEqualTo("My transfer did not complete.")
		);
		assertThat(command.getValue().recipientUserId()).isEqualTo(21L);
		assertThat(command.getValue().actionMetadata()).containsEntry("conversationId", "45");
	}

	@Test
	void permanentlyDeletesConversationAndAllDependentHistoryThroughDatabaseCascade() {
		User admin = user(7L, "ADMIN", "Admin", "One", "ACTIVE");
		SupportConversation conversation = new SupportConversation();
		conversation.setConversationId(45L);
		conversation.setCreatedBy(user(12L, "BANK_CUSTOMER", "Customer", "One", "ACTIVE"));

		when(currentUserService.resolveRequiredUser()).thenReturn(admin);
		when(conversationRepository.findById(45L)).thenReturn(Optional.of(conversation));

		supportConversationService.permanentlyDeleteConversation(45L);

		verify(messageReadRepository).deleteAllByConversationId(45L);
		verify(messageRepository).deleteAllByConversationId(45L);
		verify(conversationRepository).delete(conversation);
	}

	private User user(Long id, String roleName, String firstName, String lastName, String status) {
		Role role = new Role();
		role.setRoleName(roleName);
		User user = new User();
		user.setUserId(id);
		user.setRole(role);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setUsername(firstName.toLowerCase() + "." + id);
		user.setEmail(firstName.toLowerCase() + id + "@example.test");
		user.setStatus(status);
		return user;
	}
}
