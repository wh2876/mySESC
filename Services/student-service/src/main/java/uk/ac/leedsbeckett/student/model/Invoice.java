package uk.ac.leedsbeckett.student.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

//used with financeIntegrationService
@Data
@JsonIgnoreProperties(ignoreUnknown = true)//I think this is so that when receiving invoices from the finance service it ignores fields that aren't a part of this one
//i.e. the service returns studentId instead of account, and this will make it ignore the unknown studentId field instead of complaining
public class Invoice {
    private Long id;
    private String reference;
    private Double amount;
    private LocalDate dueDate;
    private Type type;
    private Status status;

    private FinanceAccount account;

    public enum Type {
        LIBRARY_FINE,
        TUITION_FEES
    }
    public enum Status {
        OUTSTANDING,
        PAID,
        CANCELLED
    }
}
