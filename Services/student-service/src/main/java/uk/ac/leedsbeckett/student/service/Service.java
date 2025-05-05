package uk.ac.leedsbeckett.student.service;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.ResponseEntity;

public class Service {

    //define general response methods I've used in all services
    //as I developed more I realised how to better use the response entity class and so if I had the chance I'd replace all of these calls
    // with appropriate replacements
    protected <T> ResponseEntity<EntityModel<T>> makeResponseEntity(EntityModel<T> model){
        return ResponseEntity.ok(
                        model
                );
    }
    //this one especially isn't great since regardless of the error the HttpStatus will always be the same
    // which will be unhelpful for other services making api calls to mine
    // or to internal calls of the service api methods in my program (i.e. the enrolment service makes use of different HttpStatuses
    protected ResponseEntity<Problem> getProblemResponse(Exception e){
        return ResponseEntity
                .status(HttpStatus.SC_METHOD_NOT_ALLOWED)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create()
                        .withTitle("Action failed.")
                        .withDetail(e.getMessage()));
    }

}
