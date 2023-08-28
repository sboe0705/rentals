package de.sboe0705.rentals.service;

public class ItemAlreadyReturnedException extends Exception {

	public ItemAlreadyReturnedException(Long itemId) {
		super("The item with the id " + itemId + " has already been returned!");
	}
	
}
