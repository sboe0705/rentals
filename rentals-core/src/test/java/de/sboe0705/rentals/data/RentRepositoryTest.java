package de.sboe0705.rentals.data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.OptionalAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import de.sboe0705.rentals.model.Rent;

@DataJpaTest
class RentRepositoryTest {

	@Autowired
	private RentRepository underTest;

	@Test
	@Sql({ "/testdata.sql" })
	void testFindById() throws Exception {
		// given
		long rentId = 1;

		// when
		Optional<Rent> rentOptional = underTest.findById(rentId);

		// then
		Assertions.assertThat(rentOptional) //
				.isPresent() //
				.get() //
				.extracting(Rent::getId, Rent::getItemId, Rent::getUserId, Rent::getRentSince, Rent::getReturnedAt) //
				.containsExactly(rentId, 1000L, "USER002", LocalDateTime.of(2023, 8, 10, 7, 30), null);
	}

	@Test
	void testSave() throws Exception {
		Rent rent = new Rent();
		rent.setItemId(1L);
		rent.setUserId("USER002");
		rent.setRentSince(LocalDateTime.of(2023, 8, 8, 7, 0));

		// when
		underTest.save(rent);

		// then
		Assertions.assertThat(rent.getId()).isNotNull();
	}

	@Test
	@Sql({ "/testdata.sql" })
	void testFindLatestNotReturnedByItemId() throws Exception {
		// two rents and second not returned
		assertLatestNotReturnedByItemIdIsPresent(1000L, true);
		
		// only rent not returned
		assertLatestNotReturnedByItemIdIsPresent(2000L, true);
		
		// two rents and both returned
		assertLatestNotReturnedByItemIdIsPresent(3000L, false);
		
		// only rent returned
		assertLatestNotReturnedByItemIdIsPresent(4000L, false);
		
		// no rent
		assertLatestNotReturnedByItemIdIsPresent(5000L, false);
	}

	private void assertLatestNotReturnedByItemIdIsPresent(long itemId, boolean expectedPresent) {
		// when
		Optional<Rent> rentOptional = underTest.findLatestNotReturnedByItemId(itemId);

		// then
		OptionalAssert<Rent> optionalRentAssert = Assertions.assertThat(rentOptional);
		if (expectedPresent) {
			optionalRentAssert //
					.isPresent() //
					.get() //
					.extracting(Rent::getItemId) //
					.isEqualTo(itemId);
		} else {
			optionalRentAssert //
					.isEmpty();
		}
	}

	@Test
	@Sql({ "/testdata.sql" })
	void findLatestNotReturnedByItemIds() throws Exception {
		// when
		Iterable<Rent> rents = underTest.findLatestNotReturnedByItemIds(List.of(1000L, 2000L, 3000L, 4000L, 5000L));

		// then
		Assertions.assertThat(rents) //
				.extracting(Rent::getItemId) //
				.containsExactly(1000L, 2000L);
	}

}
