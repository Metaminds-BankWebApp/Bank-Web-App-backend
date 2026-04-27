package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCustomerProfileRepository extends JpaRepository<PublicCustomerProfile, Long> {

	Optional<PublicCustomerProfile> findByUser_UserId(Long userId);

	List<PublicCustomerProfile> findAllByUser_UserIdIn(List<Long> userIds);

	boolean existsByCustomerCode(String customerCode);
}
