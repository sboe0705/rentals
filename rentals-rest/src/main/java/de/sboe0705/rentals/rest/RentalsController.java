package de.sboe0705.rentals.rest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.sboe0705.rentals.data.RentRepository;
import de.sboe0705.rentals.model.Rent;
import de.sboe0705.rentals.service.ItemAlreadyReturnedException;
import de.sboe0705.rentals.service.ItemNotYetReturnedException;
import de.sboe0705.rentals.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
public class RentalsController {

	@Autowired
	private RentRepository rentRepository;

	@Autowired
	private RentalService rentalService;

	@Operation(summary = "Get all rents", parameters = { //
			@Parameter(name = "onlyRent", description = "Optional parameter to select only rents that are currently rent (isRent = true).") //
	})
	@GetMapping("/rents")
	public List<Rent> getRents(@RequestParam(defaultValue = "false") boolean onlyRent) {
		List<Rent> rents = new ArrayList<>();
		rentRepository.findAll() //
				.forEach(rents::add);
		if (onlyRent) {
			rents = rents.stream().filter(Rent::isRent).collect(Collectors.toList());
		}
		return rents;
	}

	@Operation(summary = "Is item rent?")
	@ApiResponses(value = { //
			@ApiResponse(responseCode = "200", description = "Item's rent status as boolean (true = rent).") //
	})
	@GetMapping("is-rent/item/{itemId}")
	public Boolean isItemRent(@PathVariable long itemId) {
		return rentalService.isItemRent(itemId);
	}

	@Operation(summary = "Are items rent?", parameters = { //
			@Parameter(name = "itemIds", description = "Comma-separated list of item ids.") //
	})
	@ApiResponses(value = { //
			@ApiResponse(responseCode = "200", description = "Items' rent statuses (true = rent) mapped by item ids.") //
	})
	@GetMapping("/is-rent/items")
	public Map<Long, Boolean> areItemsRent(@RequestParam(name = "itemIds") String commaSeparatedItemIds) {
		List<Long> itemIds = Stream.of(commaSeparatedItemIds.split(",")) //
				.map(Long::valueOf) //
				.collect(Collectors.toList());
		return rentalService.areItemsRent(itemIds);
	}

	@Operation(summary = "Rent an item.")
	@ApiResponses(value = { //
			@ApiResponse(responseCode = "200", description = "Item was available and has been successfully rent by user."),
			@ApiResponse(responseCode = "404", description = "Item was not available.") //
	})
	@PostMapping("/rent/item/{itemId}/by/{userId}")
	public void rentItem(@PathVariable long itemId, @PathVariable String userId) {
		try {
			rentalService.rentItem(itemId, userId);
		} catch (ItemNotYetReturnedException e) {
			throw new ResponseStatusException(NOT_FOUND, "Item not yet returned!");
		}
	}

	@Operation(summary = "Return an item.")
	@ApiResponses(value = { //
			@ApiResponse(responseCode = "200", description = "Item has been successfully returned."),
			@ApiResponse(responseCode = "400", description = "Item was not rent.") //
	})
	@PostMapping("/return/item/{itemId}")
	public void rentItem(@PathVariable long itemId) {
		try {
			rentalService.returnItem(itemId);
		} catch (ItemAlreadyReturnedException e) {
			throw new ResponseStatusException(BAD_REQUEST, "Item is not rent!");
		}
	}

}
