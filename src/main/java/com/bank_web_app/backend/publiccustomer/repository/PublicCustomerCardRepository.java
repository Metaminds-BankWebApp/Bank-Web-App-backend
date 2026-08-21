package com.bank_web_app.backend.publiccustomer.repository;

import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Repository for public-customer card entries and provider lookups.
public interface PublicCustomerCardRepository extends JpaRepository<PublicCustomerCard, Long> {
	// Returns all card rows linked to a financial record.
	List<PublicCustomerCard> findAllByFinancialRecord_RecordId(Long recordId);

	// Returns card rows for a batch of financial records.
	List<PublicCustomerCard> findAllByFinancialRecord_RecordIdIn(Collection<Long> recordIds);

	// Deletes all card rows linked to a financial record.
	void deleteByFinancialRecord_RecordId(Long recordId);

	// Returns distinct non-empty card provider names for dropdown options.
	@Query("select distinct c.provider from PublicCustomerCard c where c.provider is not null and trim(c.provider) <> '' order by c.provider")
	List<String> findDistinctProviders();
}
