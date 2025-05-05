package uk.ac.leedsbeckett.student.model;

import lombok.Data;

//used with the financeIntegrationService
@Data
public class FinanceAccount {
    private Long id;
    private String studentId;
    private boolean hasOutstandingBalance;

    public FinanceAccount() {}
    public FinanceAccount(String studentId) {
        this.studentId = studentId;
    }
}
