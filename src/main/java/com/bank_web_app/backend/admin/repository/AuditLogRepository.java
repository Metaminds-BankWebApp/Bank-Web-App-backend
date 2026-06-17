package com.bank_web_app.backend.admin.repository;
import com.bank_web_app.backend.admin.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

	List<AuditLog> findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
		LocalDateTime createdAt,
		Pageable pageable
	);

	List<AuditLog> findAllByCreatedAtGreaterThanEqualAndActorRoleIgnoreCaseOrderByCreatedAtDesc(
		LocalDateTime createdAt,
		String actorRole,
		Pageable pageable
	);

	@Query("SELECT DISTINCT a.actionType FROM AuditLog a WHERE a.actionType IS NOT NULL ORDER BY a.actionType ASC")
	List<String> findDistinctActionTypes();

	@Query("SELECT DISTINCT a.tone FROM AuditLog a WHERE a.tone IS NOT NULL ORDER BY a.tone ASC")
	List<String> findDistinctTones();

	@Query("SELECT DISTINCT a.actorRole FROM AuditLog a WHERE a.actorRole IS NOT NULL ORDER BY a.actorRole ASC")
	List<String> findDistinctActorRoles();

	@Query("SELECT DISTINCT a.targetType FROM AuditLog a WHERE a.targetType IS NOT NULL ORDER BY a.targetType ASC")
	List<String> findDistinctTargetTypes();
}
