package de.sboe0705.rentals.data;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.sboe0705.rentals.model.Rent;

public interface RentRepository extends CrudRepository<Rent, Long> {

	default Optional<Rent> findLatestNotReturnedByItemId(Long itemId) {
		return findFirst1ByItemIdAndReturnedAtIsNullOrderByIdDesc(itemId);
	}

	Optional<Rent> findFirst1ByItemIdAndReturnedAtIsNullOrderByIdDesc(Long itemId);

	@Query(value = "select _r from Rent _r where _r.id in (select max(r.id) from Rent r where r.itemId in :itemIds and returnedAt is null group by r.itemId)")
	Iterable<Rent> findLatestNotReturnedByItemIds(Collection<Long> itemIds);

}
