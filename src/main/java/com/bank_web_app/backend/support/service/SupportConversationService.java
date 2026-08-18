package com.bank_web_app.backend.support.service;

import com.bank_web_app.backend.notification.entity.NotificationSeverity;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.entity.NotificationType;
import com.bank_web_app.backend.notification.service.NotificationCommand;
import com.bank_web_app.backend.notification.service.NotificationCurrentUserService;
import com.bank_web_app.backend.notification.service.NotificationService;
import com.bank_web_app.backend.support.dto.request.SupportConversationCreateRequest;
import com.bank_web_app.backend.support.dto.request.SupportConversationStatusUpdateRequest;
import com.bank_web_app.backend.support.dto.request.SupportMessageCreateRequest;
import com.bank_web_app.backend.support.dto.response.SupportConversationDetailResponse;
import com.bank_web_app.backend.support.dto.response.SupportConversationSummaryResponse;
import com.bank_web_app.backend.support.dto.response.SupportMessageResponse;
import com.bank_web_app.backend.support.dto.response.SupportUserResponse;
import com.bank_web_app.backend.support.entity.SupportConversation;
import com.bank_web_app.backend.support.entity.SupportConversationStatus;
import com.bank_web_app.backend.support.entity.SupportMessage;
import com.bank_web_app.backend.support.entity.SupportMessageRead;
import com.bank_web_app.backend.support.repository.SupportConversationRepository;
import com.bank_web_app.backend.support.repository.SupportMessageReadRepository;
import com.bank_web_app.backend.support.repository.SupportMessageRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportConversationService {

	private static final String ADMIN_ROLE = "ADMIN";
	private static final int PREVIEW_MAX_LENGTH = 500;

	private final SupportConversationRepository conversationRepository;
	private final SupportMessageRepository messageRepository;
	private final SupportMessageReadRepository messageReadRepository;
	private final UserRepository userRepository;
	private final NotificationCurrentUserService currentUserService;
	private final NotificationService notificationService;

	public SupportConversationService(
		SupportConversationRepository conversationRepository,
		SupportMessageRepository messageRepository,
		SupportMessageReadRepository messageReadRepository,
		UserRepository userRepository,
		NotificationCurrentUserService currentUserService,
		NotificationService notificationService
	) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.messageReadRepository = messageReadRepository;
		this.userRepository = userRepository;
		this.currentUserService = currentUserService;
		this.notificationService = notificationService;
	}

	@Transactional
	public SupportConversationDetailResponse createConversation(SupportConversationCreateRequest request) {
		User creator = currentUserService.resolveRequiredUser();
		ensureSupportRequester(creator);
		LocalDateTime now = LocalDateTime.now();

		SupportConversation conversation = new SupportConversation();
		conversation.setCreatedBy(creator);
		conversation.setCategory(normalizeRequired(request.category()));
		conversation.setSubject(normalizeRequired(request.subject()));
		conversation.setStatus(SupportConversationStatus.OPEN);
		conversation.setLastMessageAt(now);
		conversation.setLastMessagePreview(toPreview(request.message()));
		conversation = conversationRepository.save(conversation);

		createMessage(conversation, creator, request.message());
		notifyAdmins(conversation, creator, true);
		return toDetail(conversation, creator, messageRepository.findAllByConversation_ConversationIdOrderByCreatedAtAsc(conversation.getConversationId()));
	}

	@Transactional(readOnly = true)
	public List<SupportConversationSummaryResponse> getMyConversations() {
		User currentUser = currentUserService.resolveRequiredUser();
		ensureSupportRequester(currentUser);
		return conversationRepository
			.findAllByCreatedBy_UserIdOrderByLastMessageAtDesc(currentUser.getUserId())
			.stream()
			.map(conversation -> toSummary(conversation, currentUser))
			.toList();
	}

	@Transactional
	public SupportConversationDetailResponse getConversation(Long conversationId) {
		User currentUser = currentUserService.resolveRequiredUser();
		SupportConversation conversation = findConversation(conversationId);
		ensureCanAccess(conversation, currentUser);
		List<SupportMessage> messages = messageRepository.findAllByConversation_ConversationIdOrderByCreatedAtAsc(conversationId);
		markAddressedMessagesRead(messages, currentUser);
		return toDetail(conversation, currentUser, messages);
	}

	@Transactional
	public SupportConversationDetailResponse sendMessage(Long conversationId, SupportMessageCreateRequest request) {
		User sender = currentUserService.resolveRequiredUser();
		SupportConversation conversation = findConversation(conversationId);
		ensureCanAccess(conversation, sender);

		createMessage(conversation, sender, request.message());
		if (isAdmin(sender)) {
			conversation.setStatus(SupportConversationStatus.IN_PROGRESS);
			notifyConversationCreator(conversation, sender);
		} else {
			conversation.setStatus(SupportConversationStatus.OPEN);
			notifyAdmins(conversation, sender, false);
		}
		conversation.setClosedAt(null);
		conversationRepository.save(conversation);

		List<SupportMessage> messages = messageRepository.findAllByConversation_ConversationIdOrderByCreatedAtAsc(conversationId);
		return toDetail(conversation, sender, messages);
	}

	@Transactional
	public void markConversationRead(Long conversationId) {
		User currentUser = currentUserService.resolveRequiredUser();
		SupportConversation conversation = findConversation(conversationId);
		ensureCanAccess(conversation, currentUser);
		markAddressedMessagesRead(
			messageRepository.findAllByConversation_ConversationIdOrderByCreatedAtAsc(conversationId),
			currentUser
		);
	}

	@Transactional(readOnly = true)
	public List<SupportConversationSummaryResponse> getAllConversations() {
		User admin = currentUserService.resolveRequiredUser();
		ensureAdmin(admin);
		return conversationRepository
			.findAllByOrderByLastMessageAtDesc()
			.stream()
			.map(conversation -> toSummary(conversation, admin))
			.toList();
	}

	@Transactional
	public SupportConversationDetailResponse updateStatus(
		Long conversationId,
		SupportConversationStatusUpdateRequest request
	) {
		User admin = currentUserService.resolveRequiredUser();
		ensureAdmin(admin);
		SupportConversation conversation = findConversation(conversationId);
		conversation.setStatus(request.status());
		LocalDateTime now = LocalDateTime.now();
		conversation.setClosedAt(request.status() == SupportConversationStatus.CLOSED ? now : null);
		conversation.setLastMessageAt(now);
		conversationRepository.save(conversation);
		return toDetail(
			conversation,
			admin,
			messageRepository.findAllByConversation_ConversationIdOrderByCreatedAtAsc(conversationId)
		);
	}

	@Transactional
	public void permanentlyDeleteConversation(Long conversationId) {
		User admin = currentUserService.resolveRequiredUser();
		ensureAdmin(admin);
		SupportConversation conversation = findConversation(conversationId);
		deleteConversationHistory(conversation);
	}

	@Transactional
	public int permanentlyDeleteClosedInactiveConversationsBefore(LocalDateTime cutoff) {
		if (cutoff == null) {
			return 0;
		}
		List<SupportConversation> expired = conversationRepository.findAllByStatusAndLastMessageAtBefore(
			SupportConversationStatus.CLOSED,
			cutoff
		);
		expired.forEach(this::deleteConversationHistory);
		return expired.size();
	}

	private void deleteConversationHistory(SupportConversation conversation) {
		Long conversationId = conversation.getConversationId();
		messageReadRepository.deleteAllByConversationId(conversationId);
		messageRepository.deleteAllByConversationId(conversationId);
		conversationRepository.delete(conversation);
	}

	private void createMessage(SupportConversation conversation, User sender, String rawMessage) {
		String messageText = normalizeRequired(rawMessage);
		SupportMessage message = new SupportMessage();
		message.setConversation(conversation);
		message.setSender(sender);
		message.setMessageText(messageText);
		messageRepository.save(message);
		conversation.setLastMessageAt(message.getCreatedAt());
		conversation.setLastMessagePreview(toPreview(messageText));
	}

	private void markAddressedMessagesRead(List<SupportMessage> messages, User currentUser) {
		for (SupportMessage message : messages) {
			if (!isAddressedTo(message, currentUser) || hasReadReceipt(message, currentUser.getUserId())) {
				continue;
			}
			SupportMessageRead receipt = new SupportMessageRead();
			receipt.setMessage(message);
			receipt.setReader(currentUser);
			messageReadRepository.save(receipt);
			message.getReads().add(receipt);
		}
	}

	private void notifyAdmins(SupportConversation conversation, User sender, boolean isNewConversation) {
		String title = isNewConversation ? "New support request" : "New customer support reply";
		String text = isNewConversation
			? conversation.getCreatedBy().getEmail() + " opened: " + conversation.getSubject()
			: conversation.getCreatedBy().getEmail() + " replied in: " + conversation.getSubject();
		for (User admin : userRepository.findAllByRole_RoleNameOrderByUpdatedAtDesc(ADMIN_ROLE)) {
			if (!isActive(admin) || admin.getUserId().equals(sender.getUserId())) {
				continue;
			}
			notificationService.createOrRefresh(new NotificationCommand(
				admin.getUserId(),
				NotificationType.SUPPORT_MESSAGE,
				NotificationSource.SUPPORT,
				NotificationSeverity.INFO,
				title,
				text,
				"SUPPORT_CONVERSATION",
				Map.of(
					"conversationId", conversation.getConversationId().toString(),
					"category", conversation.getCategory()
				),
				"support-conversation:" + conversation.getConversationId() + ":admin:" + admin.getUserId(),
				true
			));
		}
	}

	private void notifyConversationCreator(SupportConversation conversation, User sender) {
		User creator = conversation.getCreatedBy();
		if (!isActive(creator) || creator.getUserId().equals(sender.getUserId())) {
			return;
		}
		notificationService.createOrRefresh(new NotificationCommand(
			creator.getUserId(),
			NotificationType.SUPPORT_MESSAGE,
			NotificationSource.SUPPORT,
			NotificationSeverity.INFO,
			"Support replied",
			"An administrator replied to: " + conversation.getSubject(),
			"SUPPORT_CONVERSATION",
			Map.of(
				"conversationId", conversation.getConversationId().toString(),
				"category", conversation.getCategory()
			),
			"support-conversation:" + conversation.getConversationId() + ":creator",
			true
		));
	}

	private SupportConversationSummaryResponse toSummary(SupportConversation conversation, User currentUser) {
		long unreadCount = messageRepository
			.findAllByConversation_ConversationIdOrderByCreatedAtAsc(conversation.getConversationId())
			.stream()
			.filter(message -> isAddressedTo(message, currentUser))
			.filter(message -> !hasReadReceipt(message, currentUser.getUserId()))
			.count();
		return new SupportConversationSummaryResponse(
			conversation.getConversationId(),
			conversation.getCategory(),
			conversation.getSubject(),
			conversation.getStatus(),
			toUser(conversation.getCreatedBy()),
			conversation.getLastMessagePreview(),
			conversation.getLastMessageAt(),
			unreadCount,
			conversation.getCreatedAt(),
			conversation.getClosedAt()
		);
	}

	private SupportConversationDetailResponse toDetail(
		SupportConversation conversation,
		User currentUser,
		List<SupportMessage> messages
	) {
		return new SupportConversationDetailResponse(
			conversation.getConversationId(),
			conversation.getCategory(),
			conversation.getSubject(),
			conversation.getStatus(),
			toUser(conversation.getCreatedBy()),
			conversation.getLastMessagePreview(),
			conversation.getLastMessageAt(),
			conversation.getCreatedAt(),
			conversation.getClosedAt(),
			messages.stream().map(message -> toMessage(message, conversation, currentUser)).toList()
		);
	}

	private SupportMessageResponse toMessage(SupportMessage message, SupportConversation conversation, User currentUser) {
		return new SupportMessageResponse(
			message.getMessageId(),
			toUser(message.getSender()),
			message.getMessageText(),
			message.getCreatedAt(),
			hasReadReceipt(message, currentUser.getUserId()),
			hasBeenReadByOtherParty(message, conversation)
		);
	}

	private boolean hasBeenReadByOtherParty(SupportMessage message, SupportConversation conversation) {
		User sender = message.getSender();
		if (sender == null) {
			return false;
		}
		if (isAdmin(sender)) {
			return hasReadReceipt(message, conversation.getCreatedBy().getUserId());
		}
		return message.getReads().stream().anyMatch(read -> isAdmin(read.getReader()));
	}

	private boolean hasReadReceipt(SupportMessage message, Long userId) {
		return userId != null && message.getReads().stream().anyMatch(read ->
			read.getReader() != null && userId.equals(read.getReader().getUserId())
		);
	}

	private boolean isAddressedTo(SupportMessage message, User currentUser) {
		User sender = message.getSender();
		if (sender == null || sender.getUserId().equals(currentUser.getUserId())) {
			return false;
		}
		return isAdmin(currentUser) ? !isAdmin(sender) : isAdmin(sender);
	}

	private SupportConversation findConversation(Long conversationId) {
		if (conversationId == null || conversationId <= 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support conversation was not found.");
		}
		return conversationRepository
			.findById(conversationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support conversation was not found."));
	}

	private void ensureCanAccess(SupportConversation conversation, User user) {
		if (isAdmin(user)) {
			return;
		}
		ensureSupportRequester(user);
		if (!conversation.getCreatedBy().getUserId().equals(user.getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this support conversation.");
		}
	}

	private void ensureSupportRequester(User user) {
		if (user == null || isAdmin(user)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers and bank officers can create support conversations.");
		}
		String role = roleName(user);
		if (!List.of("PUBLIC_CUSTOMER", "BANK_CUSTOMER", "BANK_OFFICER").contains(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role cannot use support conversations.");
		}
	}

	private void ensureAdmin(User user) {
		if (!isAdmin(user)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required.");
		}
	}

	private boolean isAdmin(User user) {
		return ADMIN_ROLE.equals(roleName(user));
	}

	private boolean isActive(User user) {
		return user != null && "ACTIVE".equalsIgnoreCase(user.getStatus());
	}

	private String roleName(User user) {
		return user == null || user.getRole() == null || user.getRole().getRoleName() == null
			? ""
			: user.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);
	}

	private SupportUserResponse toUser(User user) {
		if (user == null) {
			return new SupportUserResponse(null, "Deleted user", null, null);
		}
		String displayName = (normalizeOptional(user.getFirstName()) + " " + normalizeOptional(user.getLastName())).trim();
		if (displayName.isBlank()) {
			displayName = user.getUsername();
		}
		return new SupportUserResponse(user.getUserId(), displayName, user.getEmail(), roleName(user));
	}

	private String normalizeRequired(String value) {
		String normalized = normalizeOptional(value);
		if (normalized.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Support text cannot be blank.");
		}
		return normalized;
	}

	private String normalizeOptional(String value) {
		return value == null ? "" : value.trim();
	}

	private String toPreview(String message) {
		String normalized = normalizeRequired(message).replaceAll("\\s+", " ");
		return normalized.length() <= PREVIEW_MAX_LENGTH ? normalized : normalized.substring(0, PREVIEW_MAX_LENGTH);
	}
}
