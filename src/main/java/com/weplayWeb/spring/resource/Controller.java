package com.weplayWeb.spring.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.naming.spi.DirStateFactory.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

import com.squareup.square.SquareClient;
import com.squareup.square.exceptions.ApiException;
import com.squareup.square.models.CancelSubscriptionResponse;
import com.squareup.square.models.Customer;
import com.squareup.square.models.RetrieveCustomerResponse;
import com.squareup.square.models.RetrieveSubscriptionResponse;
import com.squareup.square.models.Subscription;
import com.weplayWeb.spring.Square.CreateCustomer;
import com.weplayWeb.spring.Square.CreatePayment;
import com.weplayWeb.spring.Square.CreateSubscription;
import com.weplayWeb.spring.Square.CustomerResponse;
import com.weplayWeb.spring.Square.EmailResult;
import com.weplayWeb.spring.Square.ErrorResponse;
import com.weplayWeb.spring.Square.PaymentResult;
import com.weplayWeb.spring.Square.SubscriptionResponse;
import com.weplayWeb.spring.Square.TokenWrapper;
import com.weplayWeb.spring.model.CityProfile;
import com.weplayWeb.spring.polulationData.GetCityProfiles;
import com.weplayWeb.spring.services.CSPService;
import com.weplayWeb.spring.services.EmailService;


@RestController
@RequestMapping("/api")
public class Controller {

	// @Autowired: used for automatic dependency injection

	@Autowired
    private SquareClient squareClient;
	
	@Autowired
    private CSPService cspService;
	
	@Autowired
	private CreateCustomer createCustomer;
	
	@Autowired
	private CreatePayment createPayment;
	
	 @Autowired
	 private CreateSubscription subscriptionService;
	
     
    @Autowired
    private EmailService emailService;
	
	private final Logger logger = LoggerFactory.getLogger(Controller.class);
	
	 @Value("${square.environment}")
	    private String environment;

	 @Value("${square.accessToken}")
	    private String accessToken;

	 @Value("${square.locationId}")
	    private String locationId;
	
	 @Value("${square.applicationId}")
       private String applicationId;

	
	@Value("${cors.allowed.origin}")
	private String corsAllowedOrigin;
	

	
	  public Controller() {}
	  

	@GetMapping("/cityprofiles/{state_name}")
	public ArrayList<CityProfile> getCityProfile(@PathVariable String state_name){
		state_name = state_name.substring(0, 1).toUpperCase() + state_name.substring(1).toLowerCase(); // make sure the first char is away capitalized 
		GetCityProfiles data = new GetCityProfiles(state_name);
		return data.getCityProfile();	
	}
		
	 @GetMapping("/nonce")
	    public ResponseEntity<Map<String, String>> getNonce() {
	        String nonce = cspService.generateNonce();
	            
	        return ResponseEntity.ok(Collections.singletonMap("nonce", nonce));
	    }
	 
	
	  @PostMapping("/customer") 
	  public ResponseEntity<CustomerResponse> createCustomer(@RequestBody TokenWrapper tokenObject)
	          throws InterruptedException, ExecutionException, IOException {
		  	 
			  logger.info("Received customer creation request");	  
			  ResponseEntity<CustomerResponse> response = createCustomer.createCustomerResponse(tokenObject);
			  createCustomer.setCustomerInfo(tokenObject);
			  logger.info("Customer creation completed with status: {}", response.getStatusCode());
	          return response;		  
	    } 
	      
   
	    @PostMapping("/payment") 
	    public ResponseEntity<PaymentResult> processPayment(@RequestBody TokenWrapper tokenObject)
	            throws InterruptedException, ExecutionException, IOException, ApiException {   
	    	
	    	     logger.info("Received Payment creation request");	
	    	     ResponseEntity<PaymentResult> paymentResult = createPayment.createPaymentRequest(tokenObject);	
	    	     
	    	     if(paymentResult.getStatusCode().is2xxSuccessful()) {
	    	    	
	    	    	 EmailResult emailResult;
	    	    	 
	    	    	 try {
	    	                emailResult = createPayment.sendEmail();
	    	            } catch (Exception e) {
	    	                logger.error("Failed to send email", e);
	    	                emailResult = new EmailResult("FAILURE", "Email sending failed: " + e.getMessage());
	    	            }
	    	    	 
	    	    	 PaymentResult updatedResult = new PaymentResult(
	    	    	            "SUCCESS",
	    	    	            null,
	    	    	            emailResult
	    	    	        );
	    	    	 updatedResult.setNonce(paymentResult.getBody().getNonce());
	    	    	  return ResponseEntity.ok()
	    	    	            .headers(paymentResult.getHeaders())
	    	    	            .body(updatedResult);
	    	     }
	    	     
	    		 return paymentResult;	      		
	     }
	    
