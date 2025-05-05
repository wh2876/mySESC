package uk.ac.leedsbeckett.student.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LibraryIntegrationService extends Service{

    private final RestTemplate restTemplate;
    private final String libraryUri;

    public LibraryIntegrationService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
        libraryUri = "http://localhost:80";
    }

    //used as a wrapper for the string sent as a post request to library service
    //i really wanted to get rid of this and just pass
    // "{studentId:"+studentId+"}" but it wouldnt work since I guess it gets sent as
    // "{ 'String': "{studentId:c7571529}"}" or something (depending on the studentId passed in of course)
    private static class idObject{
        public String studentId;
    }
    public String createLibraryAccount(String studentID){
       idObject idObject = new idObject();
       idObject.studentId = studentID;
       return restTemplate.postForObject(libraryUri + "/api/register",idObject, String.class);
    }
    public ResponseEntity<?> createLibraryAccountModel(String studentID){
        try {
            return ResponseEntity.ok().body(createLibraryAccount(studentID));
        }
        catch (Exception e){
            return getProblemResponse(e);
        }
    }
}
