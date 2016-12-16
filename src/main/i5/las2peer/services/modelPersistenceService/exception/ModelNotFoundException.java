package i5.las2peer.services.modelPersistenceService.exception;

import java.sql.SQLException;

/**
 * 
 * Exception class to differentiate "correct" not found cases from real database
 * errors.
 * 
 */
public class ModelNotFoundException extends SQLException {
	private static final long serialVersionUID = 518776288108061504L;

	public ModelNotFoundException(String message) {
		super(message);
	}

}