	    @PostMapping("/subscriptions")
	    public ResponseEntity<SubscriptionResponse> createSubscription(@RequestBody TokenWrapper tokenObject) {
	        try {
	            logger.info("Received subscription creation request for customer: {}", tokenObject.getCustomerId());
	            
	            // Create subscription
	            ResponseEntity<?> subscriptionResult = subscriptionService.createSubscription(tokenObject);
	            
	            if (subscriptionResult.getStatusCode().is2xxSuccessful()) {
	                String nonce = cspService.generateNonce();
	                
	                SubscriptionResponse response = new SubscriptionResponse(
	                    "SUCCESS",
	                    subscriptionResult.getBody(),
	                    nonce
	                );
	                
	                logger.info("Subscription created successfully for customer: {}", tokenObject.getCustomerId());
	                return ResponseEntity.ok()
	                    .header("Content-Security-Policy", cspService.generateCSPHeader(nonce))
	                    .body(response);
	            } else {
	                logger.error("Failed to create subscription: {}", subscriptionResult.getBody());
	                return ResponseEntity.badRequest().body(
	                    new SubscriptionResponse("FAILURE", "Failed to create subscription", null)
	                );
	            }
	        } catch (Exception e) {
	            logger.error("Error creating subscription", e);
	            return ResponseEntity.internalServerError().body(
	                new SubscriptionResponse("FAILURE", "Internal server error: " + e.getMessage(), null)
	            );
	        }
	    }
	    
	    @GetMapping("/cancel-subscription/{subscriptionId}")
	    public ModelAndView showCancellationPage(@PathVariable String subscriptionId) {
	        ModelAndView mav = new ModelAndView("cancel-subscription");
	        mav.addObject("subscriptionId", subscriptionId);
	        return mav;
	    }
	    
	    @PostMapping("/cancel-subscription/{subscriptionId}/confirm")
	    public ResponseEntity<SubscriptionResponse> confirmCancellation(
	            @PathVariable String subscriptionId ) {
	            
	        try {
	        	RetrieveSubscriptionResponse subscriptionResult = 
	        			 squareClient.getSubscriptionsApi().retrieveSubscription(subscriptionId, null);
	            
	        	Subscription subscription = subscriptionResult.getSubscription();
	        	
	        	String customeId = subscription.getCustomerId();
	        	
	        	RetrieveCustomerResponse retrieveCustomerResponse = squareClient.getCustomersApi().retrieveCustomer(customeId);
	            Customer customer = retrieveCustomerResponse.getCustomer();
	            String email = customer.getEmailAddress();
	            String givenName = customer.getGivenName();
	            String familyName = customer.getFamilyName();
	      
	            String chargeThrDate = subscription.getChargedThroughDate();
	      
	            CancelSubscriptionResponse cancellationResult = squareClient.getSubscriptionsApi().cancelSubscription(subscriptionId);

	                // Send cancellation confirmation email
	                emailService.sendSubscriptionCancelEmail(email,
	                										 givenName,
	                										 familyName, 
	                										 chargeThrDate, 	                					
	                										 subscriptionId);     
	                	
	        } catch (Exception e) {
	            return ResponseEntity.internalServerError()
	                .body(new SubscriptionResponse("FAILURE", "Internal server error", null));
	        }
	        return ResponseEntity.ok()
	                .body(new SubscriptionResponse("SUCCESS", "Subscription is cancelled", null));
	    }

	    @GetMapping("/{subscriptionId}")
	    public ResponseEntity<SubscriptionResponse> getSubscription(@PathVariable String subscriptionId) {
	        try {
	            logger.info("Received subscription retrieval request for ID: {}", subscriptionId);
	            
	            ResponseEntity<?> subscriptionResult = subscriptionService.getSubscription(subscriptionId);
	            
	            if (subscriptionResult.getStatusCode().is2xxSuccessful()) {
	                String nonce = cspService.generateNonce();
	                
	                SubscriptionResponse response = new SubscriptionResponse(
	                    "SUCCESS",
	                    subscriptionResult.getBody(),
	                    nonce
	                );
	                
	                return ResponseEntity.ok()
	                    .header("Content-Security-Policy", cspService.generateCSPHeader(nonce))
	                    .body(response);
	            } else {
	                logger.error("Failed to retrieve subscription: {}", subscriptionResult.getBody());
	                return ResponseEntity.badRequest().body(
	                    new SubscriptionResponse("FAILURE", "Failed to retrieve subscription", null)
	                );
	            }
	        } catch (Exception e) {
	            logger.error("Error retrieving subscription", e);
	            return ResponseEntity.internalServerError().body(
	                new SubscriptionResponse("FAILURE", "Internal server error: " + e.getMessage(), null)
	            );
	        }
	    }

	      	    
}

@RestControllerAdvice
class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        logger.error("Unhandled exception occurred", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An unexpected error occurred"));
    }
}


