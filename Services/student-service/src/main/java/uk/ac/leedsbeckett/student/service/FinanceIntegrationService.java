package uk.ac.leedsbeckett.student.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.leedsbeckett.student.controller.FinanceIntegrationController;
import uk.ac.leedsbeckett.student.model.FinanceAccount;
import uk.ac.leedsbeckett.student.model.Invoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

//alternative methods of getting the rest uris
//1. replace http://localhost:etc. with discovery server or api-gateway or something
//2. also try using hateoas to get correct links
@Component
public class FinanceIntegrationService extends Service{

    //Invoices returned by the finance service only provide the studentId instead of the account
    //This object is needed so I can A) store the entire returned object and B) get the account from the id so i can handle invoices as they are supposed to be filled out
    public static class IncomingInvoice extends Invoice{
        public String studentId;
    }
    //I think param could be of type IncomingInvoice but I recall at some point previously Object needed to passed in since despite the rest calls being told they were returning an IncomingInvoice-
    // -I think it threw when I tried to actually store it in an IncomingInvoice variable
    //it looks like that isnt the case from the syntax when I look at it but I'm hesitant to change it since it works as it is.
    //If I had enough time I'd go through and try to make the whole program more optimized and this would be one of the first things I'd look at
    public Invoice getInvoiceFromFinanceServiceReturn(Object invoice){
        //API call returns studentId in place of Account
        //this gets the account then returns a proper invoice object
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        IncomingInvoice tinv = mapper.convertValue(invoice, IncomingInvoice.class);
        tinv.setAccount(getAccount(tinv.studentId));

        return mapper.convertValue(tinv, Invoice.class);
    }

    private final RestTemplate restTemplate;
    private final String financeUri;

    private static EntityModel<FinanceAccount> makeAccountModel(FinanceAccount financeAccount){
        return EntityModel.of(financeAccount,
                linkTo(methodOn(FinanceIntegrationController.class).getAccountByAccountId(financeAccount.getId())).withSelfRel(),
                linkTo(methodOn(FinanceIntegrationController.class).createAccount(financeAccount)).withRel("create account"),
                linkTo(methodOn(FinanceIntegrationController.class).getAccounts()).withRel("accounts"));
    }
    private static EntityModel<Invoice> makeInvoiceModel(Invoice invoice){
        return EntityModel.of(invoice,
                linkTo(methodOn(FinanceIntegrationController.class).getInvoiceById(invoice.getId())).withSelfRel(),
                linkTo(methodOn(FinanceIntegrationController.class).getInvoices()).withRel("invoices"));
    }

    public FinanceIntegrationService(RestTemplate restTemplate) {
        this.restTemplate=restTemplate;
        this.financeUri = "http://localhost:8081";
    }
    //implemented close to the end of devlopment, didn't make a lot of use of it but did use it
    public boolean serviceOnline(){
        try {
            //returns the html code for the home page of the financeservice
            restTemplate.getForObject(financeUri, String.class);
            return true;
        }catch (Exception e){return false;}
    }

    //parameter type issues with rest returning parameterised objects since the type info is lost
    //this solves that problem
    public <T> List<EntityModel<T>> getListFromREST(String uri, Class<T> clazz) {
        ParameterizedTypeReference<CollectionModel<EntityModel<T>>> responseType =
                new ParameterizedTypeReference<CollectionModel<EntityModel<T>>>() {};

        ResponseEntity<CollectionModel<EntityModel<T>>> response =
                restTemplate.exchange(uri, HttpMethod.GET, null, responseType);

        CollectionModel<EntityModel<T>> col = response.getBody();
        assert col != null;
        return new ArrayList<>(col.getContent());
    }

    public List<FinanceAccount> getAllAccounts() {
        List<EntityModel<FinanceAccount>> li = getListFromREST(financeUri+"/accounts", FinanceAccount.class);
        List<FinanceAccount> accs = new ArrayList<FinanceAccount>();

        ObjectMapper mapper = new ObjectMapper();
        //mapper must be used or error is thrown due to type loss i think?
        for (EntityModel<FinanceAccount> emAcc : li) {
            try {
                accs.add(mapper.convertValue(emAcc.getContent(), FinanceAccount.class));
            }catch (Exception e){
                //same code as invoices, same potential errors i think
                System.out.println(e.getMessage());
            }
        }
        return accs;
    }
    public ResponseEntity<?> getAllAccountsModel() {
        try{
            //i know it seems redundant to take the collection of entitymodels returned from the rest call and turn it into a collection of the entitymodel's type
            // just to turn them back into entitymodels, but I felt it was good in case getAllAccounts needed to be used for the accounts not the entitymodels
            return ResponseEntity.status(HttpStatus.SC_OK).body(
                    CollectionModel.of(getAllAccounts()
                            .stream()
                            .map(FinanceIntegrationService::makeAccountModel)
                            .toList()
                    )
            );
        }catch(Exception e){
            return getProblemResponse(e);
        }
    }

    public FinanceAccount createFinanceAccount(FinanceAccount financeAccount) {
        return restTemplate.postForObject(financeUri+"/accounts", financeAccount, FinanceAccount.class);
    }
    public ResponseEntity<?> createAccountModel(FinanceAccount financeAccount) {
        try{
            return makeResponseEntity(makeAccountModel(createFinanceAccount(financeAccount)));
        }catch(Exception e){
            return getProblemResponse(e);
        }
    }

