package de.sboe0705.rentals.service;

public class ItemNotYetReturnedException extends Exception {

	public ItemNotYetReturnedException(Long itemId) {
		super("The item with the id " + itemId + " has not yet been returned!");
	}
	
}
