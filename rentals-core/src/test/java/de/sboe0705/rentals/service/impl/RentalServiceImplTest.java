package de.sboe0705.rentals.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import de.sboe0705.rentals.data.RentRepository;
import de.sboe0705.rentals.model.Rent;
import de.sboe0705.rentals.service.ItemAlreadyReturnedException;
import de.sboe0705.rentals.service.ItemNotYetReturnedException;
import de.sboe0705.rentals.service.RentalService;

class RentalServiceImplTest {

	private RentalService underTest;

	private RentRepository rentRepositoryMock;

	@BeforeEach
	void setUp() {
		underTest = new RentalServiceImpl();

		rentRepositoryMock = Mockito.mock(RentRepository.class);
		ReflectionTestUtils.setField(underTest, "rentRepository", rentRepositoryMock);
	}

	@AfterEach
	void tearDown() {
		Mockito.verifyNoMoreInteractions(rentRepositoryMock);
	}

	@Test
	void testIsRentWithoutRentData() {
		long itemId = 1L;

		// when
		boolean isRent = underTest.isItemRent(itemId);

		// then
		Assertions.assertThat(isRent).isFalse();

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemId(itemId);
	}

	@Test
	void testIsItemRentWithRentInRepositoryNotReturned() {
		Long itemId = 1L;

		// given
		Rent rent = new Rent();
		rent.setItemId(itemId);
		rent.setReturnedAt(null);
		Mockito.when(rentRepositoryMock.findLatestNotReturnedByItemId(itemId)) //
				.thenReturn(Optional.of(rent));

		// when
		boolean isRent = underTest.isItemRent(itemId);

		// then
		Assertions.assertThat(isRent).isTrue();

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemId(itemId);
	}

	@Test
	void testIsItemRentWithRentInRepositoryAlreadyReturned() {
		Long itemId = 1L;

		// given
		Rent rent = new Rent();
		rent.setItemId(itemId);
		rent.setReturnedAt(LocalDateTime.now());
		Mockito.when(rentRepositoryMock.findLatestNotReturnedByItemId(itemId)) //
				.thenReturn(Optional.of(rent));

		// when
		boolean isRent = underTest.isItemRent(itemId);

		// then
		Assertions.assertThat(isRent).isFalse();

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemId(itemId);
	}

	@Test
	void testAreItemsRent() throws Exception {
		List<Long> itemIds = List.of(1000L, 2000L, 3000L);
		List<Long> rentItemIds = List.of(1000L, 2000L);

		// given
		List<Rent> rents = rentItemIds.stream().map(rentItemId -> {
			Rent rent = new Rent();
			rent.setItemId(rentItemId);
			return rent;
		}).collect(Collectors.toList());
		Mockito.when(rentRepositoryMock.findLatestNotReturnedByItemIds(itemIds)) //
				.thenReturn(rents);

		// when
		Map<Long, Boolean> rentStatusOfItems = underTest.areItemsRent(itemIds);

		// then
		Assertions.assertThat(rentStatusOfItems.keySet()) //
				.hasSize(itemIds.size()) //
				.containsExactly(itemIds.toArray(Long[]::new));
		Assertions.assertThat(rentStatusOfItems) //
				.allSatisfy((itemId, rentStatus) -> {
					boolean expectedRentStatus = rentItemIds.contains(itemId);
					Assertions.assertThat(rentStatus).isEqualTo(expectedRentStatus);
				});

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemIds(itemIds);
	}

	@Test
	void testRentItem() throws Exception {
		Long itemId = 1000L;
		String userId = "USER001";

		// given
		Mockito.when(rentRepositoryMock.findLatestNotReturnedByItemId(itemId)).thenReturn(Optional.empty());

		ArgumentCaptor<Rent> rentCaptor = ArgumentCaptor.forClass(Rent.class);
		Mockito.when(rentRepositoryMock.save(rentCaptor.capture())).then(invocation -> invocation.getArguments()[0]);

		// when
		underTest.rentItem(itemId, userId);

		// then
		Rent rent = rentCaptor.getValue();
		Assertions.assertThat(rent) //
				.hasFieldOrPropertyWithValue("itemId", itemId) //
				.hasFieldOrPropertyWithValue("userId", userId);
		Assertions.assertThat(rent.getRentSince()) //
				.isNotNull();
		Assertions.assertThat(rent.getReturnedAt()) //
				.isNull();

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemId(itemId);
		Mockito.verify(rentRepositoryMock).save(ArgumentMatchers.any(Rent.class));
	}

	@Test
	void testRentItemThatIsNotYetReturned() throws Exception {
		Long itemId = 1000L;
		String userId = "USER001";

		// given
		Rent rent = new Rent();
		rent.setItemId(itemId);
		Mockito.when(rentRepositoryMock.findLatestNotReturnedByItemId(itemId)).thenReturn(Optional.of(rent));

		// when
		ThrowingCallable invocation = () -> underTest.rentItem(itemId, userId);

		// then
		Assertions.assertThatThrownBy(invocation) //
				.isInstanceOf(ItemNotYetReturnedException.class) //
				.hasFieldOrPropertyWithValue("message",
						"The item with the id " + itemId + " has not yet been returned!");

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemId(itemId);
	}

	@Test
	void testReturnItem() throws Exception {
		Long itemId = 1000L;
		String userId = "USER001";

		// given
		Rent rent = new Rent();
		rent.setId(1L);
		rent.setItemId(itemId);
		rent.setUserId(userId);
		rent.setRentSince(LocalDateTime.now());
		Mockito.when(rentRepositoryMock.findLatestNotReturnedByItemId(itemId)).thenReturn(Optional.of(rent));

		ArgumentCaptor<Rent> rentCaptor = ArgumentCaptor.forClass(Rent.class);
		Mockito.when(rentRepositoryMock.save(rentCaptor.capture())).then(invocation -> invocation.getArguments()[0]);

		// when
		underTest.returnItem(itemId);

		// then
		Assertions.assertThat(rentCaptor.getValue()) //
				.hasFieldOrPropertyWithValue("itemId", itemId) //
				.hasFieldOrPropertyWithValue("userId", userId);
		Assertions.assertThat(rent.getRentSince()) //
				.isNotNull();
		Assertions.assertThat(rent.getReturnedAt()) //
				.isNotNull();

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemId(itemId);
		Mockito.verify(rentRepositoryMock).save(ArgumentMatchers.any(Rent.class));
	}

	@Test
	void testReturnItemThatIsAlreadyReturned() throws Exception {
		Long itemId = 1000L;

		// given
		Mockito.when(rentRepositoryMock.findLatestNotReturnedByItemId(itemId)).thenReturn(Optional.empty());

		// when
		ThrowingCallable invocation = () -> underTest.returnItem(itemId);

		// then
		Assertions.assertThatThrownBy(invocation) //
				.isInstanceOf(ItemAlreadyReturnedException.class) //
				.hasFieldOrPropertyWithValue("message",
						"The item with the id " + itemId + " has already been returned!");

		Mockito.verify(rentRepositoryMock).findLatestNotReturnedByItemId(itemId);
	}

}