    public FinanceAccount getAccount(String studentId){
        return restTemplate.getForObject(financeUri+"/accounts/student/" + studentId, FinanceAccount.class);
    }
    public ResponseEntity<?> getAccountModel(String studentId){
       try{
           return makeResponseEntity(makeAccountModel(getAccount(studentId)));
       }catch(Exception e){
           return getProblemResponse(e);
       }
    }
    public FinanceAccount getAccount(Long financeId){
        return restTemplate.getForObject(financeUri+"/accounts/" + financeId, FinanceAccount.class);
    }
    public ResponseEntity<?> getAccountModel(Long financeId){
        try {
            return makeResponseEntity(makeAccountModel(getAccount(financeId)));
        }
        catch(Exception e){
           return getProblemResponse(e);
        }
    }


    public Invoice createInvoice(Invoice invoice){
        //attempted, and believed, fix to the null account problem
        invoice = getInvoiceFromFinanceServiceReturn(restTemplate.postForObject(financeUri+"/invoices", invoice, IncomingInvoice.class));
        return invoice;
    }
    public ResponseEntity<?> createInvoiceModel(Invoice invoice){
        try{

            EntityModel<Invoice> model = makeInvoiceModel(createInvoice(Objects.requireNonNull(invoice)));
            // these default to get requests if you click on the link in postman, not sure how to fix that
            model.add(linkTo(methodOn(FinanceIntegrationController.class).payInvoice(Objects.requireNonNull(model.getContent()).getReference())).withRel("pay invoice"));
            model.add(linkTo(methodOn(FinanceIntegrationController.class).cancelInvoice(model.getContent().getReference())).withRel("cancel invoice"));
            return makeResponseEntity(model);
        }catch(Exception e){
            return getProblemResponse(e);
        }

    }

    //paying an invoice is a PUT request, but it does not need an object to put, just an invoice ref in the uri
    public Invoice payInvoice(String ref){
        restTemplate.put(financeUri+"/invoices/" + ref + "/pay", null);
        return getInvoiceByReference(ref);
    }
    public ResponseEntity<?> payInvoiceModel(String reference){
        try{
            return makeResponseEntity(makeInvoiceModel(payInvoice(reference)));
        }catch (Exception e){
            return getProblemResponse(e);
        }

    }

    //cancelling an invoice is a DELETE request, but does not delete the object, just changes a value
    public Invoice cancelInvoice(String ref) {
        restTemplate.delete(financeUri+"/invoices/" + ref + "/cancel");
        return getInvoiceByReference(ref);
    }
    public ResponseEntity<?> cancelInvoiceModel(String reference){
        try{
            return makeResponseEntity(makeInvoiceModel(cancelInvoice(reference)));
        }catch (Exception e){
            return getProblemResponse(e);
        }

    }

    public List<Invoice> getAllInvoices() {
        List<EntityModel<IncomingInvoice>> li = getListFromREST(financeUri+"/invoices", IncomingInvoice.class);
        List<Invoice> invs = new ArrayList<Invoice>();

        for (EntityModel<IncomingInvoice> emInv : li) {
            //have to convert each returned invoice to our invoice structure
            Invoice inv = getInvoiceFromFinanceServiceReturn(emInv.getContent());
            invs.add(inv);
        }
        return invs;
    }

    public ResponseEntity<?> getAllInvoiceModels() {
        try {
            return ResponseEntity.status(HttpStatus.SC_OK).body(
                    CollectionModel.of(getAllInvoices()
                                .stream()
                                .map(FinanceIntegrationService::makeInvoiceModel)
                                .toList()
                    )
            );
        }catch(Exception e){
            return getProblemResponse(e);
        }
    }

    public Invoice getInvoiceById(Long id){
        try {
            return getInvoiceFromFinanceServiceReturn(restTemplate.getForObject(financeUri+"/invoices/"+id.toString(), IncomingInvoice.class));
        }catch (Exception e){
            //may not be accurate, it may be that we are not be able to connect to the finance service. same case with a following method
            //I have implemented the serviceOnline() check method and could put it here, but figured I'd leave the issue here with this note
            //in case there are other similar cases I don't pick up on just so I KNOW I have indicated that I am aware some error messages are not quite accurate
            //and that I am capable of realising that
            throw new RuntimeException("Invoice does not exist.");
        }}
    public ResponseEntity<?> getInvoiceModelById(Long id){
        try {
            return makeResponseEntity(makeInvoiceModel(getInvoiceById(id)));
        }catch (Exception e){
            return getProblemResponse(e);
        }
    }
    public Invoice getInvoiceByReference(String reference){
        try {
            return getInvoiceFromFinanceServiceReturn(restTemplate.getForObject(financeUri+"/invoices/reference/" + reference, IncomingInvoice.class));
        }catch (Exception e){

            throw new RuntimeException("Invoice does not exist.");
        }
    }
    public ResponseEntity<?> getInvoiceModelByReference(String reference){
       try{
           return makeResponseEntity(makeInvoiceModel(getInvoiceByReference(reference)));
       }catch (Exception e){
           return getProblemResponse(e);
       }
    }


}
