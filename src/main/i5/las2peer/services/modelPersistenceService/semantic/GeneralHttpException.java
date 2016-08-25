package i5.las2peer.services.modelPersistenceService.semantic;

import i5.las2peer.restMapper.HttpResponse;

public class GeneralHttpException extends Exception {
	
	private HttpResponse response;
	
	public GeneralHttpException(HttpResponse response) {
		this.response = response;
	}
	
	public HttpResponse getResponse() {
		return this.response;
	}
}
