package de.sboe0705.rentals.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.sboe0705.rentals.data.RentRepository;
import de.sboe0705.rentals.model.Rent;
import de.sboe0705.rentals.service.ItemAlreadyReturnedException;
import de.sboe0705.rentals.service.ItemNotYetReturnedException;
import de.sboe0705.rentals.service.RentalService;

@Service
class RentalServiceImpl implements RentalService {

	@Autowired
	private RentRepository rentRepository;

	@Override
	public boolean isItemRent(Long itemId) {
		return rentRepository.findLatestNotReturnedByItemId(itemId) //
				.map(Rent::isRent) //
				.orElse(false);
	}

	@Override
	public Map<Long, Boolean> areItemsRent(Collection<Long> itemIds) {
		List<Long> rentItemIds = new ArrayList<>();
		Iterable<Rent> notReturnedRents = rentRepository.findLatestNotReturnedByItemIds(itemIds);
		notReturnedRents.forEach(rent -> rentItemIds.add(rent.getItemId()));
		return itemIds.stream().collect(Collectors.toMap( //
				Function.identity(), //
				itemId -> rentItemIds.contains(itemId), //
				(existing, replacement) -> existing, //
				LinkedHashMap::new //
		));
	}

	@Override
	public void rentItem(Long itemId, String userId) throws ItemNotYetReturnedException {
		Optional<Rent> rentOptional = rentRepository.findLatestNotReturnedByItemId(itemId);
		if (rentOptional.isPresent()) {
			throw new ItemNotYetReturnedException(itemId);
		}
		Rent rent = new Rent();
		rent.setItemId(itemId);
		rent.setUserId(userId);
		rent.setRentSince(LocalDateTime.now());
		rentRepository.save(rent);
	}

	@Override
	public void returnItem(Long itemId) throws ItemAlreadyReturnedException {
		Optional<Rent> rentOptional = rentRepository.findLatestNotReturnedByItemId(itemId);
		Rent rent = rentOptional.orElseThrow(() -> new ItemAlreadyReturnedException(itemId));
		rent.setReturnedAt(LocalDateTime.now());
		rentRepository.save(rent);
	}

}
