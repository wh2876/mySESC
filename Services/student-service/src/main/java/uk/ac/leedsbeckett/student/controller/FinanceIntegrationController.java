package uk.ac.leedsbeckett.student.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.leedsbeckett.student.model.FinanceAccount;
import uk.ac.leedsbeckett.student.model.Invoice;
import uk.ac.leedsbeckett.student.service.FinanceIntegrationService;

@RestController
@RequestMapping("/finance")
public class FinanceIntegrationController {

    private FinanceIntegrationService financeIntegrationService;

    public FinanceIntegrationController(FinanceIntegrationService financeIntegrationService) {
        this.financeIntegrationService = financeIntegrationService;
    }

    /// ///ACCOUNTS/// ///

    @GetMapping("/accounts")
    @ResponseBody
    public ResponseEntity<?> getAccounts() {
        return financeIntegrationService.getAllAccountsModel();
    }

    @PostMapping("/accounts")
    @ResponseBody
    public ResponseEntity<?> createAccount(@RequestBody FinanceAccount financeAccount) {
        return financeIntegrationService.createAccountModel(financeAccount);
    }

    @GetMapping("/accounts/student/{accountId}")
    @ResponseBody
    public ResponseEntity<?> getAccountByStudentId(@PathVariable String accountId) {
        return financeIntegrationService.getAccountModel(accountId);
    }

    @GetMapping("/accounts/{id}")
    @ResponseBody
    public ResponseEntity<?> getAccountByAccountId(@PathVariable Long id) {
        return financeIntegrationService.getAccountModel(id);
    }

    /// /// /// ///

    /// ///INVOICES/// ///

    //CONTINUE HERE AND THEN IMPLEMENT HATEOAS LINKS IN THE SERVICE

    @PostMapping("/invoices")
    @ResponseBody
    public ResponseEntity<?> createInvoice(@RequestBody Invoice invoice) {
        return financeIntegrationService.createInvoiceModel(invoice);
    }

    @PutMapping("/invoices/{reference}/pay")
    @ResponseBody
    public ResponseEntity<?> payInvoice(@PathVariable String reference){
        return financeIntegrationService.payInvoiceModel(reference);
    }

    @DeleteMapping("/invoices/{reference}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelInvoice(@PathVariable String reference){
        return financeIntegrationService.cancelInvoiceModel(reference);
    }

    @GetMapping("/invoices")
    @ResponseBody
    public ResponseEntity<?> getInvoices() {
        return financeIntegrationService.getAllInvoiceModels();
    }

    @GetMapping("/invoices/{id}")
    @ResponseBody
    public ResponseEntity<?> getInvoiceById(@PathVariable Long id) {
        return financeIntegrationService.getInvoiceModelById(id);
    }

    @GetMapping("invoices/reference/{ref}")
    @ResponseBody
    public ResponseEntity<?> getInvoiceByReference(@PathVariable String ref) {
        return financeIntegrationService.getInvoiceModelByReference(ref);
    }
}
