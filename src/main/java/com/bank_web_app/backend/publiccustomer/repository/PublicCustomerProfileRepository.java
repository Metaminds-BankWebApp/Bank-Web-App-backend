package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for public-customer profile persistence and lookup.
public interface PublicCustomerProfileRepository extends JpaRepository<PublicCustomerProfile, Long> {

	// Finds profile mapped to a specific user id.
	Optional<PublicCustomerProfile> findByUser_UserId(Long userId);

	// Finds profiles for a set of user ids.
	List<PublicCustomerProfile> findAllByUser_UserIdIn(List<Long> userIds);

	// Checks whether a generated customer code already exists.
	boolean existsByCustomerCode(String customerCode);
}
