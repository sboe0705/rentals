package de.sboe0705.rentals.service;

import java.util.Collection;
import java.util.Map;

public interface RentService {

	boolean isItemRent(Long itemId);

	Map<Long, Boolean> areItemsRent(Collection<Long> itemIds);

	void rentItem(Long itemId, String userId) throws ItemNotYetReturnedException;

	void returnItem(Long itemId) throws ItemAlreadyReturnedException;

}
